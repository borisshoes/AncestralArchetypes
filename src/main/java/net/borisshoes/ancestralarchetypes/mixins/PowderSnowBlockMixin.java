package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {
   
   @ModifyReturnValue(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"))
   private static boolean archetypes$walkOnSnow(boolean original, Entity entity){
//      if(entity instanceof ServerPlayerEntity player){
//         if(AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.LIGHTWEIGHT)){
//            return true;
//         }
//      }
      return original;
   }
}
