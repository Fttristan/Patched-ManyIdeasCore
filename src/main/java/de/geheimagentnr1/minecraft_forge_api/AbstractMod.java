package de.geheimagentnr1.minecraft_forge_api;

import net.minecraftforge.eventbus.api.IEventBus;
import java.util.function.Function;

public abstract class AbstractMod {
    public <T> T registerEventHandler(T handler) { return null; }
    public <T> T registerConfig(Function<? extends AbstractMod, T> configFactory) { return null; }
    public IEventBus forgeEventBus() { return null; }
    public IEventBus modEventBus() { return null; }
    protected abstract void initMod();
}