package net.borisshoes.ancestralarchetypes.mixins;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

@Mixin(Mob.class)
public class MobMixin {
   
   @Inject(method = "attemptToShearEquipment", at = @At("HEAD"), cancellable = true)
   private void archetypes$stopMountShear(Player player, InteractionHand hand, ItemStack heldItem, CallbackInfoReturnable<Boolean> cir){
      Entity entity = (Entity)(Object) this;
      List<String> tags = entity.entityTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(!tags.isEmpty()) cir.setReturnValue(false);
   }
}
