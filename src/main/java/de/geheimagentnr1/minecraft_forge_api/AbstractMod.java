package de.geheimagentnr1.minecraft_forge_api;

import net.minecraftforge.eventbus.api.IEventBus;
import java.util.function.Function;
import de.geheimagentnr1.manyideas_core.ManyIdeasCore;

public abstract class AbstractMod {
    public Object registerEventHandler(Object handler) { return handler; }
    
    // Updated generics to match the ManyIdeasCore mod instance
    public <T> T registerConfig(Function<ManyIdeasCore, T> configFactory) { return null; }
    
    public IEventBus forgeEventBus() { return null; }
    public IEventBus modEventBus() { return null; }
    protected abstract void initMod();
}