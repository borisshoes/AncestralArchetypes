package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomSweepAttackGoal")
public abstract class PhantomSwoopGoalMixin extends Goal {
   
   @Inject(method = "canContinueToUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSpectator()Z"), cancellable = true)
   private void archetypes$shouldSwoopContinue(CallbackInfoReturnable<Boolean> cir, @Local LivingEntity livingEntity){
      if(livingEntity instanceof ServerPlayer player){
         PlayerArchetypeData profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.CAT_SCARE)){
            // TODO wtf sound registry?
            SoundUtils.playSongToPlayer(player, BuiltInRegistries.SOUND_EVENT.get(Identifier.withDefaultNamespace("entity.cat_royal.hiss")).get(), .1f, 1);
            cir.setReturnValue(false);
         }
      }
   }
}