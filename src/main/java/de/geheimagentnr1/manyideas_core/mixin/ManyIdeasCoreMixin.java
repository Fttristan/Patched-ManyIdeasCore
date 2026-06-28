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
     * @reason Overwrite initMod to ensure items register on server while skipping client-only classes.
     */
    @Overwrite(remap = false)
    protected void initMod() {
        ManyIdeasCore instance = (ManyIdeasCore) (Object) this;
        System.out.println("[ManyIdeas-Patch] STARTED: Patching ManyIdeasCore initialization...");

        try {
            // Find API methods by name (ignoring parameters/types to avoid NoSuchMethodError)
            Method regEvt = _findMethod(instance.getClass(), "registerEventHandler");
            Method regCfg = _findMethod(instance.getClass(), "registerConfig");
            Method fBus = _findMethod(instance.getClass(), "forgeEventBus");
            Method mBus = _findMethod(instance.getClass(), "modEventBus");

            // --- 1. CORE REGISTRATIONS (This fixes the Rainbow Wool / Tags errors) ---
            System.out.println("[ManyIdeas-Patch] Stage 1: Registering common factories...");
            
            Object blocksFact = regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.blocks.ModBlocksRegisterFactory());
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.commands.ModArgumentTypesRegisterFactory());
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.commands.ModCommandsRegisterFactory());
            Object itemsFact = regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.items.ModItemsRegisterFactory());

            System.out.println("[ManyIdeas-Patch] Stage 2: Registering recipes and network...");
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.recipes.ModIngredientSerializersRegisterFactory(instance));
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.recipes.ModRecipeSerializersRegisterFactory());
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.recipes.ModRecipeTypesRegisterFactory());
            regEvt.invoke(instance, de.geheimagentnr1.manyideas_core.network.Network.getInstance());

            // --- 2. CLIENT-ONLY REGISTRATIONS ---
            if (FMLEnvironment.dist.isClient()) {
                System.out.println("[ManyIdeas-Patch] Stage 3: Client detected. Running UI/Config registrations...");
                ClientPatchHelper.initClient(instance, regEvt, regCfg, fBus, mBus, blocksFact, itemsFact);
            } else {
                System.out.println("[ManyIdeas-Patch] Stage 3: Server detected. Safely skipping UI/Config registrations.");
            }

            System.out.println("[ManyIdeas-Patch] SUCCESS: Initialization completed without crashing.");

        } catch (Exception e) {
            System.err.println("[ManyIdeas-Patch] CRITICAL FAILURE: Reflection failed. Items will be missing!");
            e.printStackTrace();
        }
    }

    private Method _findMethod(Class<?> clazz, String name) throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(name)) {
                    m.setAccessible(true);
                    return m;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException("ManyIdeas-Patch: Could not find " + name);
    }
}