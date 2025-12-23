package net.borisshoes.ancestralarchetypes.cca;

import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.datastorage.DataAccess;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import static net.borisshoes.ancestralarchetypes.cca.PlayerComponentInitializer.PLAYER_DATA;

public class DataFixer {
   public static void onPlayerJoin(ServerGamePacketListenerImpl handler, PacketSender packetSender, MinecraftServer server){
      ServerPlayer player = handler.getPlayer();
      IArchetypeProfile oldData = PLAYER_DATA.get(player);
      
      if(!oldData.hasData()){
         return;
      }
      PlayerArchetypeData playerData = DataAccess.getPlayer(player.getUUID(), PlayerArchetypeData.KEY);
      if(playerData.getSubArchetype() == null){
         playerData.copyFrom(oldData,player);
      }
      oldData.clearData();
   }
}
