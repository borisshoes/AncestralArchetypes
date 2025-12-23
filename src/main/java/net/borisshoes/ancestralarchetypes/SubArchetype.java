package net.borisshoes.ancestralarchetypes;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class SubArchetype {
   private final String id;
   private final Archetype parent;
   private final ArchetypeAbility[] abilities;
   private final ItemStack displayItem;
   private final int color;
   private final EntityType<?> entityType;
   
   public SubArchetype(String id, EntityType<?> entityType, ItemStack displayItem, int color, Archetype parent, ArchetypeAbility... abilities){
      this.id = id;
      this.entityType = entityType;
      this.parent = parent;
      this.color = color;
      this.abilities = abilities;
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
   
   public ArchetypeAbility[] getAbilities(){
      return abilities;
   }
   
   public EntityType<?> getEntityType(){
      return entityType;
   }
   
   public ArrayList<ArchetypeAbility> getActualAbilities(){
      ArrayList<ArchetypeAbility> list = new ArrayList<>();
      list.addAll(Arrays.asList(getAbilities()));
      list.addAll(Arrays.asList(getArchetype().getAbilities()));
      list.removeIf(a1 -> list.stream().anyMatch(a2 -> a2.overrides(a1)));
      return list;
   }
}
