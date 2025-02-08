package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(PolymerBlockUtils.class)
public class PolymerBlockUtilsMixin {
   
   @ModifyReturnValue(method = "shouldMineServerSide", at = @At("RETURN"))
   private static boolean archetypes_overrideServerMining(boolean original, ServerPlayerEntity player, BlockPos pos, BlockState state){
      if(!original){
         IArchetypeProfile profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER) && player.isSubmergedInWater()){
            return true;
         }
         
         Entity vehicle = player.getVehicle();
         if(vehicle != null && !vehicle.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList().isEmpty()){
            return true;
         }
      }
      return original;
   }
}