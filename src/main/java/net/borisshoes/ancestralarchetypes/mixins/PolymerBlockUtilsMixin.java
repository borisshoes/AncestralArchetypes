package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

@Mixin(PolymerBlockUtils.class)
public class PolymerBlockUtilsMixin {
   
   @ModifyReturnValue(method = "shouldMineServerSide", at = @At("RETURN"))
   private static boolean archetypes$overrideServerMining(boolean original, ServerPlayer player, BlockPos pos, BlockState state){
      if(!original){
         PlayerArchetypeData profile = profile(player);
         if(profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER) && player.isUnderWater()){
            return true;
         }
         
         if(profile.hasAbility(ArchetypeRegistry.HASTY)){
            return true;
         }
         
         Entity vehicle = player.getVehicle();
         if(vehicle != null && !vehicle.getTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList().isEmpty()){
            return true;
         }
      }
      return original;
   }
}