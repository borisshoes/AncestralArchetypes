package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(targets = "net.minecraft.entity.mob.PhantomEntity$SwoopMovementGoal")
public abstract class PhantomSwoopGoalMixin extends Goal {
   
   @Final
   @Shadow
   PhantomEntity field_7333; // Outer class synthetic field
   
   @Inject(method = "shouldContinue", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isSpectator()Z"), cancellable = true)
   private void archetypes$shouldSwoopContinue(CallbackInfoReturnable<Boolean> cir, @Local LivingEntity livingEntity){
      if(livingEntity instanceof ServerPlayerEntity player){
         IArchetypeProfile profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.CAT_SCARE)){
            SoundUtils.playSongToPlayer(player, SoundEvents.ENTITY_CAT_HISS, .1f, 1);
            cir.setReturnValue(false);
         }
      }
   }
}