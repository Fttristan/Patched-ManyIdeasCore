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
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ManyIdeasCore.class, remap = false)
public abstract class ManyIdeasCoreMixin extends AbstractMod {

    /**
     * @author GeheimagentNr1 (Fixed by FTTristan)
     * @reason Overwrite initMod to prevent side-loading client classes on Dedicated Servers.
     */
    @Overwrite(remap = false)
    @Override
    protected void initMod() {
        ManyIdeasCore instance = (ManyIdeasCore) (Object) this;

        // 1. Common Side logic (Safe for Server)
        ModBlocksRegisterFactory blocksFactory = (ModBlocksRegisterFactory) registerEventHandler(new ModBlocksRegisterFactory());
        registerEventHandler(new ModArgumentTypesRegisterFactory());
        registerEventHandler(new ModCommandsRegisterFactory());
        ModItemsRegisterFactory itemsFactory = (ModItemsRegisterFactory) registerEventHandler(new ModItemsRegisterFactory());

        registerEventHandler(new ModIngredientSerializersRegisterFactory(instance));
        registerEventHandler(new ModRecipeSerializersRegisterFactory());
        registerEventHandler(new ModRecipeTypesRegisterFactory());
        registerEventHandler(Network.getInstance());

        // 2. Side Isolation
        if (FMLEnvironment.dist.isClient()) {
            // Hides client-only class references inside a nested class 
            // so the server classloader never touches them.
            ClientInitializer.run(instance, this, blocksFactory, itemsFactory);
        } else {
            System.out.println("[ManyIdeas-Patch] Running on Dedicated Server. Client-only registrations skipped.");
        }
    }

    private static class ClientInitializer {
        private static void run(ManyIdeasCore instance, ManyIdeasCoreMixin mixin, ModBlocksRegisterFactory b, ModItemsRegisterFactory i) {
            // Use a manual lambda to satisfy the compiler's conversion check
            ClientConfig config = mixin.registerConfig((mod) -> new ClientConfig(instance));

            ModDebugBlocksRegisterFactory debug = (ModDebugBlocksRegisterFactory) mixin.registerEventHandler(
                new ModDebugBlocksRegisterFactory(config)
            );

            mixin.registerEventHandler(new ModCreativeModeTabRegisterFactory(config, b, debug, i));

            PlayerDecorationManager deco = new PlayerDecorationManager();
            ((net.minecraftforge.eventbus.api.IEventBus) mixin.forgeEventBus()).addListener(deco::handlePreRenderPlayerEvent);
            ((net.minecraftforge.eventbus.api.IEventBus) mixin.modEventBus()).addListener(deco::handleFMLClientSetupEvent);
        }
    }
}