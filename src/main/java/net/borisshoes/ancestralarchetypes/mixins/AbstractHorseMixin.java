package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

@Mixin(AbstractHorseEntity.class)
public class AbstractHorseMixin {
   
   @Inject(method = "receiveFood", at = @At("HEAD"), cancellable = true)
   private void archetypes$stopHorseFeed(PlayerEntity player, ItemStack item, CallbackInfoReturnable<Boolean> cir){
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(!tags.isEmpty()) cir.setReturnValue(false);
   }
   
   @ModifyExpressionValue(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I"))
   private int archetypes$modifyHorseAnger(int original){
      LivingEntity entity = (LivingEntity) (Object) this;
      List<String> tags = entity.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList();
      if(!tags.isEmpty()) return entity.getRandom().nextInt(5) == 0 ? 1 : 0;
      return original;
   }
}
