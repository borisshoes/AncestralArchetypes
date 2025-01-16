package net.borisshoes.ancestralarchetypes.misc;

import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.utils.SoundUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class FleeFelidGoal<T extends LivingEntity> extends FleeEntityGoal<T> {
   
   public FleeFelidGoal(CreeperEntity creeper, Class<T> fleeFromType, float distance, double slowSpeed, double fastSpeed){
      super(creeper, fleeFromType, distance, slowSpeed, fastSpeed, Objects.requireNonNull(EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR)::test);
   }
   
   @Override
   public boolean canStart(){
      this.targetEntity = getServerWorld(this.mob).getClosestEntity(
            this.mob.getWorld().getEntitiesByClass(this.classToFleeFrom, this.mob.getBoundingBox().expand((double) this.fleeDistance, 3.0, (double) this.fleeDistance), (livingEntity) -> true),
            TargetPredicate.createAttackable()
                  .setBaseMaxDistance((double)this.fleeDistance)
                  .setPredicate((entity, world) -> inclusionSelector.test(entity) && extraInclusionSelector.test(entity)),
            this.mob, this.mob.getX(), this.mob.getY(), this.mob.getZ());
      if(this.targetEntity == null){
         return false;
      }else{
         Vec3d vec3d = NoPenaltyTargeting.findFrom(this.mob, 16, 7, this.targetEntity.getPos());
         if(vec3d == null){
            return false;
         }else if(this.targetEntity.squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z) < this.targetEntity.squaredDistanceTo(this.mob)){
            return false;
         }else{
            this.fleePath = this.fleeingEntityNavigation.findPathTo(vec3d.x, vec3d.y, vec3d.z, 0);
            if(this.fleePath != null){
               if(this.targetEntity instanceof PlayerEntity player){
                  IArchetypeProfile profile = profile(player);
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
      this.mob.playSound(SoundEvents.ENTITY_CREEPER_HURT, 1, 1);
      if(this.targetEntity instanceof ServerPlayerEntity player){
         SoundUtils.playSongToPlayer(player,SoundEvents.ENTITY_CAT_HISS, .1f, 1);
      }
   }
}