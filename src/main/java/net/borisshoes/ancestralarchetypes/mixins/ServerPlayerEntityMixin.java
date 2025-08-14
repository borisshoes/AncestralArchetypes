package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.utils.PlayerMovementEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.PLAYER_MOVEMENT_TRACKER;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
   
   @Inject(method = "teleportTo(Lnet/minecraft/world/TeleportTarget;)Lnet/minecraft/server/network/ServerPlayerEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;onDimensionChanged(Lnet/minecraft/entity/Entity;)V"))
   private void archetypesResetVelTrackerTeleport2(TeleportTarget teleportTarget, CallbackInfoReturnable<ServerPlayerEntity> cir){
      ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
      if(PLAYER_MOVEMENT_TRACKER.containsKey(player)){
         PLAYER_MOVEMENT_TRACKER.put(player, PlayerMovementEntry.blankEntry(player));
      }
   }
   
   @Inject(method = "onTeleportationDone", at = @At("HEAD"))
   private void archetypes_resetVelTrackerTeleport(CallbackInfo ci){
      ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
      if(PLAYER_MOVEMENT_TRACKER.containsKey(player)){
         PLAYER_MOVEMENT_TRACKER.put(player, PlayerMovementEntry.blankEntry(player));
      }
   }
   
   @Inject(method = "onDeath", at = @At("HEAD"))
   private void archetypes_resetDeathSizeReductionLevel(DamageSource damageSource, CallbackInfo ci){
      ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
      
      IArchetypeProfile profile = profile(player);
      if(profile.getDeathReductionSizeLevel() != 0){
         profile.resetDeathReductionSizeLevel();
      }
   }
}
