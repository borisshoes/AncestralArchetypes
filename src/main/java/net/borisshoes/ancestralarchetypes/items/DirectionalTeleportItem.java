package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.misc.TeleportIndicator;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public abstract class DirectionalTeleportItem extends AbilityItem {
   private static final List<DirectionalTeleportItem> ALL_INSTANCES = new ArrayList<>();
   
   private static final double STABILITY_THRESHOLD = 1.5;
   private static final double MIN_ALIGNMENT_DOT = 0.70;
   private static final double PERPENDICULAR_TOLERANCE_RATIO = 0.12;
   private static final double DISTANCE_PREFERENCE_THRESHOLD = 4.0;
   
   private final Map<UUID, Vec3> cachedSpots = new HashMap<>();
   
   protected DirectionalTeleportItem(ArchetypeAbility ability, String character, Properties settings) {
      super(ability, character, settings);
      ALL_INSTANCES.add(this);
   }
   
   /** Maximum teleport range in blocks. */
   protected abstract double getMaxRange();
   
   /** Cooldown to apply on successful use, in ticks. */
   protected abstract int getCooldownTicks();
   
   /** Get highlight color for indicator */
   protected abstract int getGlowColor();
   
   /**
    * Extra conditions that must be true for the ability to be usable and for the indicator to appear.
    * Called each inventory tick and on use.
    */
   protected boolean canActivate(ServerPlayer player, ServerLevel world) {
      return true;
   }
   
   /**
    * Additional validation applied to each candidate landing spot.
    * Return false to reject a spot that passes the standard ground-support and space-clear checks.
    */
   protected boolean isValidDestination(ServerLevel world, ServerPlayer user, Vec3 spot) {
      return true;
   }
   
   /** Particle effect displayed at the indicator position each tick. */
   protected ParticleOptions getIndicatorParticle() {
      return ParticleTypes.PORTAL;
   }
   
   /** Message shown when the player tries to use the ability but {@link #canActivate} returns false. */
   protected @Nullable Component getNotReadyMessage() {
      return null;
   }
   
   // -------------------------------------------------------------------------
   // Cache helpers
   // -------------------------------------------------------------------------
   
   public void clearCache(UUID playerUUID) {
      cachedSpots.remove(playerUUID);
   }
   
   public void clearAllCaches() {
      cachedSpots.clear();
   }
   
   protected Vec3 getCachedSpot(UUID playerUUID) {
      return cachedSpots.get(playerUUID);
   }
   
   protected void removeCachedSpot(UUID playerUUID) {
      cachedSpots.remove(playerUUID);
   }
   
   /** Clears this player's cached landing spot from every registered DirectionalTeleportItem instance. */
   public static void clearAllPlayerCaches(UUID playerUUID) {
      ALL_INSTANCES.forEach(item -> item.clearCache(playerUUID));
   }
   
   // -------------------------------------------------------------------------
   // Item behaviour
   // -------------------------------------------------------------------------
   
   @Override
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
      super.inventoryTick(stack, world, entity, slot);
      
      if(!(entity instanceof ServerPlayer player)) return;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) <= 0 && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)){
         if(canActivate(player, world)){
            Vec3 spot = findTeleportSpot(world, player);
            if(spot != null){
               Vec3 eyePos = player.getEyePosition();
               TeleportIndicator.show(player, spot.add(0, 1, 0), eyePos, this.ability, getGlowColor());
               world.sendParticles(player, getIndicatorParticle(), true, true, spot.x, spot.y + 1, spot.z, 1, 0.1, 0.1, 0.1, 0.4);
            }
         } else {
            TeleportIndicator.hide(player);
         }
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
      
      if(!canActivate(player, player.level())){
         Component msg = getNotReadyMessage();
         if(msg != null){
            player.sendSystemMessage(msg, true);
            SoundUtils.playSongToPlayer(player, SoundEvents.FIRE_EXTINGUISH, 0.25f, 0.8f);
         }
         player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
         return InteractionResult.FAIL;
      }
      
      if(performTeleport(player.level(), player)){
         profile(player).setAbilityCooldown(this.ability, getCooldownTicks());
         player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
         return InteractionResult.SUCCESS;
      } else {
         player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
         return InteractionResult.FAIL;
      }
   }
   
   protected boolean performTeleport(ServerLevel world, ServerPlayer user){
      UUID playerUUID = user.getUUID();
      Vec3 cachedSpot = cachedSpots.get(playerUUID);
      
      if(cachedSpot != null){
         if(isSpaceClearFor(user, world, cachedSpot) && hasGroundSupport(world, user, cachedSpot) && isValidDestination(world, user, cachedSpot)){
            if(user.randomTeleport(cachedSpot.x, cachedSpot.y, cachedSpot.z, true)){
               TeleportIndicator.hide(user);
               cachedSpots.remove(playerUUID);
               world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS);
               return true;
            }
         }
      }
      
      Vec3 spot = findTeleportSpot(world, user);
      if(spot == null) return false;
      if(user.randomTeleport(spot.x, spot.y, spot.z, true)){
         TeleportIndicator.hide(user);
         cachedSpots.remove(playerUUID);
         world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS);
         return true;
      }
      return false;
   }
   
   // -------------------------------------------------------------------------
   // Spot-finding logic (shared by both teleport variants)
   // -------------------------------------------------------------------------
   
   protected Vec3 findTeleportSpot(ServerLevel world, ServerPlayer user){
      UUID playerUUID = user.getUUID();
      Vec3 cachedSpot = cachedSpots.get(playerUUID);
      
      if(cachedSpot != null){
         if(isSpaceClearFor(user, world, cachedSpot) && hasGroundSupport(world, user, cachedSpot) && isValidDestination(world, user, cachedSpot)){
            Vec3 origin = user.position();
            Vec3 direction = user.getLookAngle().normalize();
            Vec3 toSpot = cachedSpot.subtract(origin);
            double distanceToSpot = toSpot.length();
            
            double dotProduct = direction.dot(toSpot.normalize());
            double alongRay = direction.dot(toSpot);
            Vec3 pointOnRay = origin.add(direction.scale(alongRay));
            double perpendicularDist = pointOnRay.distanceTo(cachedSpot);
            double maxPerpendicularDist = Math.max(2.0, distanceToSpot * PERPENDICULAR_TOLERANCE_RATIO);
            
            if(dotProduct > MIN_ALIGNMENT_DOT && perpendicularDist < maxPerpendicularDist){
               return cachedSpot;
            }
         }
         cachedSpots.remove(playerUUID);
      }
      
      Vec3 direction = user.getLookAngle().normalize();
      double maxRange = getMaxRange();
      double leniencyRange = 1.5;
      Vec3 origin = user.position();
      double distStep = 0.5;
      double radialStep = 0.5;
      double dropStep = 0.25;
      double maxDistSq = (maxRange + leniencyRange) * (maxRange + leniencyRange);
      Vec3 upRef = Math.abs(direction.y) < 0.999 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
      Vec3 right = direction.cross(upRef).normalize();
      Vec3 up = direction.cross(right).normalize();
      
      for(double d = maxRange; d >= 0.0; d -= distStep){
         Vec3 center = origin.add(direction.scale(d));
         for(double r = 0.0; r <= leniencyRange + 1e-9; r += radialStep){
            int slices = r == 0.0 ? 1 : 12;
            for(int k = 0; k < slices; k++){
               double a = slices == 1 ? 0.0 : (2.0 * Math.PI * k) / slices;
               Vec3 lateral = right.scale(r * Math.cos(a)).add(up.scale(r * Math.sin(a)));
               Vec3 base = center.add(lateral);
               double[] yNudges = new double[]{0.0, 0.5, -0.5, 1.0, -1.0};
               for(double yOff : yNudges){
                  Vec3 candidate = new Vec3(base.x, base.y + yOff, base.z);
                  if(!isSpaceClearFor(user, world, candidate)) continue;
                  if(hasGroundSupport(world, user, candidate) && isValidDestination(world, user, candidate)){
                     boolean shouldUpdate = cachedSpot == null ||
                           cachedSpot.distanceTo(candidate) > STABILITY_THRESHOLD ||
                           candidate.distanceTo(origin) > cachedSpot.distanceTo(origin) + DISTANCE_PREFERENCE_THRESHOLD;
                     if(shouldUpdate){
                        cachedSpots.put(playerUUID, candidate);
                        return candidate;
                     }
                     return cachedSpot;
                  }
                  Vec3 down = candidate;
                  while(origin.distanceToSqr(down) <= maxDistSq && down.y > world.getMinY()){
                     down = down.add(0.0, -dropStep, 0.0);
                     if(!isSpaceClearFor(user, world, down)) break;
                     if(hasGroundSupport(world, user, down) && isValidDestination(world, user, down)){
                        boolean shouldUpdate = cachedSpot == null ||
                              cachedSpot.distanceTo(down) > STABILITY_THRESHOLD ||
                              down.distanceTo(origin) > cachedSpot.distanceTo(origin) + DISTANCE_PREFERENCE_THRESHOLD;
                        if(shouldUpdate){
                           cachedSpots.put(playerUUID, down);
                           return down;
                        }
                        return cachedSpot;
                     }
                  }
               }
            }
         }
      }
      return null;
   }
   
   protected boolean hasGroundSupport(Level world, Entity entity, Vec3 targetPos){
      Vec3 delta = targetPos.subtract(entity.position());
      AABB targetBox = entity.getBoundingBox().move(delta);
      double eps = 1.0 / 16.0;
      AABB floorProbe = new AABB(targetBox.minX, targetBox.minY - eps, targetBox.minZ, targetBox.maxX, targetBox.minY, targetBox.maxZ);
      return world.getBlockCollisions(entity, floorProbe).iterator().hasNext();
   }
   
   protected boolean isSpaceClearFor(Entity entity, Level world, Vec3 targetPos){
      Vec3 delta = targetPos.subtract(entity.position());
      AABB targetBox = entity.getBoundingBox().move(delta);
      return world.noCollision(entity, targetBox, true);
   }
}
