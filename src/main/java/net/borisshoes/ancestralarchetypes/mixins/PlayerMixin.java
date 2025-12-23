package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;

@Mixin(Player.class)
public abstract class PlayerMixin {
   
   @Shadow public abstract boolean hurtServer(ServerLevel world, DamageSource source, float amount);
   
   @ModifyReturnValue(method = "isHurt", at = @At("RETURN"))
   private boolean archetypes$canFoodHeal(boolean original){
      if(!original) return false;
      Player player = (Player) (Object) this;
      PlayerArchetypeData profile = profile(player);
      if(profile.hasAbility(ArchetypeRegistry.NO_REGEN)) return false;
      return original;
   }
   
   @ModifyExpressionValue(method = "getDestroySpeed", at = @At(value = "CONSTANT", args = "floatValue=5.0"))
   private float archetypes$offGroundBlockBreakingSpeed(float constant){
      Player player = (Player) (Object) this;
      PlayerArchetypeData profile = profile(player);
      boolean canBreakQuickly =  (profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER) && player.isUnderWater())
            || (player.getVehicle() != null && !player.getVehicle().getTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList().isEmpty());
      return canBreakQuickly ? Math.min(constant, 1) : constant;
   }
   
   @ModifyExpressionValue(method = "getDestroySpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getValue()D"))
   private double archetypes$underwaterBlockBreakingSpeed(double original){
      Player player = (Player) (Object) this;
      PlayerArchetypeData profile = profile(player);
      double newValue = original;
      if(profile.hasAbility(ArchetypeRegistry.GOOD_SWIMMER)) newValue = 0.5;
      if(profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER)) newValue = 1.5;
      return Math.max(original, newValue);
   }
   
   @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
   private float archetypes$overallBlockBreakingSpeed(float original){
      Player player = (Player) (Object) this;
      PlayerArchetypeData profile = profile(player);
      double newValue = original;
      if(profile.hasAbility(ArchetypeRegistry.HASTY)) newValue *= CONFIG.getDouble(ArchetypeRegistry.HASTY_MINING_MODIFIER);
      return (float) newValue;
   }
   
   @Inject(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
   private void archetypes$thorns(ServerLevel world, DamageSource source, float amount, CallbackInfo ci){
      LivingEntity entity = (LivingEntity) (Object) this;
      if(entity instanceof ServerPlayer player && source.getEntity() instanceof LivingEntity attacker){
         PlayerArchetypeData profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.THORNY) && attacker.isAlive() && !source.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS) && !source.is(DamageTypes.THORNS)){
            double cap = CONFIG.getDouble(ArchetypeRegistry.THORNY_REFLECTION_CAP);
            float damage = (float) Math.min(cap < 0 ? Float.MAX_VALUE : cap, amount * CONFIG.getDouble(ArchetypeRegistry.THORNY_REFLECTION_MODIFIER));
            attacker.hurtServer(player.level(), player.damageSources().thorns(player), damage);
         }
      }
   }
   
   @ModifyVariable(method = "attack", at = @At(value = "STORE"), ordinal = 2)
   private boolean archetypes$lanceCrit(boolean crit, @Local(ordinal = 0) boolean fullCharge){
      Player player = (Player) (Object) this;
      Entity vehicle = player.getVehicle();
      
      if(vehicle != null && !vehicle.getTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList().isEmpty()){
         return fullCharge;
      }
      
      return crit;
   }
   
   @Inject(method = "tick", at = @At("TAIL"))
   private void archetypes$onTick(CallbackInfo callbackInfo){
      Player entity = (Player) (Object) this;
      if(!entity.level().isClientSide() && entity.isVehicle() && entity.isShiftKeyDown() && entity.onGround()) entity.getFirstPassenger().stopRiding();
   }
   
   @ModifyReturnValue(method = "onClimbable", at = @At("RETURN"))
   private boolean archetypes$isClimbing(boolean original){
      Player player = (Player) (Object) this;
      if(original) return true;
      if(player.horizontalCollision && AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.CLIMBING)){
         return true;
      }
      return original;
   }
}
