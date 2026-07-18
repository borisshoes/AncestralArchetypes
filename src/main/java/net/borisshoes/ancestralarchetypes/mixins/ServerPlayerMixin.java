package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.entities.CreakingHeartEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
   
   @Inject(method = "die", at = @At("HEAD"))
   private void archetypes$onDeath(DamageSource source, CallbackInfo ci){
      ServerPlayer player = (ServerPlayer) (Object) this;
      
      PlayerArchetypeData profile = profile(player);
      if(profile.getDeathReductionSizeLevel() != 0){
         profile.resetDeathReductionSizeLevel(player);
      }
      
      profile.metamorph(null, player);
   }
   
   @Inject(method = "setGameMode", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;removeEntitiesOnShoulder()V"))
   private void archetypes$changeGameMode(GameType mode, CallbackInfoReturnable<Boolean> cir){
      ServerPlayer player = (ServerPlayer) (Object) this;
      if(player.isVehicle()) player.getFirstPassenger().stopRiding();
   }
   
   @Inject(method = "hurtServer", at = @At("RETURN"))
   private void archetypes$creakingHurtParticles(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir){
      if(!cir.getReturnValueZ()) return;
      ServerPlayer player = (ServerPlayer) (Object) this;
      PlayerArchetypeData profile = profile(player);
      CreakingHeartEntity heart = profile.getCreakingHeart();
      if(heart != null && heart.isAlive()){
         heart.doTrailParticles(player, 20);
      }
   }
}
