package net.borisshoes.ancestralarchetypes;

import net.borisshoes.borislib.config.IConfigSetting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TooltipDisplay;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public record ArchetypeAbility(String id, boolean active, ItemStack displayStack, IConfigSetting<?>[] reliantConfigs, ArchetypeAbility... overrides) {
   
   public ArchetypeAbility(String id, boolean active, ItemStack displayStack, IConfigSetting<?>[] reliantConfigs, ArchetypeAbility... overrides){
      this.id = id;
      this.active = active;
      this.displayStack = displayStack;
      this.reliantConfigs = reliantConfigs;
      this.overrides = overrides;
      this.displayStack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.BUNDLE_CONTENTS, true));
   }
   
   public MutableComponent getName(){
      return Component.translatable(MOD_ID + ".ability.name." + id);
   }
   
   public MutableComponent getDescription(){
      return Component.translatable(MOD_ID + ".ability.description." + id);
   }
   
   @Override
   public ItemStack displayStack(){
      return displayStack.copy();
   }
   
   public boolean overrides(ArchetypeAbility other){
      for(ArchetypeAbility override : overrides){
         if(other == override) return true;
      }
      return false;
   }
   
   public static class ArchetypeAbilityBuilder {
      private final String id;
      private boolean active = false;
      private ArchetypeAbility[] overrides;
      private IConfigSetting<?>[] configs;
      private ItemStack displayStack;
      
      public ArchetypeAbilityBuilder(String id){
         this.id = id;
         this.overrides = new ArchetypeAbility[]{};
         this.configs = new IConfigSetting[]{};
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
      
      public ArchetypeAbilityBuilder setReliantConfigs(IConfigSetting<?>... configs){
         this.configs = configs;
         return this;
      }
      
      public ArchetypeAbility build(){
         return new ArchetypeAbility(this.id, this.active, this.displayStack, this.configs, this.overrides);
      }
   }
}
