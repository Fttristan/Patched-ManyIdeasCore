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

    // These shadows use 'Object' to match the compiled bytecode exactly, 
    // avoiding the NoSuchMethodError (Signature mismatch).
    @Shadow(remap = false) 
    public abstract Object registerEventHandler(Object handler);

    @Shadow(remap = false) 
    public abstract Object registerConfig(Function<?, ?> configFactory);

    @Shadow(remap = false) 
    public abstract IEventBus forgeEventBus();

    @Shadow(remap = false) 
    public abstract IEventBus modEventBus();

    /**
     * @author GeheimagentNr1 (Fixed by Patch)
     * @reason Fix Dedicated Server crash by isolating client-only factory registrations.
     */
    @Overwrite(remap = false)
    protected void initMod() {
        // 1. Common Side Registrations (Safe for Server)
        ModBlocksRegisterFactory blocksFactory = (ModBlocksRegisterFactory) registerEventHandler(new ModBlocksRegisterFactory());
        registerEventHandler(new ModArgumentTypesRegisterFactory());
        registerEventHandler(new ModCommandsRegisterFactory());
        
        ModItemsRegisterFactory itemsFactory = (ModItemsRegisterFactory) registerEventHandler(new ModItemsRegisterFactory());

        // Cast this to ManyIdeasCore for the ingredient factory
        ManyIdeasCore instance = (ManyIdeasCore) (Object) this;
        registerEventHandler(new ModIngredientSerializersRegisterFactory(instance));
        
        registerEventHandler(new ModRecipeSerializersRegisterFactory());
        registerEventHandler(new ModRecipeTypesRegisterFactory());
        registerEventHandler(Network.getInstance());

        // 2. Client-Only Registrations (Isolated from Server)
        DistExecutor.unsafeRunForDist(
            () -> () -> {
                // This code ONLY runs on the Physical Client.
                // We use registerConfig and registerEventHandler for client classes here.
                
                // Load Client Config
                ClientConfig clientConfig = (ClientConfig) registerConfig((Function<ManyIdeasCore, ClientConfig>) ClientConfig::new);

                // Load Debug Blocks (Client Only)
                ModDebugBlocksRegisterFactory debugBlocksFactory = (ModDebugBlocksRegisterFactory) registerEventHandler(
                    new ModDebugBlocksRegisterFactory(clientConfig)
                );

                // Load Creative Tabs (Client Only)
                registerEventHandler(new ModCreativeModeTabRegisterFactory(
                    clientConfig,
                    blocksFactory,
                    debugBlocksFactory,
                    itemsFactory
                ));

                // Handle Player Decorations (Client Only)
                PlayerDecorationManager playerDecorationManager = new PlayerDecorationManager();
                forgeEventBus().addListener(playerDecorationManager::handlePreRenderPlayerEvent);
                modEventBus().addListener(playerDecorationManager::handleFMLClientSetupEvent);
                
                return null;
            },
            () -> () -> {
                // Server Side: Skip all client logic
                return null;
            }
        );
    }
}