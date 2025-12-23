package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.server.level.ServerPlayer;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class PlayerRespawnCallback {
   
   public static void onRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive){
      PlayerArchetypeData profile = profile(newPlayer);
      if(profile.hasAbility(ArchetypeRegistry.GIANT_SIZED) || profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM)){
         if(alive){
            profile.setHealthUpdate(oldPlayer.getHealth());
         }else{
            profile.setHealthUpdate(oldPlayer.getMaxHealth());
         }
      }
   }
}
