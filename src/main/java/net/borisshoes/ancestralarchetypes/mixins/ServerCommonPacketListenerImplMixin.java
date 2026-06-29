package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.misc.EcholocationVibrationSystem;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {
   
   @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
   private void archetypes$echolocationSound(Packet<?> packet, CallbackInfo ci){
      ServerCommonPacketListenerImpl listenerImpl = (ServerCommonPacketListenerImpl) (Object) this;
      if(!(listenerImpl instanceof ServerGamePacketListenerImpl listener)) return;
      ServerPlayer player = listener.player;
      if(player == null || !EcholocationVibrationSystem.isActive(player.getUUID())) return;
      
      if(packet instanceof ClientboundSoundPacket sound){
         if(archetypes$isIgnored(sound.getSource())) return;
         double x = sound.getX();
         double y = sound.getY();
         double z = sound.getZ();
         archetypes$onServerThread(player, () -> EcholocationVibrationSystem.handleSoundAt(player, x, y, z));
      }else if(packet instanceof ClientboundSoundEntityPacket sound){
         if(archetypes$isIgnored(sound.getSource())) return;
         int id = sound.getId();
         archetypes$onServerThread(player, () -> EcholocationVibrationSystem.handleEntitySound(player, id));
      }
   }
   
   @Unique
   private static boolean archetypes$isIgnored(SoundSource source){
      return source == SoundSource.MUSIC || source == SoundSource.MASTER;
   }
   
   @Unique
   private static void archetypes$onServerThread(ServerPlayer player, Runnable task){
      MinecraftServer server = player.level().getServer();
      if(server.isSameThread()){
         task.run();
      }else{
         server.execute(task);
      }
   }
}


