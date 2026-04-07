package net.borisshoes.ancestralarchetypes;

import net.borisshoes.borislib.config.IConfigSetting;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TooltipDisplay;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public record ArchetypeAbility(String id, boolean active, ItemStackTemplate displayStack, IConfigSetting<?>[] reliantConfigs,
                               ArchetypeAbility... overrides) {
   
   public ArchetypeAbility(String id, boolean active, ItemStackTemplate displayStack, IConfigSetting<?>[] reliantConfigs, ArchetypeAbility... overrides){
      this.id = id;
      this.active = active;
      this.displayStack = displayStack;
      this.reliantConfigs = reliantConfigs;
      this.overrides = overrides;
   }
   
   public MutableComponent getName(){
      return Component.translatable(MOD_ID + ".ability.name." + id);
   }
   
   public MutableComponent getDescription(){
      return Component.translatable(MOD_ID + ".ability.description." + id);
   }
   
   public ItemStack getDisplayItemStack(){
      ItemStack stack = displayStack.create();
      stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.BUNDLE_CONTENTS, true));
      return stack;
   }
   
   public boolean overrides(ArchetypeAbility other){
      for(ArchetypeAbility override : overrides){
         if(other == override) return true;
      }
      return false;
   }
   
   @Override
   public boolean equals(Object o){
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;
      ArchetypeAbility that = (ArchetypeAbility) o;
      return id.equals(that.id);
   }
   
   @Override
   public int hashCode(){
      return id.hashCode();
   }
   
   public static class ArchetypeAbilityBuilder {
      private final String id;
      private boolean active = false;
      private ArchetypeAbility[] overrides;
      private IConfigSetting<?>[] configs;
      private ItemStackTemplate displayStack;
      
      public ArchetypeAbilityBuilder(String id){
         this.id = id;
         this.overrides = new ArchetypeAbility[]{};
         this.configs = new IConfigSetting[]{};
         this.displayStack = new ItemStackTemplate(Items.BARRIER);
      }
      
      public ArchetypeAbilityBuilder setActive(){
         this.active = true;
         return this;
      }
      
      public ArchetypeAbilityBuilder setDisplayStack(ItemStackTemplate stack){
         this.displayStack = stack;
         return this;
      }
      
      public ArchetypeAbilityBuilder setDisplayStack(ItemStack stack){
         this.displayStack = ItemStackTemplate.fromNonEmptyStack(stack);
         return this;
      }
      
      public ArchetypeAbilityBuilder setDescription(String[] description){
         return this;
      }
      
      public ArchetypeAbilityBuilder setOverrides(ArchetypeAbility... overrides){
         this.overrides = overrides;
         return this;
      }
      
      public ArchetypeAbilityBuilder setReliantConfigs(IConfigSetting<?>... configs){
         this.configs = configs;
         return this;
      }
      
      public ArchetypeAbility build(){
         return new ArchetypeAbility(this.id, this.active, this.displayStack, this.configs, this.overrides);
      }
   }
}
