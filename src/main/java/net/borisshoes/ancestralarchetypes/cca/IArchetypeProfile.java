package net.borisshoes.ancestralarchetypes.cca;

import net.borisshoes.ancestralarchetypes.Archetype;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.SubArchetype;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Pair;
import org.ladysnake.cca.api.v3.component.ComponentV3;

import java.util.List;
import java.util.UUID;

public interface IArchetypeProfile extends ComponentV3 {
   
   boolean hasAbility(ArchetypeAbility ability);
   int getDeathReductionSizeLevel();
   SubArchetype getSubArchetype();
   Archetype getArchetype();
   List<ArchetypeAbility> getAbilities();
   int getAbilityCooldown(ArchetypeAbility ability);
   ItemStack getPotionStack();
   int getGlideTime();
   int getMaxGlideTime();
   UUID getMountEntity(ArchetypeAbility ability);
   float getMountHealth(ArchetypeAbility ability);
   Inventory getMountInventory();
   HorseMarking getHorseMarking();
   HorseColor getHorseColor();
   int getGliderColor();
   boolean canChangeArchetype();
   boolean giveReminders();
   boolean giveAbilityItems(boolean shortCooldown);
   float getHealthUpdate();
   
   void setSubarchetype(SubArchetype subarchetype);
   void incrementDeathReductionSizeLevel();
   void resetDeathReductionSizeLevel();
   void setAbilityCooldown(ArchetypeAbility ability, int ticks);
   void tick();
   void setPotionType(Pair<Item, RegistryEntry<Potion>> pair);
   void resetAbilityCooldowns();
   void startGlideTimer();
   void setMountEntity(ArchetypeAbility ability, UUID uuid);
   void setMountHealth(ArchetypeAbility ability, float health);
   void setHorseVariant(HorseColor color, HorseMarking marking);
   void setGliderColor(int color);
   void changeArchetype(SubArchetype archetype);
   void increaseAllowedChanges(int num);
   void setReminders(boolean reminders);
   void setHealthUpdate(float health);
}
