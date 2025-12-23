package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {
   
   @ModifyExpressionValue(method= "tick",at=@At(value="INVOKE",target= "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
   private boolean archetypes$fuckPhantoms(boolean original, @Local ServerPlayer player){
      if(original) return true;
      PlayerArchetypeData profile = profile(player);
      return profile.hasAbility(ArchetypeRegistry.CAT_SCARE);
   }
}