package net.borisshoes.ancestralarchetypes.cca;

import net.borisshoes.ancestralarchetypes.Archetype;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.SubArchetype;
import net.minecraft.core.Holder;
import net.minecraft.util.Tuple;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import org.ladysnake.cca.api.v3.component.ComponentV3;

import java.util.List;
import java.util.UUID;

public interface IArchetypeProfile extends ComponentV3 {
   
   boolean hasData();
   void clearData();
   
   boolean hasAbility(ArchetypeAbility ability);
   int getDeathReductionSizeLevel();
   SubArchetype getSubArchetype();
   Archetype getArchetype();
   List<ArchetypeAbility> getAbilities();
   int getAbilityCooldown(ArchetypeAbility ability);
   ItemStack getPotionStack();
   float getGlideTime();
   float getHoverTime();
   float getFortifyTime();
   boolean isFortifyActive();
   UUID getMountEntity(ArchetypeAbility ability);
   float getMountHealth(ArchetypeAbility ability);
   SimpleContainer getMountInventory();
   SimpleContainer getBackpackInventory();
   Markings getHorseMarking();
   Variant getHorseColor();
   String getMountName();
   int getGliderColor();
   int getHelmetColor();
   Holder<TrimMaterial> getGliderTrimMaterial();
   Holder<TrimMaterial> getHelmetTrimMaterial();
   boolean canChangeArchetype();
   boolean giveReminders();
   float getHealthUpdate();
}
