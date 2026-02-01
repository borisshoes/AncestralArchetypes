package net.borisshoes.ancestralarchetypes;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public record Archetype(String id, ItemStack displayItem, int color) {
   
   public MutableComponent getName(){
      return Component.translatable(MOD_ID + ".archetype.name." + id);
   }
   
   public MutableComponent getDescription(){
      return Component.translatable(MOD_ID + ".archetype.description." + id);
   }
   
   @Override
   public boolean equals(Object o){
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;
      Archetype that = (Archetype) o;
      return id.equals(that.id);
   }
   
   @Override
   public int hashCode(){
      return id.hashCode();
   }
}
