package de.geheimagentnr1.manyideas_core.mixin;

import de.geheimagentnr1.manyideas_core.ManyIdeasCore;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(value = ManyIdeasCore.class, remap = false)
public class ManyIdeasCoreMixin {

    /**
     * Redirects registerConfig. 
     * On Server: Returns null to prevent ClientConfig from loading.
     * On Client: Allows the normal call.
     */
    @Redirect(
        method = "initMod",
        at = @At(value = "INVOKE", target = "Lde/geheimagentnr1/manyideas_core/ManyIdeasCore;registerConfig(Ljava/util/function/Function;)Ljava/lang/Object;"),
        remap = false
    )
    private Object redirectConfig(ManyIdeasCore instance, Function<?, ?> factory) {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            return null; // Skip on server
        }
        return instance.registerConfig(factory);
    }

    /**
     * Redirects registerEventHandler.
     * If the handler being registered is a Client-only factory, we skip it on the server.
     */
    @Redirect(
        method = "initMod",
        at = @At(value = "INVOKE", target = "Lde/geheimagentnr1/manyideas_core/ManyIdeasCore;registerEventHandler(Ljava/lang/Object;)Ljava/lang/Object;"),
        remap = false
    )
    private Object redirectEventHandlers(ManyIdeasCore instance, Object handler) {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            String className = handler.getClass().getName();
            // These two classes crash the server because they reference Client-only code
            if (className.contains("ModDebugBlocksRegisterFactory") || 
                className.contains("ModCreativeModeTabRegisterFactory")) {
                return null; // Skip on server
            }
        }
        return instance.registerEventHandler(handler);
    }
}