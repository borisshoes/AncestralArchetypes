package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.minecraft.server.MinecraftServer;

public class ServerStartingCallback {
   public static void serverStarting(MinecraftServer server){
      AncestralArchetypes.SERVER = server;
   }
}
