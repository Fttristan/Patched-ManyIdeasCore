package de.geheimagentnr1.manyideas_core.mixin;

import de.geheimagentnr1.manyideas_core.ManyIdeasCore;
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
import de.geheimagentnr1.manyideas_core.config.ClientConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Function;

@Mixin(value = ManyIdeasCore.class, remap = false)
public abstract class ManyIdeasCoreMixin {

    @Shadow(remap = false) 
    public abstract Object registerEventHandler(Object handler);

    @Shadow(remap = false) 
    public abstract Object registerConfig(Function<?, ?> configFactory);

    @Shadow(remap = false) 
    public abstract Object forgeEventBus();

    @Shadow(remap = false) 
    public abstract Object modEventBus();

    /**
     * @author GeheimagentNr1 (Fixed by FTTristan)
     * @reason Completely replace initMod with a side-aware version to prevent server crashes.
     */
    @Overwrite(remap = false)
    protected void initMod() {
        ManyIdeasCore instance = (ManyIdeasCore) (Object) this;

        // --- COMMON INITIALIZATION (Safe for both Server and Client) ---
        
        // 1. Register basic factories
        ModBlocksRegisterFactory blocksFactory = (ModBlocksRegisterFactory) registerEventHandler(new ModBlocksRegisterFactory());
        registerEventHandler(new ModArgumentTypesRegisterFactory());
        registerEventHandler(new ModCommandsRegisterFactory());
        ModItemsRegisterFactory itemsFactory = (ModItemsRegisterFactory) registerEventHandler(new ModItemsRegisterFactory());

        // 2. Register Recipes & Ingredients
        registerEventHandler(new ModIngredientSerializersRegisterFactory(instance));
        registerEventHandler(new ModRecipeSerializersRegisterFactory());
        registerEventHandler(new ModRecipeTypesRegisterFactory());

        // 3. Register Network
        registerEventHandler(Network.getInstance());

        // --- SIDE SPECIFIC INITIALIZATION ---

        if (FMLEnvironment.dist.isClient()) {
            // This code ONLY executes on the Physical Client.
            // We use a helper method to instantiate client classes so the 
            // Server's classloader never even "looks" at them.
            ClientInitializer.run(instance, this, blocksFactory, itemsFactory);
        } else {
            System.out.println("[ManyIdeas-Patch] Running on Dedicated Server. Skipping Client-only registrations.");
        }
    }

    /**
     * Internal class to isolate client-only class references.
     * This prevents the "Attempted to load class ... for invalid dist" crash.
     */
    private static class ClientInitializer {
        private static void run(ManyIdeasCore instance, ManyIdeasCoreMixin mixin, ModBlocksRegisterFactory b, ModItemsRegisterFactory i) {
            // Load Config
            ClientConfig config = (ClientConfig) mixin.registerConfig(ClientConfig::new);

            // Register Debug Blocks
            ModDebugBlocksRegisterFactory debug = (ModDebugBlocksRegisterFactory) mixin.registerEventHandler(
                new ModDebugBlocksRegisterFactory(config)
            );

            // Register Creative Tabs
            mixin.registerEventHandler(new ModCreativeModeTabRegisterFactory(config, b, debug, i));

            // Setup Decoration Manager
            PlayerDecorationManager deco = new PlayerDecorationManager();
            ((net.minecraftforge.eventbus.api.IEventBus) mixin.forgeEventBus()).addListener(deco::handlePreRenderPlayerEvent);
            ((net.minecraftforge.eventbus.api.IEventBus) mixin.modEventBus()).addListener(deco::handleFMLClientSetupEvent);
        }
    }
}