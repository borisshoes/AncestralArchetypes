package net.borisshoes.ancestralarchetypes.misc;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.tracker.PlayerMovementEntry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class TongueAnimation {
   private static final int EXTEND_TICKS = 5;
   private static final int RETRACT_TICKS = 5;
   private static final float TONGUE_THICKNESS = 0.15f;
   private static final int TELEPORT_DURATION = 3;
   private static final int CHASE_DURATION = 2;
   private static final double VELOCITY_LEAD = 3.0;
   
   private static final Map<UUID, TongueAnimation> ANIMATIONS = new HashMap<>();
   
   private final ServerPlayer caster;
   private final Entity hitEntity;   // null if no entity was hit
   private final Vec3 fixedTarget;   // used when hitEntity is null or dead
   private final float fullLength;
   private final double cancelDistanceSq; // (3 * tongueRange)^2
   private final float tongueThickness;   // TONGUE_THICKNESS * player.getScale()
   private final ElementHolder holder;
   private final BlockDisplayElement display;
   private int serverTick;
   private Phase phase;
   
   private enum Phase {EXTENDING, RETRACTING, DONE}
   
   /**
    * Spawns a tongue animation for the given player.
    *
    * @param player      The player using the tongue
    * @param targetPos   The initial tip position of the tongue
    * @param hitEntity   The living entity that was hit, or null
    * @param tongueRange The configured tongue range (used for the cancel threshold: 3x range)
    */
   public static void spawn(ServerPlayer player, Vec3 targetPos, Entity hitEntity, double tongueRange){
      TongueAnimation existing = ANIMATIONS.remove(player.getUUID());
      if(existing != null) existing.destroy();
      
      Vec3 anchorPos = player.position().add(0, player.getEyeHeight() * 0.8, 0);
      if(anchorPos.distanceTo(targetPos) < 0.1) return;
      
      TongueAnimation anim = new TongueAnimation(player, anchorPos, targetPos, hitEntity, tongueRange);
      ANIMATIONS.put(player.getUUID(), anim);
   }
   

   public static void tickAll(){
      if(ANIMATIONS.isEmpty()) return;
      Iterator<TongueAnimation> it = ANIMATIONS.values().iterator();
      while(it.hasNext()){
         TongueAnimation anim = it.next();
         anim.tick();
         if(anim.phase == Phase.DONE){
            anim.destroy();
            it.remove();
         }
      }
   }
   
   private Vec3 computeAnchor(){
      return caster.position().add(0, caster.getEyeHeight() * 0.8, 0);
   }
   
   private Vec3 computeTarget(){
      if(hitEntity != null && hitEntity.isAlive()){
         return hitEntity.position().add(0, hitEntity.getBbHeight() / 2.0, 0);
      }
      return fixedTarget;
   }
   
   private TongueAnimation(ServerPlayer player, Vec3 initialAnchor, Vec3 initialTarget, Entity hitEntity, double tongueRange){
      this.caster = player;
      this.hitEntity = hitEntity;
      this.fixedTarget = initialTarget;
      this.fullLength = (float) initialAnchor.distanceTo(initialTarget);
      this.cancelDistanceSq = Math.pow(tongueRange * 3.0, 2);
      this.tongueThickness = TONGUE_THICKNESS * player.getScale();
      this.serverTick = 0;
      this.phase = Phase.EXTENDING;
      this.holder = new ElementHolder();
      this.display = new BlockDisplayElement(Blocks.RED_TERRACOTTA.defaultBlockState());
      this.display.setScale(new Vector3f(tongueThickness, tongueThickness, 0.001f));
      this.display.setTranslation(new Vector3f(0, (float) (player.getEyeHeight() * 0.8), 0));
      this.display.setTeleportDuration(TELEPORT_DURATION);
      applyRotation(initialAnchor, initialTarget);
      this.display.setBrightness(Brightness.FULL_BRIGHT);
      this.display.setViewRange(1.0f);
      this.holder.addElement(this.display);
      EntityAttachment.ofTicking(this.holder, player);
      
      for(ServerPlayer p : player.level().players()){
         if(p.distanceToSqr(player) < 64.0 * 64.0){
            this.holder.startWatching(p);
         }
      }
   }
   
   private void applyRotation(Vec3 anchor, Vec3 target){
      Vec3 dir = target.subtract(anchor).normalize();
      Vector3f dirF = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
      Vector3f zAxis = new Vector3f(0f, 0f, 1f);
      Quaternionf rotation;
      if(dirF.dot(zAxis) < -0.9999f){
         rotation = new Quaternionf().rotateY((float) Math.PI);
      }else{
         rotation = new Quaternionf().rotationTo(zAxis, dirF);
      }
      this.display.setLeftRotation(rotation);
   }
   
   private Vector3f leadTranslation(){
      PlayerMovementEntry tracker = BorisLib.PLAYER_MOVEMENT_TRACKER.get(caster);
      Vec3 vel = tracker == null ? Vec3.ZERO : tracker.velocity();
      float yOffset = (float) (caster.getEyeHeight() * 0.8);
      return new Vector3f(
            (float) (vel.x * VELOCITY_LEAD),
            yOffset + (float) (vel.y * VELOCITY_LEAD),
            (float) (vel.z * VELOCITY_LEAD));
   }
   
   private float lengthForTick(int t){
      if(t <= 0) return 0.001f;
      if(t <= EXTEND_TICKS){
         return Math.max(0.001f, this.fullLength * ((float) t / EXTEND_TICKS));
      }
      int retractTick = t - EXTEND_TICKS;
      return Math.max(0.001f, this.fullLength * (1.0f - (float) retractTick / RETRACT_TICKS));
   }
   
   private void tick(){
      Vec3 anchor = computeAnchor();
      Vec3 target = computeTarget();
      
      // Cancel if the two ends have drifted more than 3x the tongue range apart
      if(anchor.distanceToSqr(target) > this.cancelDistanceSq){
         this.phase = Phase.DONE;
         return;
      }
      
      serverTick++;
      
      if(this.phase == Phase.EXTENDING && serverTick > EXTEND_TICKS){
         this.phase = Phase.RETRACTING;
      }
      if(serverTick >= EXTEND_TICKS + RETRACT_TICKS){
         this.phase = Phase.DONE;
         return;
      }
      
      applyRotation(anchor, target);
      this.display.setTranslation(leadTranslation());
      this.display.setScale(new Vector3f(tongueThickness, tongueThickness, lengthForTick(serverTick)));
      this.display.setInterpolationDuration(CHASE_DURATION);
      this.display.setStartInterpolation(-1);
      this.display.startInterpolation();
   }
   
   private void destroy(){
      this.holder.destroy();
   }
}
