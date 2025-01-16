package net.borisshoes.ancestralarchetypes.mixins;

import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(PolymerBlockUtils.class)
public class PolymerBlockUtilsMixin {
   
   @Inject(method = "shouldMineServerSide", at = @At("RETURN"), cancellable = true)
   private static void archetypes_overrideServerMining(ServerPlayerEntity player, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir){
      if(!cir.getReturnValueZ()){
         IArchetypeProfile profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER) && player.isSubmergedInWater()){
            cir.setReturnValue(true);
         }
      }
   }
}