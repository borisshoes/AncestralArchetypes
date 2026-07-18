package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.misc.TongueAnimation;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.conditions.Condition;
import net.borisshoes.borislib.conditions.ConditionInstance;
import net.borisshoes.borislib.conditions.Conditions;
import net.borisshoes.borislib.timers.GenericTimer;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.TONGUE;

public class TongueItem extends AbilityItem {
   
   public TongueItem(Properties settings){
      super(TONGUE, "Ū", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.COPPER_INGOT;
      }else{
         return Items.BEEF;
      }
   }
   
   @Override
   public InteractionResult use(Level world, Player user, InteractionHand hand){
      if(!(user instanceof ServerPlayer player)) return InteractionResult.PASS;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.ability_on_cooldown").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC), true);
         SoundUtils.playSongToPlayer(player, SoundEvents.FIRE_EXTINGUISH, 0.25f, 0.8f);
         return InteractionResult.PASS;
      }
      
      double baseLength = AncestralArchetypes.CONFIG.getDouble(ArchetypeRegistry.TONGUE_RANGE);
      Vec3 playerPos = player.getEyePosition();
      Vec3 view = player.getForward();
      MinecraftUtils.LasercastResult result = MinecraftUtils.lasercast(world, playerPos, view, baseLength, true, user);
      Vec3 animationTarget = playerPos.add(view.scale(baseLength));
      Entity hitEntity = null;
      
      boolean found = false;
      for(Entity hit : result.sortedHits()){
         if(hit instanceof LivingEntity livingHit){
            double x = playerPos.x() - hit.getX();
            double y = playerPos.y() - hit.getY();
            double z = playerPos.z() - hit.getZ();
            double speed = .1;
            double heightMod = .08;
            BorisLib.addTickTimerCallback(player.level(), new GenericTimer(1, () -> {
               hit.setDeltaMovement(x * speed, y * speed + Math.sqrt(Math.sqrt(x * x + y * y + z * z)) * heightMod, z * speed);
               if(hit instanceof ServerPlayer targetPlayer)
                  targetPlayer.connection.send(new ClientboundSetEntityMotionPacket(targetPlayer));
            }));
            
            float damage = AncestralArchetypes.CONFIG.getFloat(ArchetypeRegistry.TONGUE_DAMAGE);
            livingHit.hurtServer(player.level(), world.damageSources().playerAttack(player), damage);
            
            if(profile.hasAbility(ArchetypeRegistry.BIOME_ADAPTIVE)){
               Holder<Biome> biome = world.getBiome(user.blockPosition());
               Holder<Condition> conditionType;
               float value;
               int duration;
               if(biome.is(BiomeTags.SPAWNS_COLD_VARIANT_FROGS)){
                  conditionType = Conditions.TORPOR;
                  value = AncestralArchetypes.CONFIG.getFloat(ArchetypeRegistry.BIOME_ADAPTIVE_COLD_SLOWNESS_AMPLIFIER);
                  duration = AncestralArchetypes.CONFIG.getInt(ArchetypeRegistry.BIOME_ADAPTIVE_COLD_SLOWNESS_DURATION);
               }else if(biome.is(BiomeTags.SPAWNS_WARM_VARIANT_FROGS)){
                  conditionType = Conditions.VULNERABILITY;
                  value = AncestralArchetypes.CONFIG.getFloat(ArchetypeRegistry.BIOME_ADAPTIVE_HOT_VULNERABILITY);
                  duration = AncestralArchetypes.CONFIG.getInt(ArchetypeRegistry.BIOME_ADAPTIVE_HOT_VULNERABILITY_DURATION);
               }else{
                  conditionType = Conditions.FEEBLE;
                  value = -AncestralArchetypes.CONFIG.getFloat(ArchetypeRegistry.BIOME_ADAPTIVE_TEMPERATE_FEEBLE);
                  duration = AncestralArchetypes.CONFIG.getInt(ArchetypeRegistry.BIOME_ADAPTIVE_TEMPERATE_FEEBLE_DURATION);
               }
               
               Conditions.addCondition(player.level().getServer(), livingHit, new ConditionInstance(
                     conditionType, archetypesId("biome_adaptive_tongue"), duration, value,
                     true, true, false,
                     AttributeModifier.Operation.ADD_VALUE, player.getUUID()));
            }
            
            // Tongue tip ends at the hit entity's center
            animationTarget = hit.position().add(0, hit.getBbHeight() / 2.0, 0);
            hitEntity = livingHit;
            found = true;
            break;
         }
      }
      
      if(!found){
         AABB box = new AABB(playerPos, playerPos).inflate(baseLength + 0.5);
         List<ItemEntity> items = world.getEntities(EntityTypes.ITEM, box, (entity) -> itemInRange(entity.position(), playerPos, result.endPos(), 0.5));
         items.sort(Comparator.comparingDouble((e) -> (double) e.distanceTo(user)));
         
         for(ItemEntity hit : items){
            double x = playerPos.x() - hit.getX();
            double y = playerPos.y() - hit.getY();
            double z = playerPos.z() - hit.getZ();
            double speed = .1;
            double heightMod = .08;
            hit.setDeltaMovement(x * speed, y * speed + Math.sqrt(Math.sqrt(x * x + y * y + z * z)) * heightMod, z * speed);
            // Tongue tip ends at the hit entity's center
            animationTarget = hit.position().add(0, hit.getBbHeight() / 2.0, 0);
            found = true;
            break;
         }
      }
      
      // Spawn the tongue animation visible to all nearby players
      TongueAnimation.spawn(player, animationTarget, hitEntity, baseLength);
      
      int cooldown = CONFIG.getInt(ArchetypeRegistry.TONGUE_COOLDOWN);
      profile(player).setAbilityCooldown(this.ability, found ? cooldown : cooldown / 2);
      SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.INK_SAC_USE, SoundSource.PLAYERS, 1f, 1.5f);
      player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
      return InteractionResult.SUCCESS;
   }
   
   private boolean itemInRange(Vec3 itemPos, Vec3 start, Vec3 end, double activeRange){
      double dist = itemPos.subtract(start).cross(end.subtract(start)).length() / end.subtract(start).length();
      return dist <= activeRange;
   }
}
