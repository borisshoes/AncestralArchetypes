package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public class FarmBlockMixin {
   
   @Inject(method = "fallOn", at = @At(value = "HEAD"), cancellable = true)
   private static void archetypes$stopTrample(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci){
      if(entity instanceof ServerPlayer player && AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.LIGHTWEIGHT)){
         ci.cancel();
      }
   }
}
