package de.geheimagentnr1.manyideas_core.mixin;

import de.geheimagentnr1.manyideas_core.ManyIdeasCore;
import de.geheimagentnr1.manyideas_core.config.ClientConfig;
import de.geheimagentnr1.manyideas_core.elements.blocks.ModBlocksRegisterFactory;
import de.geheimagentnr1.manyideas_core.elements.blocks.ModDebugBlocksRegisterFactory;
import de.geheimagentnr1.manyideas_core.elements.commands.ModArgumentTypesRegisterFactory;
import de.geheimagentnr1.manyideas_core.elements.commands.ModCommandsRegisterFactory;
import de.geheimagentnr1.manyideas_core.elements.creative_mod_tabs.ModCreativeModeTabRegisterFactory;
import de.geheimagentnr1.manyideas_core.elements.items.ModItemsRegisterFactory;
import de.geheimagentnr1.manyideas_core.elements.recipes.ModIngredientSerializersRegisterFactory;
import de.geheimagentnr1.manyideas_core.elements.recipes.ModRecipeSerializersRegisterFactory;
import de.geheimagentnr1.manyideas_core.elements.recipes.ModRecipeTypesRegisterFactory;
import de.geheimagentnr1.manyideas_core.network.Network;
import de.geheimagentnr1.manyideas_core.special.decoration_renderer.PlayerDecorationManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Function;

@Mixin(value = ManyIdeasCore.class, remap = false)
public abstract class ManyIdeasCoreMixin {

    // Shadowing the methods from the private AbstractMod class so the Mixin can use them.
    // remap = false is used because these belong to a custom API, not Mojang mappings.
    @Shadow(remap = false) 
    public abstract <T> T registerEventHandler(T handler);

    @Shadow(remap = false) 
    public abstract <T> T registerConfig(Function<ManyIdeasCore, T> configFactory);

    @Shadow(remap = false) 
    public abstract IEventBus forgeEventBus();

    @Shadow(remap = false) 
    public abstract IEventBus modEventBus();

    /**
     * @author GeheimagentNr1 (Fixed by Patch)
     * @reason Prevent Dedicated Server crash by isolating Client-side classes.
     */
    @Overwrite(remap = false)
    protected void initMod() {
        // 1. Initialization required on BOTH sides (Server and Client)
        ModBlocksRegisterFactory modBlocksRegisterFactory = registerEventHandler(new ModBlocksRegisterFactory());
        registerEventHandler(new ModArgumentTypesRegisterFactory());
        registerEventHandler(new ModCommandsRegisterFactory());
        ModItemsRegisterFactory modItemsRegisterFactory = registerEventHandler(new ModItemsRegisterFactory());

        // Cast this to ManyIdeasCore for the register factory
        ManyIdeasCore instance = (ManyIdeasCore) (Object) this;

        registerEventHandler(new ModIngredientSerializersRegisterFactory(instance));
        registerEventHandler(new ModRecipeSerializersRegisterFactory());
        registerEventHandler(new ModRecipeTypesRegisterFactory());
        registerEventHandler(Network.getInstance());

        // 2. Fixed side isolation using DistExecutor
        DistExecutor.unsafeRunForDist(
            () -> () -> {
                // This block executes ONLY on Physical Client (Minecraft Client)
                // Classes like ClientConfig and PlayerDecorationManager are safe to load here.
                ClientConfig clientConfig = registerConfig(ClientConfig::new);

                ModDebugBlocksRegisterFactory modDebugBlocksRegisterFactory = registerEventHandler(
                    new ModDebugBlocksRegisterFactory(clientConfig)
                );

                registerEventHandler(new ModCreativeModeTabRegisterFactory(
                    clientConfig,
                    modBlocksRegisterFactory,
                    modDebugBlocksRegisterFactory,
                    modItemsRegisterFactory
                ));

                PlayerDecorationManager playerDecorationManager = new PlayerDecorationManager();
                forgeEventBus().addListener(playerDecorationManager::handlePreRenderPlayerEvent);
                modEventBus().addListener(playerDecorationManager::handleFMLClientSetupEvent);
            },
            () -> () -> {
                // This block executes ONLY on Dedicated Server
                // We do nothing here, skipping the client-only classes above.
            }
        );
    }
}