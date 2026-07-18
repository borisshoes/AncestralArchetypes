package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(Entity.class)
public class EntityMixin {
   
   
   @ModifyReturnValue(method = "isSteppingCarefully", at = @At("RETURN"))
   private boolean archetypes$lightweightBypass(boolean original){
      Entity entity = (Entity) (Object) this;
      if(entity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         ServerLevel world = player.level();
         
         if(profile.hasAbility(ArchetypeRegistry.LIGHTWEIGHT)){
            return true;
         }
      }
      return original;
   }
   
   @Inject(method = "removePassenger", at = @At("TAIL"))
   private void archetypes$onRemovePassenger(Entity passenger, CallbackInfo callbackInfo){
      Entity entity = (Entity) (Object) this;
      if(!entity.level().isClientSide() && entity instanceof Player)
         ((ServerPlayer) entity).connection.send(new ClientboundSetPassengersPacket(entity));
   }
   
   @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At("TAIL"))
   private void archetypes$onStartRiding(Entity entity, boolean force, boolean emit, CallbackInfoReturnable<Boolean> cir){
      if(!entity.level().isClientSide() && entity instanceof Player)
         ((ServerPlayer) entity).connection.send(new ClientboundSetPassengersPacket(entity));
   }
   
   @WrapOperation(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
   private boolean playerladder$allowRidingPlayers(EntityType<?> instance, Operation<Boolean> original, Entity entity, boolean force, boolean emit){
      if(instance == EntityTypes.PLAYER && entity instanceof ServerPlayer player && AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.RIDEABLE)){
         return true;
      }else{
         return original.call(instance);
      }
   }
}
