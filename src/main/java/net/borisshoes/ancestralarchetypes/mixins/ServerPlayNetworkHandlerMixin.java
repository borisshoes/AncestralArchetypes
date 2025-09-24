package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.borislib.tracker.PlayerMovementEntry;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.borislib.BorisLib.PLAYER_MOVEMENT_TRACKER;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
   @Shadow
   public ServerPlayerEntity player;
   
   @Unique
   private Vec3d lavaDelta = null;
   
   @Unique private boolean hasInput = false;
   
   @Inject(method = "onPlayerInput", at = @At("HEAD"))
   private void onPlayerInput(PlayerInputC2SPacket pkt, CallbackInfo ci) {
      if(AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.LAVA_WALKER) && player.getFluidHeight(FluidTags.LAVA) > 0.1 && !player.isSneaking()){
         PlayerInput input = pkt.input();
         this.hasInput = input.backward() || input.forward() || input.left() || input.right();
      }
   }
   
   @ModifyExpressionValue(method = "onPlayerMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;clampHorizontal(D)D", ordinal = 0))
   private double archetypes$lavaStrideX(double original){
      if(AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.LAVA_WALKER) && player.getFluidHeight(FluidTags.LAVA) > 0.1 && !player.isSneaking() && hasInput){
         double delta = original - player.getX();
         lavaDelta = new Vec3d(delta,0,0);
         return original;
      }
      return original;
   }
   
   @ModifyExpressionValue(method = "onPlayerMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;clampHorizontal(D)D", ordinal = 1))
   private double archetypes$lavaStrideZ(double original){
      if(AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.LAVA_WALKER) && player.getFluidHeight(FluidTags.LAVA) > 0.1 && !player.isSneaking()  && hasInput){
         double delta = original - player.getZ();
         if(lavaDelta != null){
            lavaDelta = new Vec3d(lavaDelta.getX(),0,delta);
         }
         return original;
      }
      return original;
   }
   
   @Inject(method = "onPlayerMove", at = @At(value = "INVOKE",target = "Lnet/minecraft/server/network/ServerPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"))
   private void archetypes$strideAndClimb(PlayerMoveC2SPacket packet, CallbackInfo ci){
      IArchetypeProfile profile = AncestralArchetypes.profile(player);
      if(profile.hasAbility(ArchetypeRegistry.LAVA_WALKER) && player.getFluidHeight(FluidTags.LAVA) > 0.1 && !player.isSneaking()){
         if(lavaDelta != null && hasInput){
            double multiplier = CONFIG.getDouble(ArchetypeRegistry.LAVA_WALKER_SPEED_MULTIPLIER)+1.2;
            Vec3d v = PLAYER_MOVEMENT_TRACKER.get(player).velocity();
            Vec3d desired = lavaDelta.multiply(multiplier,1,multiplier);
            double maxH = profile.isFungusBoosted() ? 1.5 : 1.0;
            double maxY = 0.35;
            double accelMove = profile.isFungusBoosted() ? 0.5 : 0.30;
            double dragMove = 0.25;
            double stopSpeed = 0.03;
            
            double hLen = Math.sqrt(desired.x * desired.x + desired.z * desired.z);
            if (hLen > maxH) desired = new Vec3d(desired.x * (maxH / hLen), desired.y, desired.z * (maxH / hLen));
            if (desired.y > maxY) desired = new Vec3d(desired.x, maxY, desired.z);
            if (desired.y < -maxY) desired = new Vec3d(desired.x, -maxY, desired.z);
            
            Vec3d blended = v.add(desired.subtract(v).multiply(accelMove));
            Vec3d damped = blended.multiply(1.0 - dragMove);

            double newH = Math.sqrt(damped.x * damped.x + damped.z * damped.z);
            if (newH > maxH) damped = new Vec3d(damped.x * (maxH / newH), damped.y, damped.z * (maxH / newH));
            if (damped.y > maxY) damped = new Vec3d(damped.x, maxY, damped.z);
            if (damped.y < -maxY) damped = new Vec3d(damped.x, -maxY, damped.z);

            if (damped.length() < stopSpeed) damped = Vec3d.ZERO;

            player.setVelocity(damped);
            player.fallDistance = 0.0F;
            player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
            lavaDelta = null;
         }
      }
      
      if(packet.horizontalCollision() && !player.getAbilities().flying && player.isSneaking() && profile.hasAbility(ArchetypeRegistry.CLIMBING)){
         player.setVelocity(new Vec3d(player.getVelocity().getX(),0.2,player.getVelocity().getZ()));
         player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
         player.networkHandler.floatingTicks = 0;
      }
   }
}
