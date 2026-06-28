package de.geheimagentnr1.manyideas_core.mixin;

import de.geheimagentnr1.manyideas_core.ManyIdeasCore;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.lang.reflect.Method;

@Mixin(value = ManyIdeasCore.class, remap = false)
public abstract class ManyIdeasCoreMixin {

    /**
     * @author FTTristan
     * @reason Overwrite initMod with hierarchical reflection to fix registration and server crashes.
     */
    @Overwrite(remap = false)
    protected void initMod() {
        ManyIdeasCore instance = (ManyIdeasCore) (Object) this;
        System.out.println("[ManyIdeas-Patch] Attempting to patch ManyIdeasCore initialization...");

        try {
            // Hierarchical search for methods (checks ManyIdeasCore AND AbstractMod)
            Method regEvt = _findMethod(instance.getClass(), "registerEventHandler", Object.class);
            Method regCfg = _findMethod(instance.getClass(), "registerConfig", java.util.function.Function.class);
            Method fBus = _findMethod(instance.getClass(), "forgeEventBus", new Class[0]);
            Method mBus = _findMethod(instance.getClass(), "modEventBus", new Class[0]);

            // --- 1. COMMON REGISTRATIONS (Required for Tags to work!) ---
            System.out.println("[ManyIdeas-Patch] Registering Blocks and Items...");
            Object blocksFact = regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.blocks.ModBlocksRegisterFactory());
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.commands.ModArgumentTypesRegisterFactory());
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.commands.ModCommandsRegisterFactory());
            Object itemsFact = regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.items.ModItemsRegisterFactory());

            System.out.println("[ManyIdeas-Patch] Registering Recipes and Network...");
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.recipes.ModIngredientSerializersRegisterFactory(instance));
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.recipes.ModRecipeSerializersRegisterFactory());
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.recipes.ModRecipeTypesRegisterFactory());
            regEvt.invoke(instance, de.geheimagentnr1.manyideas_core.network.Network.getInstance());

            // --- 2. SIDE ISOLATION ---
            if (FMLEnvironment.dist.isClient()) {
                System.out.println("[ManyIdeas-Patch] Client detected. Running Client registrations...");
                ClientPatchHelper.initClient(instance, regEvt, regCfg, fBus, mBus, blocksFact, itemsFact);
            } else {
                System.out.println("[ManyIdeas-Patch] Server detected. Successfully skipped Client registrations.");
            }

            System.out.println("[ManyIdeas-Patch] PATCH APPLIED SUCCESSFULLY. Registry should be intact.");

        } catch (Exception e) {
            System.err.println("[ManyIdeas-Patch] CRITICAL ERROR: Initialization failed!");
            e.printStackTrace();
            // If this fails, the server will have missing items/tags!
        }
    }

    /**
     * Finds a method by searching up the class hierarchy.
     */
    private Method _findMethod(Class<?> clazz, String name, Class<?>... params) throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method m = current.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                // If the parameter signature is slightly different, try name-only match
                for (Method m : current.getDeclaredMethods()) {
                    if (m.getName().equals(name)) {
                        m.setAccessible(true);
                        return m;
                    }
                }
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException("Could not find method " + name + " in " + clazz.getName());
    }
}