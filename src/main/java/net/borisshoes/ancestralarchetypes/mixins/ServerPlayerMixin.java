package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.tracker.PlayerMovementEntry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
   
   @Inject(method = "die", at = @At("HEAD"))
   private void archetypes$resetDeathSizeReductionLevel(DamageSource damageSource, CallbackInfo ci){
      ServerPlayer player = (ServerPlayer) (Object) this;
      
      PlayerArchetypeData profile = profile(player);
      if(profile.getDeathReductionSizeLevel() != 0){
         profile.resetDeathReductionSizeLevel(player);
      }
   }
   
   @Inject(method = "setGameMode", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;removeEntitiesOnShoulder()V"))
   private void archetypes$changeGameMode(GameType gameMode, CallbackInfoReturnable<Boolean> cir){
      ServerPlayer player = (ServerPlayer) (Object) this;
      if(player.isVehicle()) player.getFirstPassenger().stopRiding();
   }
}
