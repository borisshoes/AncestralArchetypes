package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
            double speed = CONFIG.getDouble(ArchetypeRegistry.LAVA_WALKER_SPEED_MULTIPLIER) * player.getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
            Vec3d v = PLAYER_MOVEMENT_TRACKER.get(player).velocity();
            Vec3d horiz = v.multiply(1,0,1).normalize().multiply(speed);
            Vec3d newVel = new Vec3d(horiz.x,v.y,horiz.z);
            player.setVelocity(newVel);
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
