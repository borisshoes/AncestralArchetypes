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
      IArchetypeProfile profile = profile(player);
      if((player.getHealth() > 20 && player.getMaxHealth() > 20) || profile.getDeathReductionSizeLevel() != 0){
         if(profile.hasAbility(ArchetypeRegistry.GIANT_SIZED) || profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM) || profile.getDeathReductionSizeLevel() != 0){
            profile.setHealthUpdate(player.getHealth());
         }
      }
      
      
   }
}
