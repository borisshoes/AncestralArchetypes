package net.borisshoes.ancestralarchetypes.misc;

import net.borisshoes.arcananovum.items.ShieldOfFortitude;
import net.borisshoes.arcananovum.utils.ArcanaItemUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public class ArcanaCompat {
   
   public static void triggerShieldOfFortitude(LivingEntity entity, float damage){
      // Activate Shield of Fortitude
      ItemStack activeItem = entity.getActiveItem();
      if(ArcanaItemUtils.identifyItem(activeItem) instanceof ShieldOfFortitude shield){
         shield.shieldBlock(entity, activeItem, damage);
      }
   }
}
