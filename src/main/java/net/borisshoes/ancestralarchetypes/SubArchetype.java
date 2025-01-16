package net.borisshoes.ancestralarchetypes;

import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class SubArchetype {
   private final String id;
   private final Archetype parent;
   private final ArchetypeAbility[] abilities;
   private final ItemStack displayItem;
   private final int color;
   
   public SubArchetype(String id, ItemStack displayItem, int color, Archetype parent, ArchetypeAbility... abilities){
      this.id = id;
      this.parent = parent;
      this.color = color;
      this.abilities = abilities;
      this.displayItem = displayItem;
   }
   
   public MutableText getName(){
      return Text.translatable(MOD_ID+".subarchetype.name."+id);
   }
   
   public MutableText getDescription(){
      return Text.translatable(MOD_ID+".subarchetype.description."+id,this.parent == null ? Text.translatable("text.ancestralarchetypes.archetype") : parent.getName());
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
   
   public Archetype getArchetype(){
      return parent;
   }
   
   public ArchetypeAbility[] getAbilities(){
      return abilities;
   }
}
