package net.borisshoes.ancestralarchetypes;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.*;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class SubArchetype {
   private final String id;
   private final Archetype parent;
   private final Set<ArchetypeAbility> abilities = new HashSet<>();
   private final Set<ArchetypeAbility> defaultAbilities = new HashSet<>();
   private final ItemStack displayItem;
   private final int color;
   private final EntityType<?> entityType;
   
   public SubArchetype(String id, EntityType<?> entityType, ItemStack displayItem, int color, Archetype parent, ArchetypeAbility... abilities){
      this.id = id;
      this.entityType = entityType;
      this.parent = parent;
      this.color = color;
      this.abilities.addAll(List.of(abilities));
      this.defaultAbilities.addAll(List.of(abilities));
      this.displayItem = displayItem;
   }
   
   public MutableComponent getName(){
      return Component.translatable(MOD_ID+".subarchetype.name."+id);
   }
   
   public MutableComponent getDescription(){
      return Component.translatable(MOD_ID+".subarchetype.description."+id,this.parent == null ? Component.translatable("text.ancestralarchetypes.archetype") : parent.getName());
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
   
   public Set<ArchetypeAbility> getRawAbilities(){
      return abilities;
   }
   
   public Set<ArchetypeAbility> getDefaultAbilities(){
      return defaultAbilities;
   }
   
   public void setAbilities(Set<ArchetypeAbility> archetypeAbilities){
      this.abilities.clear();
      this.abilities.addAll(archetypeAbilities);
   }
   
   public void resetAbilities(){
      this.abilities.clear();
      this.abilities.addAll(this.defaultAbilities);
   }
   
   public boolean addAbility(ArchetypeAbility ability){
      return this.abilities.add(ability);
   }
   
   public boolean removeAbility(ArchetypeAbility ability){
      return this.abilities.remove(ability);
   }
   
   public EntityType<?> getEntityType(){
      return entityType;
   }
   
   public Set<ArchetypeAbility> getActualAbilities(){
      HashSet<ArchetypeAbility> actualAbilities = new HashSet<>();
      for(ArchetypeAbility ability : abilities){
         if(actualAbilities.stream().anyMatch(current -> current.overrides(ability))){
            continue;
         }
         actualAbilities.removeIf(ability::overrides);
         actualAbilities.add(ability);
      }
      return actualAbilities;
   }
   
   @Override
   public boolean equals(Object o){
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;
      SubArchetype that = (SubArchetype) o;
      return id.equals(that.id);
   }
   
   @Override
   public int hashCode(){
      return id.hashCode();
   }
}
