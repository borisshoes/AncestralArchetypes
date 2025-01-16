package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.misc.FleeFelidGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public abstract class CreeperMixin extends HostileEntity {
   
   protected CreeperMixin(EntityType<? extends HostileEntity> entityType, World world){
      super(entityType, world);
   }
   
   @Inject(method = "initGoals", at = @At("HEAD"))
   protected void initGoals(CallbackInfo ci){
      CreeperEntity creeper = (CreeperEntity) (Object) this;
      this.goalSelector.add(1, new FleeFelidGoal<>(creeper, PlayerEntity.class, 8.0F, 1.0, 1.2));
   }
}
