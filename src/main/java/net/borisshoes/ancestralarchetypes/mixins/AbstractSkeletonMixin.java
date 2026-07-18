package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.misc.FleeWolfGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin extends Monster {
   
   protected AbstractSkeletonMixin(EntityType<? extends Monster> entityType, Level world){
      super(entityType, world);
   }
   
   @Inject(method = "registerGoals", at = @At("HEAD"))
   protected void initGoals(CallbackInfo ci){
      AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
      this.goalSelector.addGoal(1, new FleeWolfGoal<>(skeleton, Player.class, 8.0F, 1.0, 1.2));
   }
}