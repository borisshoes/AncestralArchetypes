package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.misc.EcholocationVibrationSystem;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
   
   @Inject(method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V", at = @At("HEAD"))
   private void archetypes$echolocationDispatch(Holder<GameEvent> event, Vec3 pos, GameEvent.Context context, CallbackInfo ci){
      EcholocationVibrationSystem.dispatchGameEvent((ServerLevel)(Object) this, event, pos, context);
   }
}

