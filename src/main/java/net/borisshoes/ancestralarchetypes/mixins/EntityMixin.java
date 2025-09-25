package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.borislib.BorisLib.PLAYER_MOVEMENT_TRACKER;

@Mixin(Entity.class)
public class EntityMixin {
   
   @ModifyReturnValue(method = "bypassesSteppingEffects", at = @At("RETURN"))
   private boolean archetypes$lightweightBypass(boolean original){
      Entity entity = (Entity)(Object) this;
      if(entity instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         ServerWorld world = player.getWorld();
         
         if(profile.hasAbility(ArchetypeRegistry.LIGHTWEIGHT)){
            return true;
         }
      }
      return original;
   }
   
   @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onEntityLand(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;)V"))
   private void archetypes$onEntityLand(MovementType type, Vec3d movement, CallbackInfo ci, @Local Block block, @Local BlockState state){
      Entity entity = (Entity)(Object) this;
      if(entity instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         ServerWorld world = player.getWorld();
         
         if(profile.hasAbility(ArchetypeRegistry.BOUNCY) && !player.isDead()){
            if (!player.bypassesLandingEffects()) {
               Vec3d oldVel = PLAYER_MOVEMENT_TRACKER.get(player).velocity();
               if (oldVel.y < 0.0) {
                  double newY = oldVel.y > -0.425 ? 0 : -0.9*oldVel.y;
                  Vec3d newVel = new Vec3d(oldVel.x, newY, oldVel.z);
                  player.fallDistance = 0;
                  player.setVelocity(newVel);
                  player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player.getId(), newVel));
               }
            }
         }
      }
   }
   
   @Inject(method = "removePassenger", at = @At("TAIL"))
   private void archetypes$onRemovePassenger(Entity passenger, CallbackInfo callbackInfo){
      Entity entity = (Entity) (Object) this;
      if(!entity.getWorld().isClient && entity instanceof PlayerEntity)
         ((ServerPlayerEntity) entity).networkHandler.sendPacket(new EntityPassengersSetS2CPacket(entity));
   }
   
   @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", at = @At("TAIL"))
   private void archetypes$onStartRiding(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir){
      if(!entity.getWorld().isClient && entity instanceof PlayerEntity)
         ((ServerPlayerEntity)entity).networkHandler.sendPacket(new EntityPassengersSetS2CPacket(entity));
   }
   
   @WrapOperation(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;isSaveable()Z"))
   private boolean playerladder$allowRidingPlayers(EntityType<?> instance, Operation<Boolean> original, Entity entity, boolean force) {
      if(instance == EntityType.PLAYER && entity instanceof ServerPlayerEntity player && AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.RIDEABLE)) {
         return true;
      }else{
         return original.call(instance);
      }
   }
   
//   @ModifyVariable(method = "updatePassengerPosition(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity$PositionUpdater;)V", at = @At(value = "STORE"), ordinal = 0, require = 0)
//   private double archetypes$offsetPassengersClientSide(double d, Entity passenger) {
//      Entity entity = (Entity) (Object) this;
//      return entity.getWorld().isClient && passenger instanceof PlayerEntity ? d-getRidingOffset(passenger) : d;
//   }
}
