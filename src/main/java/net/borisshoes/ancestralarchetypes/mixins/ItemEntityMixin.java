package net.borisshoes.ancestralarchetypes.mixins;

import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.ancestralarchetypes.items.MetamorphHeadItem;
import net.borisshoes.ancestralarchetypes.misc.ArchetypeUtils;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {
   @Inject(method= "setItem",at=@At("HEAD"))
   private void archetype_destroyAbilityItem(ItemStack stack, CallbackInfo ci){
      ItemEntity itemEntity = (ItemEntity) (Object) this;
      if(stack.getItem() instanceof AbilityItem){
         itemEntity.discard();
         return;
      }else if(stack.getItem() instanceof MetamorphHeadItem){
         itemEntity.discard();
         return;
      }
      
      ArchetypeUtils.removeMetamorphHelmetTags(stack);
   }
}
