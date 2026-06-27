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
import de.geheimagentnr1.minecraft_forge_api.AbstractMod;
import net.minecraftforge.fml.DistExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ManyIdeasCore.class, remap = false)
public abstract class ManyIdeasCoreMixin extends AbstractMod {

    /**
     * @author GeheimagentNr1 (Fixed by Patch)
     * @reason Fixes side-loading crash on Dedicated Servers
     */
    @Overwrite(remap = false)
    @Override
    protected void initMod() {
        // These methods are now "visible" because we extend AbstractMod
        ModBlocksRegisterFactory modBlocksRegisterFactory = registerEventHandler(new ModBlocksRegisterFactory());
        registerEventHandler(new ModArgumentTypesRegisterFactory());
        registerEventHandler(new ModCommandsRegisterFactory());
        ModItemsRegisterFactory modItemsRegisterFactory = registerEventHandler(new ModItemsRegisterFactory());

        // Use 'this' as the mod instance
        ManyIdeasCore instance = (ManyIdeasCore) (Object) this;

        registerEventHandler(new ModIngredientSerializersRegisterFactory(instance));
        registerEventHandler(new ModRecipeSerializersRegisterFactory());
        registerEventHandler(new ModRecipeTypesRegisterFactory());
        registerEventHandler(Network.getInstance());

        // Isolated Client Logic
        DistExecutor.unsafeRunForDist(
            () -> () -> {
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
                // Server Side: Do nothing
            }
        );
    }
}