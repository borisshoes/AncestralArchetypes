package net.borisshoes.ancestralarchetypes.misc;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.SubArchetype;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.mixins.EntityAccessor;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.conditions.Condition;
import net.borisshoes.borislib.conditions.ConditionInstance;
import net.borisshoes.borislib.conditions.ConditionParticles;
import net.borisshoes.borislib.conditions.Conditions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
   // Slows to a crawl without the full root of an infinite Torpor, so jumping still works
   public static final float STUN_TORPOR = 999f;

   // Condition names take the color of the source's subarchetype, an uncolored name is used when it has none
   public static Component conditionName(String key, Entity source){
      MutableComponent name = Component.translatable("text.ancestralarchetypes.condition." + key);
      if(source instanceof ServerPlayer player){
         SubArchetype subArchetype = AncestralArchetypes.profile(player).getSubArchetype();
         if(subArchetype != null) name.withColor(subArchetype.getColor());
      }
      return name;
   }

   // Particles are the condition's defaults when shown, none otherwise
   public static Optional<ConditionParticles> particles(boolean show){
      return show ? Optional.empty() : Optional.of(ConditionParticles.NONE);
   }

   // Refreshes the condition behind a passive ability, it lapses half a second after the last call
   public static void passiveCondition(ServerPlayer player, Holder<Condition> condition, String id, String nameKey, float value, AttributeModifier.Operation operation){
      if(!(value > 0)) return;
      Conditions.addCondition(player.level().getServer(), player, new ConditionInstance(
            condition, AncestralArchetypes.archetypesId(id), 10, value,
            true, particles(false), true,
            operation, null, conditionName(nameKey, player)));
   }

   // Applies a multiplier as an additive magnitude of the amplifier above 1, or an additive percentage of the attenuator below 1
   public static void passiveMultiplier(ServerPlayer player, Holder<Condition> amplifier, Holder<Condition> attenuator, String id, String nameKey, double multiplier){
      if(multiplier > 1){
         passiveCondition(player, amplifier, id, nameKey, (float) (multiplier - 1), AttributeModifier.Operation.ADD_VALUE);
      }else{
         passiveCondition(player, attenuator, id, nameKey, (float) (1 - Math.max(0, multiplier)), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
      }
   }

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
            tag.putString("asset_id", assetId);
            stack.set(DataComponents.EQUIPPABLE, Equippable.CODEC.parse(ops, tag).getOrThrow());
         }catch(Exception e){
            AncestralArchetypes.LOGGER.error("Error decoding metamorph helmet asset id: {}", assetId);
         }
      }
      archetypes$ITEM_DATA.removeProperty(stack, METAMORPH_HIDDEN_MODEL);
      archetypes$ITEM_DATA.removeProperty(stack, METAMORPH_VISIBLE_MODEL);
      archetypes$ITEM_DATA.removeProperty(stack, METAMORPH_HIDDEN_ASSET);
      archetypes$ITEM_DATA.removeProperty(stack, METAMORPH_HELMET_TYPE);
   }
   
   public static void addGlow(ServerPlayer viewer, Entity target, TeamColor color){
      String teamName = "glow_" + viewer.getStringUUID() + "_" + target.getId();
      PlayerTeam team = new PlayerTeam(new Scoreboard(), teamName);
      team.setColor(Optional.of(color));
      team.setAllowFriendlyFire(true);
      team.setSeeFriendlyInvisibles(false);
      team.getPlayers().add(target.getScoreboardName());
      byte flags = target.getEntityData().get(EntityAccessor.getFLAGS());
      byte glowing = (byte) (flags | (1 << 6));
      List<SynchedEntityData.DataValue<?>> entries = List.of(SynchedEntityData.DataValue.create(EntityAccessor.getFLAGS(), glowing));
      viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(), entries));
      viewer.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
      viewer.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
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
   
   public static Pair<Integer, Integer> getNearbyPackHunterAllies(ServerPlayer player){
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
