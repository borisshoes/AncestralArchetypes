package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.spawner.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {
   
   @ModifyExpressionValue(method="spawn",at=@At(value="INVOKE",target="Lnet/minecraft/server/network/ServerPlayerEntity;isSpectator()Z"))
   private boolean archetypes_fuckPhantoms(boolean original, @Local ServerPlayerEntity player){
      if(original) return true;
      IArchetypeProfile profile = profile(player);
      return profile.hasAbility(ArchetypeRegistry.CAT_SCARE);
   }
}