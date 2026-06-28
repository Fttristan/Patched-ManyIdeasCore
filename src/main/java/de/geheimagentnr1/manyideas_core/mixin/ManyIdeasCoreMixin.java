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
     * @reason Overwrite initMod using pure reflection to bypass private API signature mismatches.
     */
    @Overwrite(remap = false)
    protected void initMod() {
        ManyIdeasCore instance = (ManyIdeasCore) (Object) this;

        try {
            // Find methods by name ONLY. This bypasses the NoSuchMethodError.
            Method regEvt = _findMethodByName(instance.getClass(), "registerEventHandler");
            Method regCfg = _findMethodByName(instance.getClass(), "registerConfig");
            Method fBus = _findMethodByName(instance.getClass(), "forgeEventBus");
            Method mBus = _findMethodByName(instance.getClass(), "modEventBus");

            // 1. Common Side logic (Safe for Server)
            Object blocksFactory = regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.blocks.ModBlocksRegisterFactory());
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.commands.ModArgumentTypesRegisterFactory());
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.commands.ModCommandsRegisterFactory());
            Object itemsFactory = regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.items.ModItemsRegisterFactory());

            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.recipes.ModIngredientSerializersRegisterFactory(instance));
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.recipes.ModRecipeSerializersRegisterFactory());
            regEvt.invoke(instance, new de.geheimagentnr1.manyideas_core.elements.recipes.ModRecipeTypesRegisterFactory());
            regEvt.invoke(instance, de.geheimagentnr1.manyideas_core.network.Network.getInstance());

            // 2. Side Isolation
            if (FMLEnvironment.dist.isClient()) {
                // Pass everything to a helper class to prevent the server from seeing client classes
                ClientPatchHelper.initClient(instance, regEvt, regCfg, fBus, mBus, blocksFactory, itemsFactory);
            } else {
                System.out.println("[ManyIdeas-Patch] Server detected. Skipping Client registrations.");
            }

        } catch (Exception e) {
            System.err.println("[ManyIdeas-Patch] CRITICAL: Reflection failed!");
            e.printStackTrace();
        }
    }

    private Method _findMethodByName(Class<?> clazz, String name) throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) { // Fixed: 'curr' to 'current'
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(name)) {
                    m.setAccessible(true);
                    return m;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException("Could not find API method: " + name);
    }
}