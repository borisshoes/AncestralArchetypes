package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class PlayerConnectionCallback {
   public static void onPlayerLeave(ServerPlayNetworkHandler handler, MinecraftServer server){
      ServerPlayerEntity player = handler.player;
      if(player.getHealth() > 20 && player.getMaxHealth() > 20){
         IArchetypeProfile profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.TALL_SIZED)){
            profile.setHealthUpdate(player.getHealth());
         }
      }
   }
}
