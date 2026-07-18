package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.misc.TeleportIndicator;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
   
   /**
    * Toggle verbose timing logs for spot searches.
    */
   private static final boolean DEBUG = false;
   
   // How often (in ticks) the full spot search runs. The indicator is refreshed every
   // tick so its TTL never expires between searches.
   private static final int SEARCH_INTERVAL = 3;
   // How many ticks the indicator survives a transient canActivate()==false (e.g. the
   // player's onGround() flickering for a tick) before it is hidden. Stops blinking.
   private static final int READY_GRACE_TICKS = 5;
   
   // --- Search tuning ---------------------------------------------------------
   private static final double RAY_STEP = 0.5;          // step along the look ray
   private static final double MIN_ALONG = 1.5;         // ignore spots right on top of the player
   private static final double RANGE_LENIENCY = 1.0;    // allow reaching slightly past max range
   private static final double MAX_PERP = 3.0;          // max distance from the crosshair ray to accept
   private static final int SURFACE_UP_WINDOW = 3;      // how far above the ray to look for a surface
   private static final int SURFACE_DOWN_WINDOW = 6;    // how far below the ray (lateral columns)
   private static final int CENTER_DEEP_DROP = 40;      // deep drop under the crosshair (cliffs / caves)
   private static final double DISTANCE_REWARD = 2.0;   // preference for farther spots (emphasize distance)
   private static final double[] LATERAL_RADII = {1.0, 2.0}; // perpendicular leniency rings
   private static final int LATERAL_SLICES = 8;
   private static final int MAX_VALIDATIONS = 20;       // cap expensive checks per search
   
   // --- Stability (hysteresis) ------------------------------------------------
   // New spot must beat the cached spot's score by this margin to replace it.
   private static final double SWITCH_MARGIN = 1.0;
   // Cached spot is dropped once the crosshair has moved this far off it.
   private static final double KEEP_MAX_PERP = 4.0;
   
   private final Map<UUID, Vec3> cachedSpots = new HashMap<>();
   private final Map<UUID, Integer> readyGrace = new HashMap<>();
   
   protected DirectionalTeleportItem(ArchetypeAbility ability, String character, Properties settings){
      super(ability, character, settings);
      ALL_INSTANCES.add(this);
   }
   
   /**
    * Maximum teleport range in blocks.
    */
   protected abstract double getMaxRange();
   
   /**
    * Cooldown to apply on successful use, in ticks.
    */
   protected abstract int getCooldownTicks();
   
   /**
    * Get highlight color for indicator
    */
   protected abstract int getGlowColor();
   
   /**
    * Extra conditions that must be true for the ability to be usable and for the indicator to appear.
    * Called each inventory tick and on use.
    */
   protected boolean canActivate(ServerPlayer player, ServerLevel world){
      return true;
   }
   
   /**
    * Additional validation applied to each candidate landing spot during search.
    * Return false to reject a spot that passes the standard ground-support and space-clear checks.
    */
   protected boolean isValidDestination(ServerLevel world, ServerPlayer user, Vec3 spot){
      return true;
   }
   
   /**
    * Validation used when checking a <em>cached</em> spot.
    * Defaults to {@link #isValidDestination}; subclasses may override to skip expensive
    * re-checks (e.g. path-connectivity) that are not needed once a spot is already cached.
    */
   protected boolean isValidCachedDestination(ServerLevel world, ServerPlayer user, Vec3 spot){
      return isValidDestination(world, user, spot);
   }
   
   /**
    * Particle effect displayed at the indicator position each tick.
    */
   protected ParticleOptions getIndicatorParticle(){
      return ParticleTypes.PORTAL;
   }
   
   /**
    * Message shown when the player tries to use the ability but {@link #canActivate} returns false.
    */
   protected @Nullable Component getNotReadyMessage(){
      return null;
   }
   
   // -------------------------------------------------------------------------
   // Cache helpers
   // -------------------------------------------------------------------------
   
   public void clearCache(UUID playerUUID){
      cachedSpots.remove(playerUUID);
      readyGrace.remove(playerUUID);
   }
   
   public void clearAllCaches(){
      cachedSpots.clear();
      readyGrace.clear();
   }
   
   protected Vec3 getCachedSpot(UUID playerUUID){
      return cachedSpots.get(playerUUID);
   }
   
   protected void removeCachedSpot(UUID playerUUID){
      cachedSpots.remove(playerUUID);
   }
   
   /**
    * Clears this player's cached landing spot from every registered DirectionalTeleportItem instance.
    */
   public static void clearAllPlayerCaches(UUID playerUUID){
      ALL_INSTANCES.forEach(item -> item.clearCache(playerUUID));
   }
   
   // -------------------------------------------------------------------------
   // Item behaviour
   // -------------------------------------------------------------------------
   
   @Override
   public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot){
      super.inventoryTick(stack, world, entity, slot);
      
      if(!(entity instanceof ServerPlayer player)) return;
      PlayerArchetypeData profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0 || !(slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND))
         return;
      
      UUID uuid = player.getUUID();
      
      // canActivate() can flicker for a single tick (e.g. onGround() briefly false while
      // walking). Debounce it with a short grace window so the indicator doesn't blink.
      boolean ready = canActivate(player, world);
      int grace = ready ? READY_GRACE_TICKS : Math.max(0, readyGrace.getOrDefault(uuid, 0) - 1);
      readyGrace.put(uuid, grace);
      
      if(!ready){
         if(grace <= 0){
            TeleportIndicator.hide(player);
            cachedSpots.remove(uuid);
            return;
         }
         // Transient not-ready: keep showing the last spot, skip the expensive recompute.
         Vec3 held = cachedSpots.get(uuid);
         if(held != null){
            TeleportIndicator.show(player, held.add(0, 1, 0), player.getEyePosition(), this.ability, getGlowColor());
         }
         return;
      }
      
      // Run the expensive spot search only every SEARCH_INTERVAL ticks. The indicator is
      // refreshed every tick so its TTL (=3) never reaches zero between searches.
      boolean shouldRecompute = (world.getServer().getTickCount() % SEARCH_INTERVAL == 0);
      if(shouldRecompute){
         long t0 = System.nanoTime();
         findTeleportSpot(world, player); // updates cachedSpots
         if(DEBUG){
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            if(ms > 2) System.out.printf("[DirectionalTeleport] findTeleportSpot %.1fms (%s %s)%n",
                  (double) ms, getClass().getSimpleName(), player.getName().getString());
         }
      }
      
      Vec3 spot = cachedSpots.get(uuid);
      if(spot != null){
         TeleportIndicator.show(player, spot.add(0, 1, 0), player.getEyePosition(), this.ability, getGlowColor());
         if(shouldRecompute){
            world.sendParticles(player, getIndicatorParticle(), true, true, spot.x, spot.y + 1, spot.z, 1, 0.1, 0.1, 0.1, 0.4);
         }
      }else if(shouldRecompute){
         TeleportIndicator.hide(player);
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
      }else{
         player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), player.getUsedItemHand() == InteractionHand.MAIN_HAND ? 36 + player.getInventory().getSelectedSlot() : 45, player.getItemInHand(hand)));
         return InteractionResult.FAIL;
      }
   }
   
   protected boolean performTeleport(ServerLevel world, ServerPlayer user){
      UUID playerUUID = user.getUUID();
      
      // Teleport to exactly the spot the indicator is showing. Only re-search if the
      // cached spot is missing or has become physically invalid — this keeps the landing
      // consistent with the indicator the player was aiming with.
      Vec3 spot = cachedSpots.get(playerUUID);
      if(spot == null || !isSpaceClearFor(user, world, spot) || !hasGroundSupport(world, user, spot) || !isValidDestination(world, user, spot)){
         cachedSpots.remove(playerUUID);
         spot = findTeleportSpot(world, user);
      }
      if(spot == null) return false;
      
      if(user.randomTeleport(spot.x, spot.y, spot.z, true)){
         TeleportIndicator.hide(user);
         cachedSpots.remove(playerUUID);
         readyGrace.remove(playerUUID);
         world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS);
         return true;
      }
      return false;
   }
   
   // -------------------------------------------------------------------------
   // Spot-finding logic (shared by both teleport variants)
   // -------------------------------------------------------------------------
   
   /**
    * Finds the best landing spot for the player's current aim and stores it in the cache.
    *
    * <p>The search treats the crosshair as a ray and looks for the valid landing spot that
    * is <em>closest to that ray</em> (so tall thin pillars no longer hijack the target),
    * while giving a mild preference to farther spots. It happily reaches through walls and
    * down into caves because it never requires line of sight — only a physically safe
    * landing (clear space + solid ground + {@link #isValidDestination}).</p>
    *
    * <p>Performance: cheap surface probes (block-solidity only, cached) enumerate and score
    * all candidates; the expensive collision / path validation runs on only the best few.
    * A hysteresis pass keeps the previously shown spot unless a new one is clearly better,
    * so the indicator stays stable instead of flickering.</p>
    */
   protected Vec3 findTeleportSpot(ServerLevel world, ServerPlayer user){
      UUID playerUUID = user.getUUID();
      Vec3 origin = user.position();
      Vec3 direction = user.getLookAngle().normalize();
      double maxRange = getMaxRange();
      double maxAlong = maxRange + RANGE_LENIENCY;
      
      long searchStart = System.nanoTime();
      int[] candidatesChecked = {0};
      
      // Basis perpendicular to the look direction for the leniency rings.
      Vec3 upRef = Math.abs(direction.y) < 0.999 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
      Vec3 right = direction.cross(upRef).normalize();
      Vec3 up = direction.cross(right).normalize();
      
      Map<Long, Boolean> solidCache = new HashMap<>();
      Map<Long, ScoredSpot> candidates = new HashMap<>();
      
      for(double d = MIN_ALONG; d <= maxRange + 1e-9; d += RAY_STEP){
         Vec3 rayPt = origin.add(direction.scale(d));
         // Center column gets a deep downward drop so aiming over cliffs / into caves works.
         collectColumn(world, user, origin, direction, maxRange, maxAlong, rayPt,
               SURFACE_UP_WINDOW, CENTER_DEEP_DROP, solidCache, candidates, candidatesChecked);
         // Perpendicular leniency rings give lateral tolerance around the crosshair.
         for(double r : LATERAL_RADII){
            for(int k = 0; k < LATERAL_SLICES; k++){
               double a = (2.0 * Math.PI * k) / LATERAL_SLICES;
               Vec3 off = right.scale(r * Math.cos(a)).add(up.scale(r * Math.sin(a)));
               collectColumn(world, user, origin, direction, maxRange, maxAlong, rayPt.add(off),
                     SURFACE_UP_WINDOW, SURFACE_DOWN_WINDOW, solidCache, candidates, candidatesChecked);
            }
         }
      }
      
      // Validate candidates best-first; the first that passes the expensive checks wins.
      List<ScoredSpot> sorted = new ArrayList<>(candidates.values());
      sorted.sort(Comparator.comparingDouble(s -> s.score));
      Vec3 best = null;
      double bestScore = Double.POSITIVE_INFINITY;
      int validations = 0;
      for(ScoredSpot s : sorted){
         if(validations >= MAX_VALIDATIONS) break;
         validations++;
         if(isSpaceClearFor(user, world, s.spot) && hasGroundSupport(world, user, s.spot) && isValidDestination(world, user, s.spot)){
            best = s.spot;
            bestScore = s.score;
            break;
         }
      }
      
      // Hysteresis: prefer keeping the currently shown spot for a stable indicator.
      Vec3 cached = cachedSpots.get(playerUUID);
      boolean cachedUsable = false;
      double cachedScore = Double.POSITIVE_INFINITY;
      if(cached != null){
         double along = direction.dot(cached.subtract(origin));
         if(along >= MIN_ALONG && along <= maxAlong){
            double perp = origin.add(direction.scale(along)).distanceTo(cached);
            if(perp <= KEEP_MAX_PERP && isSpaceClearFor(user, world, cached) && hasGroundSupport(world, user, cached) && isValidCachedDestination(world, user, cached)){
               cachedUsable = true;
               cachedScore = (perp / along) - DISTANCE_REWARD * (along / maxRange);
            }
         }
      }
      
      if(best == null){
         if(cachedUsable){
            logSearch(searchStart, candidatesChecked[0], user, "kept-cached-none");
            return cached;
         }
         cachedSpots.remove(playerUUID);
         logSearch(searchStart, candidatesChecked[0], user, "null");
         return null;
      }
      
      if(cachedUsable && cachedScore <= bestScore + SWITCH_MARGIN){
         logSearch(searchStart, candidatesChecked[0], user, "kept-cached");
         return cached;
      }
      
      cachedSpots.put(playerUUID, best);
      logSearch(searchStart, candidatesChecked[0], user, "found");
      return best;
   }
   
   /**
    * Probes a single column near {@code probePt}, finds the standable surface closest to the
    * ray height, scores it against the crosshair, and records it if it beats any prior
    * candidate for the same block.
    */
   private void collectColumn(ServerLevel world, ServerPlayer user, Vec3 origin, Vec3 direction,
                              double maxRange, double maxAlong, Vec3 probePt, int upWindow, int downWindow,
                              Map<Long, Boolean> solidCache, Map<Long, ScoredSpot> candidates, int[] counter){
      int colX = Mth.floor(probePt.x);
      int colZ = Mth.floor(probePt.z);
      Integer surfaceTop = nearestStandableSurface(world, colX, colZ, probePt.y, upWindow, downWindow, solidCache);
      if(surfaceTop == null) return;
      counter[0]++;
      
      Vec3 feet = new Vec3(colX + 0.5, surfaceTop + 1, colZ + 0.5);
      double along = direction.dot(feet.subtract(origin));
      if(along < MIN_ALONG || along > maxAlong) return;
      double perp = origin.add(direction.scale(along)).distanceTo(feet);
      if(perp > MAX_PERP) return;
      
      double score = (perp / along) - DISTANCE_REWARD * (along / maxRange);
      long key = BlockPos.asLong(colX, surfaceTop, colZ);
      ScoredSpot existing = candidates.get(key);
      if(existing == null || score < existing.score){
         candidates.put(key, new ScoredSpot(feet, score));
      }
   }
   
   /**
    * Scans column {@code (x, z)} for the standable surface (solid block with two air blocks
    * above) whose feet position is nearest to {@code targetY}, searching {@code up} blocks
    * above and {@code down} blocks below. Returns the surface block Y, or {@code null}.
    */
   private static Integer nearestStandableSurface(ServerLevel world, int x, int z, double targetY, int up, int down, Map<Long, Boolean> cache){
      int startY = Mth.floor(targetY);
      Integer best = null;
      double bestDist = Double.POSITIVE_INFINITY;
      for(int y = startY + up; y >= startY - down; y--){
         if(isSolid(world, x, y, z, cache) && !isSolid(world, x, y + 1, z, cache) && !isSolid(world, x, y + 2, z, cache)){
            double dist = Math.abs((y + 1) - targetY);
            if(dist < bestDist){
               bestDist = dist;
               best = y;
            }
         }
      }
      return best;
   }
   
   private static boolean isSolid(ServerLevel world, int x, int y, int z, Map<Long, Boolean> cache){
      long key = BlockPos.asLong(x, y, z);
      Boolean cached = cache.get(key);
      if(cached != null) return cached;
      boolean solid = world.getBlockState(new BlockPos(x, y, z)).isSolidRender();
      cache.put(key, solid);
      return solid;
   }
   
   private void logSearch(long startNs, int candidates, ServerPlayer user, String outcome){
      if(!DEBUG) return;
      long ms = (System.nanoTime() - startNs) / 1_000_000L;
      if(ms > 2) System.out.printf("[DirectionalTeleport] search %dms %d candidates -> %s (%s %s)%n",
            ms, candidates, outcome, getClass().getSimpleName(), user.getName().getString());
   }
   
   private static final class ScoredSpot {
      final Vec3 spot;
      final double score;
      
      ScoredSpot(Vec3 spot, double score){
         this.spot = spot;
         this.score = score;
      }
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
