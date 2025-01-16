package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.PLAYER_MOVEMENT_TRACKER;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
   
   @Inject(method = "onTeleportationDone", at = @At("HEAD"))
   private void archetypes_resetVelTrackerTeleport(CallbackInfo ci){
      ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
      if(PLAYER_MOVEMENT_TRACKER.containsKey(player)){
         PLAYER_MOVEMENT_TRACKER.put(player, new Pair<>(player.getPos(), new Vec3d(0,0,0)));
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
