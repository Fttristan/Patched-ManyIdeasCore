package de.geheimagentnr1.manyideas_core.mixin;

import de.geheimagentnr1.manyideas_core.ManyIdeasCore;
import de.geheimagentnr1.manyideas_core.config.ClientConfig;
import de.geheimagentnr1.manyideas_core.elements.blocks.ModBlocksRegisterFactory;
import de.geheimagentnr1.manyideas_core.elements.blocks.ModDebugBlocksRegisterFactory;
import de.geheimagentnr1.manyideas_core.elements.creative_mod_tabs.ModCreativeModeTabRegisterFactory;
import de.geheimagentnr1.manyideas_core.elements.items.ModItemsRegisterFactory;
import de.geheimagentnr1.manyideas_core.special.decoration_renderer.PlayerDecorationManager;
import net.minecraftforge.eventbus.api.IEventBus;

import java.lang.reflect.Method;
import java.util.function.Function;

public class ClientPatchHelper {
    public static void initClient(Object mod, Method regEvt, Method regCfg, Method fBus, Method mBus, Object bFact, Object iFact) throws Exception {
        // Load Config
        ClientConfig config = (ClientConfig) regCfg.invoke(mod, (Function<ManyIdeasCore, ClientConfig>) ClientConfig::new);

        // Register Debug Blocks
        Object debugFact = regEvt.invoke(mod, new ModDebugBlocksRegisterFactory(config));

        // Register Creative Tabs
        regEvt.invoke(mod, new ModCreativeModeTabRegisterFactory(
            config, 
            (ModBlocksRegisterFactory)bFact, 
            (ModDebugBlocksRegisterFactory)debugFact, 
            (ModItemsRegisterFactory)iFact
        ));

        // Setup Decoration Manager
        PlayerDecorationManager deco = new PlayerDecorationManager();
        ((IEventBus) fBus.invoke(mod)).addListener(deco::handlePreRenderPlayerEvent);
        ((IEventBus) mBus.invoke(mod)).addListener(deco::handleFMLClientSetupEvent);
    }
}