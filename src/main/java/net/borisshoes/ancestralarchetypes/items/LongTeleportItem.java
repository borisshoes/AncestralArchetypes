package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.misc.TeleportIndicator;
import net.borisshoes.borislib.utils.SoundUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class LongTeleportItem extends AbilityItem{
   private static final Map<UUID, Vec3> CACHED_SPOTS = new HashMap<>();
   private static final double STABILITY_THRESHOLD = 1.5;
   private static final double MIN_ALIGNMENT_DOT = 0.70; // ~45 degrees - allows precise aiming at long range
   private static final double PERPENDICULAR_TOLERANCE_RATIO = 0.12; // 12% of distance - scales with range
   private static final double DISTANCE_PREFERENCE_THRESHOLD = 4.0; // Prefer spots at least 4 blocks further away
   
   public LongTeleportItem(Properties settings){
      super(ArchetypeRegistry.LONG_TELEPORT, "\uD83D\uDC41", settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      if(PolymerResourcePackUtils.hasMainPack(packetContext)){
         return Items.CLAY_BALL;
      }else{
         return Items.ENDER_PEARL;
      }
   }
   
   @Override
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      
      if(!(entity instanceof ServerPlayer player)) return;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) <= 0 && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)){
         Vec3 spot = findTeleportSpot(world,player);
         if(spot != null){
            Vec3 eyePos = player.getEyePosition();
            TeleportIndicator.show(player, spot.add(0,1,0), eyePos);
            world.sendParticles(player, ParticleTypes.PORTAL, true, true, spot.x, spot.y+1, spot.z, 1, 0.1, 0.1, 0.1, 0.4);
         }
      }
   }
   
   @Override
   public InteractionResult use(Level world, Player user, InteractionHand hand){
      if(!(user instanceof ServerPlayer player)) return InteractionResult.PASS;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.ability_on_cooldown").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.FIRE_EXTINGUISH,0.25f,0.8f);
         return InteractionResult.PASS;
      }
      
      if(teleport(player.level(),player)){
         profile(player).setAbilityCooldown(this.ability, CONFIG.getInt(ArchetypeRegistry.LONG_TELEPORT_COOLDOWN));
         player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
         return InteractionResult.SUCCESS;
      }else{
         player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
         return InteractionResult.FAIL;
      }
   }
   
   private boolean teleport(ServerLevel world, ServerPlayer user){
      UUID playerUUID = user.getUUID();
      Vec3 cachedSpot = CACHED_SPOTS.get(playerUUID);
      
      // Try to use cached spot first (what the indicator is showing)
      if(cachedSpot != null){
         // Safety check - ensure it's still valid
         if(isSpaceClearFor(user, world, cachedSpot) && hasGroundSupport(world, user, cachedSpot)){
            if(user.randomTeleport(cachedSpot.x, cachedSpot.y, cachedSpot.z, true)){
               TeleportIndicator.hide(user);
               CACHED_SPOTS.remove(playerUUID);
               world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS);
               return true;
            }
         }
      }
      
      // Fallback: recalculate if cached spot failed safety check
      Vec3 spot = findTeleportSpot(world, user);
      if(spot == null) return false;
      if(user.randomTeleport(spot.x, spot.y, spot.z, true)){
         TeleportIndicator.hide(user);
         CACHED_SPOTS.remove(playerUUID);
         world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS);
         return true;
      }
      return false;
   }
   
   public static void clearCache(UUID playerUUID){
      CACHED_SPOTS.remove(playerUUID);
   }
   
   public static void clearAllCaches(){
      CACHED_SPOTS.clear();
   }
   
   private Vec3 findTeleportSpot(ServerLevel world, ServerPlayer user){
      UUID playerUUID = user.getUUID();
      Vec3 cachedSpot = CACHED_SPOTS.get(playerUUID);
      
      // If cached spot exists and is still valid, prefer it for stability
      if(cachedSpot != null){
         if(isSpaceClearFor(user, world, cachedSpot) && hasGroundSupport(world, user, cachedSpot)){
            Vec3 origin = user.position();
            Vec3 direction = user.getLookAngle().normalize();
            Vec3 toSpot = cachedSpot.subtract(origin);
            double distanceToSpot = toSpot.length();
            
            // Check alignment with aim direction
            double dotProduct = direction.dot(toSpot.normalize());
            
            // Calculate perpendicular distance from aim ray to cached spot
            double alongRay = direction.dot(toSpot);
            Vec3 pointOnRay = origin.add(direction.scale(alongRay));
            double perpendicularDist = pointOnRay.distanceTo(cachedSpot);
            
            // Scale tolerance with distance - allows precision at long range, tight control at close range
            double maxPerpendicularDist = Math.max(2.0, distanceToSpot * PERPENDICULAR_TOLERANCE_RATIO);
            
            // Keep cached spot if it's well-aligned AND close to aim ray
            if(dotProduct > MIN_ALIGNMENT_DOT && perpendicularDist < maxPerpendicularDist){
               return cachedSpot;
            }
         }
         CACHED_SPOTS.remove(playerUUID);
      }
      
      // Find new spot
      Vec3 direction = user.getLookAngle().normalize();
      double maxRange = CONFIG.getDouble(ArchetypeRegistry.LONG_TELEPORT_DISTANCE);
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
                  if(hasGroundSupport(world, user, candidate)){
                     // Prefer farther spots - update cache if new spot is significantly different or farther
                     boolean shouldUpdate = cachedSpot == null ||
                           cachedSpot.distanceTo(candidate) > STABILITY_THRESHOLD ||
                           candidate.distanceTo(origin) > cachedSpot.distanceTo(origin) + DISTANCE_PREFERENCE_THRESHOLD;
                     
                     if(shouldUpdate){
                        CACHED_SPOTS.put(playerUUID, candidate);
                        return candidate;
                     }
                     return cachedSpot;
                  }
                  Vec3 down = candidate;
                  while(origin.distanceToSqr(down) <= maxDistSq && down.y > world.getMinY()){
                     down = down.add(0.0, -dropStep, 0.0);
                     if(!isSpaceClearFor(user, world, down)) break;
                     if(hasGroundSupport(world, user, down)){
                        // Prefer farther spots - update cache if new spot is significantly different or farther
                        boolean shouldUpdate = cachedSpot == null ||
                              cachedSpot.distanceTo(down) > STABILITY_THRESHOLD ||
                              down.distanceTo(origin) > cachedSpot.distanceTo(origin) + DISTANCE_PREFERENCE_THRESHOLD;
                        
                        if(shouldUpdate){
                           CACHED_SPOTS.put(playerUUID, down);
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
   
   private boolean hasGroundSupport(Level world, Entity entity, Vec3 targetPos){
      Vec3 delta = targetPos.subtract(entity.position());
      AABB targetBox = entity.getBoundingBox().move(delta);
      double eps = 1.0 / 16.0;
      AABB floorProbe = new AABB(targetBox.minX, targetBox.minY - eps, targetBox.minZ, targetBox.maxX, targetBox.minY, targetBox.maxZ);
      return world.getBlockCollisions(entity, floorProbe).iterator().hasNext();
   }
   
   private boolean isSpaceClearFor(Entity entity, Level world, Vec3 targetPos) {
      Vec3 delta = targetPos.subtract(entity.position());
      AABB targetBox = entity.getBoundingBox().move(delta);
      return world.noCollision(entity, targetBox, true);
   }
   
}
