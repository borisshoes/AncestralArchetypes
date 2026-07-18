package net.borisshoes.ancestralarchetypes.misc;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.entities.CreakingHeartEntity;
import net.borisshoes.borislib.sequences.CameraKeyframe;
import net.borisshoes.borislib.sequences.CameraPath;
import net.borisshoes.borislib.sequences.InterpolationType;
import net.borisshoes.borislib.sequences.SequenceManager;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

/**
 * Drives the Creaking Heart anchored-respawn flow.
 *
 * <h3>Overview</h3>
 * When a player dies while owning a live {@link CreakingHeartEntity}, the exact server tick of death is recorded on
 * their {@link PlayerArchetypeData} (persisted). When they press <em>Respawn</em>, instead of appearing at their
 * normal spawn they are placed into a spectator {@link CreakingRespawnCutscene} orbiting their heart. Once the
 * configured timer ({@link ArchetypeRegistry#CREAKING_HEART_RESPAWN_TIMER}) has elapsed <em>from the moment of
 * death</em>, they are pulled to their heart.
 *
 * <h3>Robustness</h3>
 * The whole flow is re-derived every tick from a single persisted value (the death tick), so it survives every
 * awkward case:
 * <ul>
 *   <li><b>Heart destroyed mid-flow</b> — the orbit is cancelled and the player is left at their normal respawn
 *       point (the cutscene snapshot).</li>
 *   <li><b>Player lingers on the death screen</b> — the timer still counts from the real death tick, so a long wait
 *       simply shortens (or skips) the orbit.</li>
 *   <li><b>Disconnect at any point</b> — {@link SequenceManager} restores the player on disconnect; on reconnect the
 *       flow resumes from the persisted death tick (re-entering the orbit or finishing at the heart).</li>
 *   <li><b>Server restart</b> — sequences never survive a restart, so a reset tick counter is detected and the flow
 *       is finalised immediately.</li>
 * </ul>
 * <p>
 * The heart's own self-preservation (it must not break from the owner being "out of range" during the respawn
 * screen) and its trial-spawner charging particles are driven inside {@link CreakingHeartEntity}, which reads the
 * same persisted death tick.
 */
public final class CreakingRespawnManager {
   
   /**
    * Orbit radius (blocks) of the respawn camera around the heart.
    */
   private static final double ORBIT_RADIUS = 5.0;
   /**
    * One full revolution of the orbit camera takes this many ticks.
    * The path covers {@code durationTicks / TICKS_PER_REVOLUTION} revolutions for any given cutscene.
    * Previously a {@code Math.max(1, Math.round(...))} clamp made all changes here ineffective —
    * that clamp has been removed; the fraction is now used directly.
    */
   private static final int TICKS_PER_REVOLUTION = 800;
   /**
    * How many ticks the heart suppresses its out-of-range self-destruct after {@link #finalizeToHeart} runs.
    * This covers the window in which a {@link TeleportTransition} (especially cross-dimension) has been issued
    * but {@code player.level()} / entity position have not yet fully settled to the heart's level.
    */
   private static final int POST_FINALIZE_GRACE_TICKS = 20;
   
   private CreakingRespawnManager(){
   }
   
   // ═══════════════════════════════ event hooks ══════════════════════════════
   
   /**
    * {@code ServerLivingEntityEvents.AFTER_DEATH} hook. Records the death tick when the victim is a player who owns a
    * live Creaking Heart, arming the anchored-respawn flow.
    */
   public static void onDeath(LivingEntity entity, DamageSource source){
      if(!(entity instanceof ServerPlayer player)) return;
      MinecraftServer server = player.level().getServer();
      if(server == null) return;
      PlayerArchetypeData profile = profile(player);
      if(!profile.hasAbility(ArchetypeRegistry.CREAKING_HEART)) return;
      CreakingHeartEntity heart = profile.getCreakingHeart();
      if(heart == null || !heart.isAlive()) return;
      
      profile.setCreakingRespawnDeathTick(server.getTickCount());
      player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.creaking_heart_respawn_anchored")
            .withColor(0xfc7812), false);
   }
   
   /**
    * Called from the player-respawn callback with the freshly-respawned player. Immediately advances the flow so the
    * orbit starts (or the player is placed at the heart) on the same tick they leave the death screen.
    */
   public static void onRespawn(ServerPlayer player){
      MinecraftServer server = player.level().getServer();
      if(server == null) return;
      progress(server, player);
   }
   
   /**
    * Called every server tick to advance every online player's anchored-respawn flow.
    */
   public static void tick(MinecraftServer server){
      for(ServerPlayer player : server.getPlayerList().getPlayers()){
         if(profile(player).isCreakingRespawnActive()){
            progress(server, player);
         }
      }
   }
   
   // ═══════════════════════════════ core logic ═══════════════════════════════
   
   private static void progress(MinecraftServer server, ServerPlayer player){
      PlayerArchetypeData profile = profile(player);
      long deathTick = profile.getCreakingRespawnDeathTick();
      if(deathTick <= 0L) return;
      
      int total = CONFIG.getInt(ArchetypeRegistry.CREAKING_HEART_RESPAWN_TIMER);
      long now = server.getTickCount();
      long elapsed = now - deathTick;
      if(elapsed < 0L) elapsed = total; // tick counter reset (server restart) -> finalise immediately
      
      CreakingHeartEntity heart = profile.getCreakingHeart();
      boolean heartAlive = heart != null && heart.isAlive();
      boolean inCutscene = SequenceManager.isInSequence(player.getUUID(), CreakingRespawnCutscene.class);
      
      // Still viewing the death screen — wait for the respawn press. The heart's own tick keeps it alive and charging
      // (it reads the same persisted death tick), so there is nothing to drive here yet.
      if(player.isDeadOrDying()){
         return;
      }
      
      // Heart gone (destroyed / discarded / lost on reconnect) -> abort. Player keeps their normal respawn point.
      if(!heartAlive){
         if(inCutscene) SequenceManager.cancel(player); // restores the pre-orbit (normal respawn) snapshot
         profile.setCreakingRespawnDeathTick(0L);
         player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.creaking_heart_respawn_lost")
               .withColor(TextColor.RED.getValue()), false);
         SoundUtils.playSongToPlayer(player, SoundEvents.CREAKING_DEATH, 1.5f, 0.8f);
         return;
      }
      
      // Timer (measured from the actual death tick) elapsed -> pull the player to their heart.
      if(elapsed >= total){
         if(inCutscene) SequenceManager.cancel(player); // restore snapshot, then override the position below
         finalizeToHeart(player, profile, heart);
         return;
      }
      
      // Time remains -> make sure the orbit cutscene is running. This also transparently resumes the orbit for the
      // remaining time after a reconnect (where the previous sequence was ended by the disconnect handler).
      if(!inCutscene){
         startCutscene(player, heart, (int) (total - elapsed));
      }
   }
   
   private static void startCutscene(ServerPlayer player, CreakingHeartEntity heart, int durationTicks){
      Vec3 focus = heart.position().add(0, heart.getBbHeight() / 2.0, 0);
      CameraPath path = buildOrbitPath(focus, durationTicks);
      SequenceManager.start(player, new CreakingRespawnCutscene(player.getUUID(), path, Math.max(1, durationTicks)));
   }
   
   private static void finalizeToHeart(ServerPlayer player, PlayerArchetypeData profile, CreakingHeartEntity heart){
      ServerLevel heartLevel = (ServerLevel) heart.level();
      Vec3 pos = heart.position();
      
      // Set the grace period BEFORE clearing the death tick so the heart keeps its self-destruct
      // suppressed across the window where TeleportTransition is committed but player.level() /
      // entity position may not yet reflect the new location (especially for cross-dimension teleports).
      profile.setCreakingRespawnGraceTicks(POST_FINALIZE_GRACE_TICKS);
      profile.setCreakingRespawnDeathTick(0L);
      
      // Dramatic arrival burst of trial-spawner particles at the heart, plus feedback for the player.
      heartLevel.sendParticles(ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER,
            pos.x, pos.y + heart.getBbHeight() / 2.0, pos.z, 60, 0.6, 0.6, 0.6, 0.05);
      SoundUtils.playSongToPlayer(player, SoundEvents.CREAKING_HEART_SPAWN, 2.0f, 0.8f);
      SoundUtils.playSongToPlayer(player, SoundEvents.CREAKING_SPAWN, 1.5f, 1.2f);
      player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.creaking_heart_respawn_success")
            .withColor(0xfc7812), false);
      
      // Teleport last; clear fall distance on arrival so landing on/at the floating heart deals no fall damage.
      player.teleport(new TeleportTransition(heartLevel, pos, Vec3.ZERO, player.getYRot(), player.getXRot(),
            entity -> entity.resetFallDistance()));
   }
   
   // ═══════════════════════════════ camera path ══════════════════════════════
   
   /**
    * Builds a smooth, constant-speed camera orbit around {@code focus}.
    * Angular speed = 2π / TICKS_PER_REVOLUTION rad/tick — completely independent of cutscene duration.
    */
   private static CameraPath buildOrbitPath(Vec3 focus, int durationTicks){
      // Fractional revolutions: e.g. 200 ticks / 800 ticks-per-rev = 0.25 revolutions (quarter circle).
      // No integer clamp — the fraction IS the orbit arc.
      double totalRevolutions = (double) durationTicks / TICKS_PER_REVOLUTION;
      int segments = Math.max(8, (int) Math.ceil(totalRevolutions * 8));
      
      CameraPath.Builder builder = CameraPath.builder();
      for(int i = 0; i <= segments; i++){
         double t = (double) i / segments;
         double angle = 2.0 * Math.PI * totalRevolutions * t;
         double camX = focus.x + ORBIT_RADIUS * Math.cos(angle);
         double camZ = focus.z + ORBIT_RADIUS * Math.sin(angle);
         double camY = focus.y + 2.5 + 1.0 * Math.sin(angle * 2.0); // gentle undulation
         
         // Face inward toward the heart.
         double dx = focus.x - camX;
         double dz = focus.z - camZ;
         float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
         double horizDist = Math.sqrt(dx * dx + dz * dz);
         float pitch = (float) Math.toDegrees(Math.atan2(camY - focus.y, horizDist));
         
         CameraKeyframe.Builder kf = CameraKeyframe.at(t).pos(camX, camY, camZ).rot(yaw, pitch);
         if(i > 0) kf.interp(InterpolationType.LINEAR);
         builder.add(kf.build());
      }
      return builder.build();
   }
}
