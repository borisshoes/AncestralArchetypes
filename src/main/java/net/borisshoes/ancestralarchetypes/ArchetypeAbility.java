package net.borisshoes.ancestralarchetypes;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class ArchetypeAbility {
   
   private final String id;
   private final boolean active;
   private final ArchetypeAbility[] overrides;
   private final ItemStack displayStack;
   
   public ArchetypeAbility(String id, boolean activeAbility, ItemStack displayStack, ArchetypeAbility... overrides){
      this.id = id;
      this.active = activeAbility;
      this.displayStack = displayStack;
      this.overrides = overrides;
   }
   
   public MutableText getName(){
      return Text.translatable(MOD_ID+".ability.name."+id);
   }
   
   public MutableText getDescription(){
      return Text.translatable(MOD_ID+".ability.description."+id);
   }
   
   public String getId(){
      return id;
   }
   
   public ItemStack getDisplayStack(){
      return displayStack.copy();
   }
   
   public boolean isActive(){
      return active;
   }
   
   public ArchetypeAbility[] getOverrides(){
      return overrides;
   }
   
   public boolean overrides(ArchetypeAbility other){
      for(ArchetypeAbility override : overrides){
         if(other == override) return true;
      }
      return false;
   }
   
   public static class ArchetypeAbilityBuilder{
      private final String id;
      private boolean active = false;
      private ArchetypeAbility[] overrides;
      private ItemStack displayStack;
      
      public ArchetypeAbilityBuilder(String id){
         this.id = id;
         this.overrides = new ArchetypeAbility[]{};
         this.displayStack = new ItemStack(Items.BARRIER);
      }
      
      public ArchetypeAbilityBuilder setActive(){
         this.active = true;
         return this;
      }
      
      public ArchetypeAbilityBuilder setDisplayStack(ItemStack stack){
         this.displayStack = stack.copy();
         return this;
      }
      
      public ArchetypeAbilityBuilder setDescription(String[] description){
         return this;
      }
      
      public ArchetypeAbilityBuilder setOverrides(ArchetypeAbility... overrides){
         this.overrides = overrides;
         return this;
      }
      
      public ArchetypeAbility build(){
         return new ArchetypeAbility(this.id, this.active, this.displayStack, this.overrides);
      }
   }
}
