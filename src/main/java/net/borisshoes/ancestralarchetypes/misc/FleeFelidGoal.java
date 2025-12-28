package net.borisshoes.ancestralarchetypes.misc;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class FleeFelidGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
   
   public FleeFelidGoal(Creeper creeper, Class<T> fleeFromType, float distance, double slowSpeed, double fastSpeed){
      super(creeper, fleeFromType, distance, slowSpeed, fastSpeed, Objects.requireNonNull(EntitySelector.NO_CREATIVE_OR_SPECTATOR)::test);
   }
   
   @Override
   public boolean canUse(){
      this.toAvoid = getServerLevel(this.mob).getNearestEntity(
            this.mob.level().getEntitiesOfClass(this.avoidClass, this.mob.getBoundingBox().inflate((double) this.maxDist, 3.0, (double) this.maxDist), (livingEntity) -> true),
            TargetingConditions.forCombat()
                  .range((double)this.maxDist)
                  .selector((entity, world) -> predicateOnAvoidEntity.test(entity) && avoidPredicate.test(entity)),
            this.mob, this.mob.getX(), this.mob.getY(), this.mob.getZ());
      if(this.toAvoid == null){
         return false;
      }else{
         Vec3 vec3d = DefaultRandomPos.getPosAway(this.mob, 16, 7, this.toAvoid.position());
         if(vec3d == null){
            return false;
         }else if(this.toAvoid.distanceToSqr(vec3d.x, vec3d.y, vec3d.z) < this.toAvoid.distanceToSqr(this.mob)){
            return false;
         }else{
            this.path = this.pathNav.createPath(vec3d.x, vec3d.y, vec3d.z, 0);
            if(this.path != null){
               if(this.toAvoid instanceof ServerPlayer player){
                  PlayerArchetypeData profile = profile(player);
                  if(profile.hasAbility(ArchetypeRegistry.CAT_SCARE)){
                     return true;
                  }
               }
            }
            return false;
         }
      }
   }
   
   @Override
   public void start(){
      super.start();
      this.mob.playSound(SoundEvents.CREEPER_HURT, 1, 1);
      if(this.toAvoid instanceof ServerPlayer player){
         SoundUtils.playSongToPlayer(player, SoundEvents.CAT_HISS, .1f, 1);
      }
   }
}