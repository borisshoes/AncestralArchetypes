package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.borislib.tracker.PlayerMovementEntry;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.borislib.BorisLib.PLAYER_MOVEMENT_TRACKER;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
   @Shadow
   public ServerPlayerEntity player;
   
   @Inject(method = "onPlayerMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;setMovement(ZZLnet/minecraft/util/math/Vec3d;)V"))
   private void archetypes_updateVelocityTracker(PlayerMoveC2SPacket packet, CallbackInfo ci, @Local Vec3d velocity){
      if(PLAYER_MOVEMENT_TRACKER.containsKey(player) && !player.isDead()){
         PlayerMovementEntry oldEntry = PLAYER_MOVEMENT_TRACKER.get(player);
         long now = System.nanoTime();
         long then = oldEntry.timestamp();
         long diff = now-then;
         //float tickDiff = ((float) diff / (float) player.getServer().getAverageNanosPerTick());
         float tickDiff = 1;
         Vec3d trueVel = velocity.multiply(1/tickDiff);
//         System.out.println("Tick Diff: "+tickDiff);
//         System.out.println(velocity);
//         System.out.println(trueVel);
         PlayerMovementEntry newEntry = new PlayerMovementEntry(player, player.getPos(), trueVel, System.nanoTime());
         PLAYER_MOVEMENT_TRACKER.put(player,newEntry);
      }else{
         PLAYER_MOVEMENT_TRACKER.put(player,PlayerMovementEntry.blankEntry(player));
      }
   }
}
