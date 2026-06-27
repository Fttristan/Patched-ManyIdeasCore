package de.geheimagentnr1.minecraft_forge_api;

// Satisfies the "cannot access" errors for the various factories and interfaces
namespace de.geheimagentnr1.minecraft_forge_api.elements.blocks { public class BlocksRegisterFactory {} }
namespace de.geheimagentnr1.minecraft_forge_api.elements.items { public class ItemsRegisterFactory {} }
namespace de.geheimagentnr1.minecraft_forge_api.network { public class AbstractNetwork {} }
namespace de.geheimagentnr1.minecraft_forge_api.events { 
    public interface ModEventHandlerInterface {} 
    public interface ForgeEventHandlerInterface {} 
}