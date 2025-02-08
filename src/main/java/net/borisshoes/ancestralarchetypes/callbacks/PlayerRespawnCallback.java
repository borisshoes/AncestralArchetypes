package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class PlayerRespawnCallback {
   
   public static void onRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive){
      IArchetypeProfile profile = profile(newPlayer);
      if(profile.hasAbility(ArchetypeRegistry.GIANT_SIZED) || profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM)){
         if(alive){
            profile.setHealthUpdate(oldPlayer.getHealth());
         }else{
            profile.setHealthUpdate(oldPlayer.getMaxHealth());
         }
      }
   }
}
