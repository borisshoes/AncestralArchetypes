package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
   @Inject(method="setStack",at=@At("HEAD"))
   private void archetype_destroyAbilityItem(ItemStack stack, CallbackInfo ci){
      ItemEntity itemEntity = (ItemEntity) (Object) this;
      if(stack.getItem() instanceof AbilityItem){
         itemEntity.discard();
      }
   }
}
