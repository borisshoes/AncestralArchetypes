package net.borisshoes.ancestralarchetypes.misc;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.mixins.EntityAccessor;
import net.borisshoes.borislib.BorisLib;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.archetypes$ITEM_DATA;
import static net.borisshoes.ancestralarchetypes.items.MetamorphHeadItem.*;

public class ArchetypeUtils {
   public static void addMetamorphHelmetTags(ItemStack stack, MetamorphTypes type){
      if(stack.isEmpty()) return;
      try{
         if(type == null){
            removeMetamorphHelmetTags(stack);
            return;
         }
         Identifier modelId = type.getBlock().asItem().getDefaultInstance().get(DataComponents.ITEM_MODEL);
         if(Objects.equals(stack.get(DataComponents.ITEM_MODEL), modelId)) return;
         if(archetypes$ITEM_DATA.getStringProperty(stack, METAMORPH_HIDDEN_ASSET).isEmpty()){
            if(stack.has(DataComponents.EQUIPPABLE)){
               Equippable equip = stack.get(DataComponents.EQUIPPABLE);
               if(equip.assetId().isPresent()){
                  archetypes$ITEM_DATA.putProperty(stack, METAMORPH_HIDDEN_ASSET, equip.assetId().get().identifier().toString());
                  DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, BorisLib.SERVER.registryAccess());
                  CompoundTag tag = (CompoundTag) Equippable.CODEC.encodeStart(ops, equip).getOrThrow();
                  tag.remove("asset_id");
                  stack.set(DataComponents.EQUIPPABLE, Equippable.CODEC.parse(ops, tag).getOrThrow());
               }
            }
         }
         if(archetypes$ITEM_DATA.getStringProperty(stack, METAMORPH_HIDDEN_MODEL).isEmpty()){
            archetypes$ITEM_DATA.putProperty(stack, METAMORPH_HIDDEN_MODEL, stack.get(DataComponents.ITEM_MODEL).toString());
         }
         archetypes$ITEM_DATA.putProperty(stack, METAMORPH_HELMET_TYPE, type.toString());
         archetypes$ITEM_DATA.putProperty(stack, METAMORPH_VISIBLE_MODEL, modelId.toString());
         stack.set(DataComponents.ITEM_MODEL, modelId);
      }catch(Exception e){
         AncestralArchetypes.LOGGER.error("Error setting metamorph helmet tags: {} {}", stack.toString(), type.toString());
      }
   }
   
   public static void removeMetamorphHelmetTags(ItemStack stack){
      if(stack.isEmpty()) return;
      String hiddenHelmetModel = archetypes$ITEM_DATA.getStringProperty(stack, METAMORPH_HIDDEN_MODEL);
      if(hiddenHelmetModel.isEmpty()) return;
      String assetId = archetypes$ITEM_DATA.getStringProperty(stack, METAMORPH_HIDDEN_ASSET);
      stack.set(DataComponents.ITEM_MODEL, Identifier.parse(hiddenHelmetModel));
      if(stack.has(DataComponents.EQUIPPABLE) && !assetId.isEmpty()){
         try{
            Equippable equip = stack.get(DataComponents.EQUIPPABLE);
            DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, BorisLib.SERVER.registryAccess());
            CompoundTag tag = (CompoundTag) Equippable.CODEC.encodeStart(ops, equip).getOrThrow();
            stack.set(DataComponents.EQUIPPABLE, Equippable.CODEC.parse(ops, tag).getOrThrow());
         }catch(Exception e){
            AncestralArchetypes.LOGGER.error("Error decoding metamorph helmet asset id: {}", assetId);
         }
      }
      archetypes$ITEM_DATA.removeProperty(stack, METAMORPH_HIDDEN_MODEL);
      archetypes$ITEM_DATA.removeProperty(stack, METAMORPH_VISIBLE_MODEL);
      archetypes$ITEM_DATA.removeProperty(stack, METAMORPH_HIDDEN_ASSET);
      archetypes$ITEM_DATA.removeProperty(stack, METAMORPH_HELMET_TYPE);
      System.out.println("Set Model to "+hiddenHelmetModel);
   }
   
   public static void addGlow(ServerPlayer viewer, Entity target, TeamColor color){
      String teamName = "glow_" + viewer.getStringUUID() + "_" + target.getId();
      PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
      team.setColor(Optional.of(color));
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
   
   public static Pair<Integer,Integer> getNearbyPackHunterAllies(ServerPlayer player){
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
      return new Pair<>(packHunter, nonPackHunter);
   }
}
