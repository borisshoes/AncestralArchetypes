package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;
import static net.borisshoes.borislib.BorisLib.PLAYER_MOVEMENT_TRACKER;

@Mixin(Entity.class)
public class EntityMixin {
   
   @ModifyReturnValue(method = "isSteppingCarefully", at = @At("RETURN"))
   private boolean archetypes$lightweightBypass(boolean original){
      Entity entity = (Entity)(Object) this;
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         ServerLevel world = player.level();
         
         if(profile.hasAbility(ArchetypeRegistry.LIGHTWEIGHT)){
            return true;
         }
      }
      return original;
   }
   
   @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateEntityMovementAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"))
   private void archetypes$onEntityLand(MoverType type, Vec3 movement, CallbackInfo ci, @Local Block block, @Local BlockState state){
      Entity entity = (Entity)(Object) this;
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         ServerLevel world = player.level();
         
         if(profile.hasAbility(ArchetypeRegistry.BOUNCY) && !player.isDeadOrDying()){
            if (!player.isSuppressingBounce()) {
               Vec3 oldVel = PLAYER_MOVEMENT_TRACKER.get(player).velocity();
               if (oldVel.y < 0.0) {
                  double newY = oldVel.y > -0.425 ? 0 : -0.9*oldVel.y;
                  Vec3 newVel = new Vec3(oldVel.x, newY, oldVel.z);
                  player.fallDistance = 0;
                  player.setDeltaMovement(newVel);
                  player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), newVel));
               }
            }
         }
      }
   }
   
   @Inject(method = "removePassenger", at = @At("TAIL"))
   private void archetypes$onRemovePassenger(Entity passenger, CallbackInfo callbackInfo){
      Entity entity = (Entity) (Object) this;
      if(!entity.level().isClientSide() && entity instanceof Player)
         ((ServerPlayer) entity).connection.send(new ClientboundSetPassengersPacket(entity));
   }
   
   @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At("TAIL"))
   private void archetypes$onStartRiding(Entity entity, boolean force, boolean emit, CallbackInfoReturnable<Boolean> cir){
      if(!entity.level().isClientSide() && entity instanceof Player)
         ((ServerPlayer)entity).connection.send(new ClientboundSetPassengersPacket(entity));
   }
   
   @WrapOperation(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
   private boolean playerladder$allowRidingPlayers(EntityType<?> instance, Operation<Boolean> original, Entity entity, boolean force, boolean emit) {
      if(instance == EntityType.PLAYER && entity instanceof ServerPlayer player && AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.RIDEABLE)) {
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
