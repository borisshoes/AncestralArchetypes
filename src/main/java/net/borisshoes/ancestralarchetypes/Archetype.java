package net.borisshoes.ancestralarchetypes;

import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class Archetype {
   
   private final String id;
   private final ArchetypeAbility[] abilities;
   private final ItemStack displayItem;
   private final int color;
   
   public Archetype(String id, ItemStack displayItem, int color, ArchetypeAbility... abilities){
      this.id = id;
      this.color = color;
      this.abilities = abilities;
      this.displayItem = displayItem;
   }
   
   public MutableText getName(){
      return Text.translatable(MOD_ID+".archetype.name."+id);
   }
   
   public MutableText getDescription(){
      return Text.translatable(MOD_ID+".archetype.description."+id);
   }
   
   public String getId(){
      return id;
   }
   
   public ItemStack getDisplayItem(){
      return displayItem;
   }
   
   public int getColor(){
      return color;
   }
   
   public ArchetypeAbility[] getAbilities(){
      return abilities;
   }
}
