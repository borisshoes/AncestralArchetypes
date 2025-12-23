package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.borislib.BorisLib.PLAYER_MOVEMENT_TRACKER;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
   @Shadow
   public ServerPlayer player;
   
   @Unique
   private Vec3 lavaDelta = null;
   
   @Unique private boolean hasInput = false;
   
   @Inject(method = "handlePlayerInput", at = @At("HEAD"))
   private void onPlayerInput(ServerboundPlayerInputPacket pkt, CallbackInfo ci) {
      if(AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.LAVA_WALKER) && player.getFluidHeight(FluidTags.LAVA) > 0.1 && !player.isShiftKeyDown()){
         Input input = pkt.input();
         this.hasInput = input.backward() || input.forward() || input.left() || input.right();
      }
   }
   
   @ModifyExpressionValue(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D", ordinal = 0))
   private double archetypes$lavaStrideX(double original){
      if(AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.LAVA_WALKER) && player.getFluidHeight(FluidTags.LAVA) > 0.1 && !player.isShiftKeyDown() && hasInput){
         double delta = original - player.getX();
         lavaDelta = new Vec3(delta,0,0);
         return original;
      }
      return original;
   }
   
   @ModifyExpressionValue(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D", ordinal = 1))
   private double archetypes$lavaStrideZ(double original){
      if(AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.LAVA_WALKER) && player.getFluidHeight(FluidTags.LAVA) > 0.1 && !player.isShiftKeyDown()  && hasInput){
         double delta = original - player.getZ();
         if(lavaDelta != null){
            lavaDelta = new Vec3(lavaDelta.x(),0,delta);
         }
         return original;
      }
      return original;
   }
   
   @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE",target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
   private void archetypes$strideAndClimb(ServerboundMovePlayerPacket packet, CallbackInfo ci){
      PlayerArchetypeData profile = AncestralArchetypes.profile(player);
      if(profile.hasAbility(ArchetypeRegistry.LAVA_WALKER) && player.getFluidHeight(FluidTags.LAVA) > 0.1 && !player.isShiftKeyDown()){
         if(lavaDelta != null && hasInput){
            double speed = CONFIG.getDouble(ArchetypeRegistry.LAVA_WALKER_SPEED_MULTIPLIER) * player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            Vec3 v = PLAYER_MOVEMENT_TRACKER.get(player).velocity();
            Vec3 horiz = v.multiply(1,0,1).normalize().scale(speed);
            Vec3 newVel = new Vec3(horiz.x,v.y,horiz.z);
            player.setDeltaMovement(newVel);
            player.fallDistance = 0.0F;
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            lavaDelta = null;
         }
      }
      
      if(packet.horizontalCollision() && !player.getAbilities().flying && player.isShiftKeyDown() && profile.hasAbility(ArchetypeRegistry.CLIMBING)){
         player.setDeltaMovement(new Vec3(player.getDeltaMovement().x(),0.2,player.getDeltaMovement().z()));
         player.connection.send(new ClientboundSetEntityMotionPacket(player));
         player.connection.aboveGroundTickCount = 0;
      }
   }
}
