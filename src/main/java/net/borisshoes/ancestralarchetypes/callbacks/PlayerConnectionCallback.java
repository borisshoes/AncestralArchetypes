package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.items.LongTeleportItem;
import net.borisshoes.borislib.datastorage.DataAccess;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class PlayerConnectionCallback {
   public static void onPlayerLeave(ServerGamePacketListenerImpl handler, MinecraftServer server){
      ServerPlayer player = handler.player;
      PlayerArchetypeData profile = profile(player);
      if((player.getHealth() > 20 && player.getMaxHealth() > 20) || profile.getDeathReductionSizeLevel() != 0){
         if(profile.hasAbility(ArchetypeRegistry.GIANT_SIZED) ||
               profile.hasAbility(ArchetypeRegistry.MASSIVE_SIZED) ||
               profile.hasAbility(ArchetypeRegistry.MOONLIT_FROG) ||
               profile.getDeathReductionSizeLevel() != 0){
            profile.setHealthUpdate(player.getHealth());
         }
      }
      
      if(player.isVehicle()) player.getFirstPassenger().stopRiding();
      LongTeleportItem.clearCache(player.getUUID());
   }
   
   public static void onPlayerJoin(ServerGamePacketListenerImpl handler, PacketSender packetSender, MinecraftServer server){
      ServerPlayer player = handler.player;
      try {
         DataAccess.getPlayer(player.getUUID(), PlayerArchetypeData.KEY).playerJoin(player);
      } catch (Exception e) {
         System.err.println("[AncestralArchetypes] Failed to load player data for " + player.getScoreboardName() + ": " + e.getMessage());
         e.printStackTrace();
      }
   }
}
