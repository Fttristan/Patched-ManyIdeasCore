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
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.function.Function;

@Mixin(value = ManyIdeasCore.class, remap = false)
public abstract class ManyIdeasCoreMixin {

    /**
     * This @Inject at HEAD runs BEFORE the original code. 
     * We run our fixed logic and then call ci.cancel() to prevent the 
     * original, crashing code from ever running.
     */
    @Inject(method = "initMod", at = @At("HEAD"), cancellable = true, remap = false)
    private void onInitMod(CallbackInfo ci) {
        ManyIdeasCore instance = (ManyIdeasCore) (Object) this;

        try {
            // Use Reflection to find the API methods in the parent class
            Method regEvt = _findMethod(instance.getClass(), "registerEventHandler", Object.class);
            Method regCfg = _findMethod(instance.getClass(), "registerConfig", Function.class);
            Method fBus = _findMethod(instance.getClass(), "forgeEventBus");
            Method mBus = _findMethod(instance.getClass(), "modEventBus");

            // 1. Common Side logic (Safe for both)
            ModBlocksRegisterFactory blocks = (ModBlocksRegisterFactory) regEvt.invoke(instance, new ModBlocksRegisterFactory());
            regEvt.invoke(instance, new ModArgumentTypesRegisterFactory());
            regEvt.invoke(instance, new ModCommandsRegisterFactory());
            ModItemsRegisterFactory items = (ModItemsRegisterFactory) regEvt.invoke(instance, new ModItemsRegisterFactory());

            regEvt.invoke(instance, new ModIngredientSerializersRegisterFactory(instance));
            regEvt.invoke(instance, new ModRecipeSerializersRegisterFactory());
            regEvt.invoke(instance, new ModRecipeTypesRegisterFactory());
            regEvt.invoke(instance, Network.getInstance());

            // 2. Safe Side Isolation
            if (FMLEnvironment.dist.isClient()) {
                // We only run this if we are a physical client
                ClientConfig config = (ClientConfig) regCfg.invoke(instance, (Function<ManyIdeasCore, ClientConfig>) ClientConfig::new);
                ModDebugBlocksRegisterFactory debug = (ModDebugBlocksRegisterFactory) regEvt.invoke(instance, new ModDebugBlocksRegisterFactory(config));

                regEvt.invoke(instance, new ModCreativeModeTabRegisterFactory(config, blocks, debug, items));

                PlayerDecorationManager decos = new PlayerDecorationManager();
                ((IEventBus) fBus.invoke(instance)).addListener(decos::handlePreRenderPlayerEvent);
                ((IEventBus) mBus.invoke(instance)).addListener(decos::handleFMLClientSetupEvent);
            }

            // SUCCESS: Cancel the original method so the crashing code never runs
            ci.cancel();

        } catch (Exception e) {
            System.err.println("[ManyIdeasCore-Patch] CRITICAL ERROR: Failed to apply server fix via reflection!");
            e.printStackTrace();
            // If reflection fails, we don't cancel, so the original code runs (and crashes)
        }
    }

    private Method _findMethod(Class<?> clazz, String name, Class<?>... params) throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method m = current.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }
}