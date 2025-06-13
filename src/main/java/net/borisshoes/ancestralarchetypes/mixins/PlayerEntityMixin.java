package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.ArchetypeConfig;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
   
   @Shadow public abstract boolean damage(ServerWorld world, DamageSource source, float amount);
   
   @ModifyReturnValue(method = "canFoodHeal", at = @At("RETURN"))
   private boolean archetypes_canFoodHeal(boolean original){
      if(!original) return false;
      PlayerEntity player = (PlayerEntity) (Object) this;
      IArchetypeProfile profile = profile(player);
      if(profile.hasAbility(ArchetypeRegistry.NO_REGEN)) return false;
      return original;
   }
   
   @ModifyExpressionValue(method = "getBlockBreakingSpeed", at = @At(value = "CONSTANT", args = "floatValue=5.0"))
   private float archetypes_offGroundBlockBreakingSpeed(float constant){
      PlayerEntity player = (PlayerEntity) (Object) this;
      IArchetypeProfile profile = profile(player);
      boolean canBreakQuickly =  (profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER) && player.isSubmergedInWater())
            || (player.getVehicle() != null && !player.getVehicle().getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList().isEmpty());
      return canBreakQuickly ? Math.min(constant, 1) : constant;
   }
   
   @ModifyExpressionValue(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/attribute/EntityAttributeInstance;getValue()D"))
   private double archetypes_underwaterBlockBreakingSpeed(double original){
      PlayerEntity player = (PlayerEntity) (Object) this;
      IArchetypeProfile profile = profile(player);
      double newValue = original;
      if(profile.hasAbility(ArchetypeRegistry.GOOD_SWIMMER)) newValue = 0.5;
      if(profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER)) newValue = 1.5;
      return Math.max(original, newValue);
   }
   
   @ModifyReturnValue(method = "getBlockBreakingSpeed", at = @At("RETURN"))
   private float archetypes_overallBlockBreakingSpeed(float original){
      PlayerEntity player = (PlayerEntity) (Object) this;
      IArchetypeProfile profile = profile(player);
      double newValue = original;
      if(profile.hasAbility(ArchetypeRegistry.HASTY)) newValue *= ArchetypeConfig.getDouble(ArchetypeRegistry.HASTY_MINING_MODIFIER);
      return (float) newValue;
   }
   
   @Inject(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;applyArmorToDamage(Lnet/minecraft/entity/damage/DamageSource;F)F"))
   private void archetypes_thorns(ServerWorld world, DamageSource source, float amount, CallbackInfo ci){
      LivingEntity entity = (LivingEntity) (Object) this;
      if(entity instanceof ServerPlayerEntity player && source.getAttacker() instanceof LivingEntity attacker){
         IArchetypeProfile profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.THORNY) && attacker.isAlive() && !source.isIn(DamageTypeTags.AVOIDS_GUARDIAN_THORNS) && !source.isOf(DamageTypes.THORNS)){
            double cap = ArchetypeConfig.getDouble(ArchetypeRegistry.THORNY_REFLECTION_CAP);
            float damage = (float) Math.min(cap < 0 ? Float.MAX_VALUE : cap, amount * ArchetypeConfig.getDouble(ArchetypeRegistry.THORNY_REFLECTION_MODIFIER));
            attacker.damage(player.getServerWorld(), player.getDamageSources().thorns(player), damage);
         }
      }
   }
   
   @ModifyVariable(method = "attack", at = @At(value = "STORE"), ordinal = 2)
   private boolean archetypes_lanceCrit(boolean crit, @Local(ordinal = 0) boolean fullCharge){
      PlayerEntity player = (PlayerEntity) (Object) this;
      Entity vehicle = player.getVehicle();
      
      if(vehicle != null && !vehicle.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList().isEmpty()){
         return fullCharge;
      }
      
      return crit;
   }
}
