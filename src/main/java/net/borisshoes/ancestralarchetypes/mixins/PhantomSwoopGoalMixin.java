package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Phantom;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomSweepAttackGoal")
public abstract class PhantomSwoopGoalMixin extends Goal {
   
   @Final
   @Shadow
   Phantom field_7333; // Outer class synthetic field
   
   @Inject(method = "canContinueToUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSpectator()Z"), cancellable = true)
   private void archetypes$shouldSwoopContinue(CallbackInfoReturnable<Boolean> cir, @Local LivingEntity livingEntity){
      if(livingEntity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.CAT_SCARE)){
            SoundUtils.playSongToPlayer(player, SoundEvents.CAT_HISS, .1f, 1);
            cir.setReturnValue(false);
         }
      }
   }
}