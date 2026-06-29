package net.borisshoes.ancestralarchetypes.misc;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.mixins.EntityAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.List;

public class ArchetypeUtils {
   public static void addGlow(ServerPlayer viewer, Entity target, ChatFormatting color){
      String teamName = "glow_" + viewer.getStringUUID() + "_" + target.getId();
      PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
      team.setColor(color);
      team.setAllowFriendlyFire(true);
      team.setSeeFriendlyInvisibles(false);
      team.getPlayers().add(target.getScoreboardName());
      byte flags = target.getEntityData().get(EntityAccessor.getFLAGS());
      byte glowing = (byte)(flags | (1 << 6));
      List<SynchedEntityData.DataValue<?>> entries = List.of(SynchedEntityData.DataValue.create(EntityAccessor.getFLAGS(), glowing));
      viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), entries));
      viewer.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team,true));
   }
   
   public static void removeGlow(ServerPlayer viewer, Entity target){
      if((target instanceof LivingEntity living && living.isDeadOrDying()) || target.isRemoved()) return;
      String teamName = "glow_" + viewer.getStringUUID() + "_" + target.getId();
      PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
      byte flags = target.getEntityData().get(EntityAccessor.getFLAGS());
      viewer.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
      viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), List.of(SynchedEntityData.DataValue.create(EntityAccessor.getFLAGS(), flags))));
   }
   
   public static boolean shouldProcSkiddish(ServerPlayer player){
      double range = AncestralArchetypes.CONFIG.getDouble(ArchetypeRegistry.SKIDDISH_RANGE);
      boolean allyUnteamedWithRogue = AncestralArchetypes.CONFIG.getBoolean(ArchetypeRegistry.SKIDDISH_ALLY_UNTEAMED_WITH_ROGUE);
      boolean allyTeamedWithRogue = AncestralArchetypes.CONFIG.getBoolean(ArchetypeRegistry.SKIDDISH_ALLY_TEAMED_WITH_ROGUE);
      boolean allyUnteamedWithTeamed = AncestralArchetypes.CONFIG.getBoolean(ArchetypeRegistry.SKIDDISH_ALLY_UNTEAMED_WITH_TEAMED);
      
      PlayerTeam myTeam = player.getTeam();
      List<ServerPlayer> players = player.level().getPlayers(other -> {
         if(other.getUUID().equals(player.getUUID())) return false;
         if(other.distanceTo(player) > range) return false;
         return true;
      });
      for(ServerPlayer other : players){
         PlayerTeam otherTeam = other.getTeam();
         if(myTeam != null){
            if(otherTeam == null && !allyTeamedWithRogue){
               return true;
            }else if(otherTeam != null && !otherTeam.isAlliedTo(myTeam)){
               return true;
            }
         }else{
            if(otherTeam == null && !allyUnteamedWithRogue){
               return true;
            }else if(otherTeam != null && !allyUnteamedWithTeamed){
               return true;
            }
         }
      }
      return false;
   }
   
   public static Tuple<Integer,Integer> getNearbyPackHunterAllies(ServerPlayer player){
      double range = AncestralArchetypes.CONFIG.getDouble(ArchetypeRegistry.PACK_HUNTER_RANGE);
      boolean allyUnteamedWithRogue = AncestralArchetypes.CONFIG.getBoolean(ArchetypeRegistry.PACK_HUNTER_ALLY_UNTEAMED_WITH_ROGUE);
      boolean allyTeamedWithRogue = AncestralArchetypes.CONFIG.getBoolean(ArchetypeRegistry.PACK_HUNTER_ALLY_TEAMED_WITH_ROGUE);
      boolean allyUnteamedWithTeamed = AncestralArchetypes.CONFIG.getBoolean(ArchetypeRegistry.PACK_HUNTER_ALLY_UNTEAMED_WITH_TEAMED);
      
      PlayerTeam myTeam = player.getTeam();
      List<ServerPlayer> players = player.level().getPlayers(other -> {
         if(other.getUUID().equals(player.getUUID())) return false;
         if(other.distanceTo(player) > range) return false;
         PlayerTeam otherTeam = other.getTeam();
         if(myTeam != null){
            if(otherTeam == null){
               return allyTeamedWithRogue;
            }else{
               return otherTeam.isAlliedTo(myTeam);
            }
         }else{
            if(otherTeam == null){
               return allyUnteamedWithRogue;
            }else{
               return allyUnteamedWithTeamed;
            }
         }
      });
      
      int nonPackHunter = 0;
      int packHunter = 0;
      for(ServerPlayer other : players){
         if(AncestralArchetypes.profile(other).hasAbility(ArchetypeRegistry.PACK_HUNTER)){
            packHunter++;
         }else{
            nonPackHunter++;
         }
      }
      return new Tuple<>(packHunter, nonPackHunter);
   }
}
