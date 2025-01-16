package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
   
   @Inject(method = "canFoodHeal", at = @At("RETURN"), cancellable = true)
   private void archetypes_canFoodHeal(CallbackInfoReturnable<Boolean> cir){
      if(!cir.getReturnValue()) return;
      PlayerEntity player = (PlayerEntity) (Object) this;
      IArchetypeProfile profile = profile(player);
      if(profile.hasAbility(ArchetypeRegistry.NO_REGEN)) cir.setReturnValue(false);
   }
   
   @ModifyExpressionValue(method = "getBlockBreakingSpeed", at = @At(value = "CONSTANT", args = "floatValue=5.0"))
   private float archetypes_offGroundBlockBreakingSpeed(float constant){
      PlayerEntity player = (PlayerEntity) (Object) this;
      IArchetypeProfile profile = profile(player);
      return profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER) && player.isSubmergedInWater() ? Math.min(constant, 1) : constant;
   }
   
   @ModifyExpressionValue(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/attribute/EntityAttributeInstance;getValue()D"))
   private double archetypes_underwaterBlockBreakingSpeed(double original){
      PlayerEntity player = (PlayerEntity) (Object) this;
      IArchetypeProfile profile = profile(player);
      return profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER) && player.isSubmergedInWater() ? Math.max(original, 1) : original;
   }
}
