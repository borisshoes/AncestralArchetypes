package net.borisshoes.ancestralarchetypes.cca;

import net.borisshoes.ancestralarchetypes.*;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.ancestralarchetypes.utils.MiscUtils;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.potion.Potion;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.*;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.ITEMS;

public class ArchetypeProfile implements IArchetypeProfile {
   
   private final PlayerEntity player;
   private boolean giveReminders = ArchetypeConfig.getBoolean(ArchetypeRegistry.REMINDERS_ON_BY_DEFAULT);
   private int deathReductionSizeLevel;
   private int glideTime;
   private int gliderColor = 0xFFFFFF;
   private int archetypeChangesAllowed = ArchetypeConfig.getInt(ArchetypeRegistry.STARTING_ARCHETYPE_CHANGES);
   private int giveItemsCooldown;
   private ItemStack potionBrewerStack = ItemStack.EMPTY;
   private HorseMarking horseMarking = HorseMarking.NONE;
   private HorseColor horseColor = HorseColor.CHESTNUT;
   private SubArchetype subArchetype;
   private final ArrayList<ArchetypeAbility> abilities = new ArrayList<>();
   private final HashMap<ArchetypeAbility,Integer> abilityCooldowns = new HashMap<>();
   private final HashMap<ArchetypeAbility,Pair<UUID,Float>> mountData = new HashMap<>();
   private final SimpleInventory mountInventory = new SimpleInventory(54);
   
   public ArchetypeProfile(PlayerEntity player){
      this.player = player;
   }
   
   @Override
   public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup wrapperLookup){
      this.deathReductionSizeLevel = tag.getInt("deathReductionSizeLevel");
      this.glideTime = tag.getInt("glideTime");
      this.archetypeChangesAllowed = tag.getInt("archetypeChangesAllowed");
      if(tag.contains("gliderColor")) this.gliderColor = tag.getInt("gliderColor");
      this.giveReminders = tag.getBoolean("giveReminders");
      this.subArchetype = ArchetypeRegistry.SUBARCHETYPES.get(Identifier.of(MOD_ID, tag.getString("subArchetype")));
      this.giveItemsCooldown = tag.getInt("giveItemsCooldown");
      this.calculateAbilities();
      
      abilityCooldowns.clear();
      if(tag.contains("cooldowns")){
         NbtCompound cooldownTag = tag.getCompound("cooldowns");
         for(String key : cooldownTag.getKeys()){
            ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.get(Identifier.of(MOD_ID, key));
            if(ability != null){
               abilityCooldowns.put(ability,cooldownTag.getInt(key));
            }
         }
      }
      
      if(tag.contains("potionBrewerStack")){
         this.potionBrewerStack = ItemStack.fromNbtOrEmpty(wrapperLookup,tag.getCompound("potionBrewerStack"));
      }
      
      if(tag.contains("horseMarking")){
         this.horseMarking = HorseMarking.byIndex(tag.getInt("horseMarking"));
      }
      if(tag.contains("horseColor")){
         this.horseColor = HorseColor.byId(tag.getInt("horseColor"));
      }
      
      this.mountData.clear();
      if(tag.contains("mountData")){
         NbtCompound mountDataTag = tag.getCompound("mountData");
         for(String key : mountDataTag.getKeys()){
            ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.get(Identifier.of(MOD_ID, key));
            if(ability != null){
               NbtCompound entryTag = mountDataTag.getCompound(key);
               UUID uuid = MiscUtils.getUUIDOrNull(entryTag.getString("id"));
               mountData.put(ability,new Pair<>(uuid,entryTag.getFloat("hp")));
            }
         }
      }
      
      NbtList nbtList = tag.getList("mountInventory", NbtElement.COMPOUND_TYPE);
      for (int i = 0; i < nbtList.size(); ++i) {
         NbtCompound nbtCompound = nbtList.getCompound(i);
         int j = nbtCompound.getByte("Slot") & 0xFF;
         if (j >= mountInventory.size()) continue;
         mountInventory.setStack(j, ItemStack.fromNbt(wrapperLookup, nbtCompound).orElse(ItemStack.EMPTY));
      }
   }
   
   @Override
   public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup wrapperLookup){
      tag.putInt("deathReductionSizeLevel",this.deathReductionSizeLevel);
      tag.putInt("glideTime",this.glideTime);
      tag.putInt("gliderColor",this.gliderColor);
      tag.putInt("horseMarking",this.horseMarking.getId());
      tag.putInt("horseColor",this.horseColor.getId());
      tag.putInt("archetypeChangesAllowed",this.archetypeChangesAllowed);
      tag.putInt("giveItemsCooldown",this.giveItemsCooldown);
      tag.putBoolean("giveReminders",this.giveReminders);
      tag.putString("subArchetype",this.subArchetype != null ? this.subArchetype.getId() : "");
      
      NbtCompound cooldownTag = new NbtCompound();
      abilityCooldowns.forEach((ability, cooldown) -> cooldownTag.putInt(ability.getId(),cooldown));
      tag.put("cooldowns",cooldownTag);
      tag.put("potionBrewerStack",potionBrewerStack.toNbtAllowEmpty(wrapperLookup));
      
      NbtCompound mountDataTag = new NbtCompound();
      mountData.forEach((ability, pair) -> {
         NbtCompound tagEntry = new NbtCompound();
         tagEntry.putString("id", pair.getLeft() != null ? pair.getLeft().toString() : "");
         tagEntry.putFloat("hp", pair.getRight());
         mountDataTag.put(ability.getId(),tagEntry);
      });
      tag.put("mountData",mountDataTag);
      
      NbtList nbtList = new NbtList();
      for (int i = 0; i < mountInventory.size(); ++i) {
         ItemStack itemStack = mountInventory.getStack(i);
         if (itemStack.isEmpty()) continue;
         NbtCompound nbtCompound = new NbtCompound();
         nbtCompound.putByte("Slot", (byte)i);
         nbtList.add(itemStack.toNbt(wrapperLookup, nbtCompound));
      }
      tag.put("mountInventory", nbtList);
   }
   
   private void calculateAbilities(){
      this.abilities.clear();
      if(this.subArchetype == null) return;
      abilities.addAll(Arrays.asList(this.subArchetype.getAbilities()));
      abilities.addAll(Arrays.asList(this.subArchetype.getArchetype().getAbilities()));
      abilities.removeIf(a1 -> abilities.stream().anyMatch(a2 -> a2.overrides(a1)));
   }
   
   @Override
   public List<ArchetypeAbility> getAbilities(){
      return new ArrayList<>(this.abilities);
   }
   
   @Override
   public int getAbilityCooldown(ArchetypeAbility ability){
      return abilityCooldowns.getOrDefault(ability,0);
   }
   
   @Override
   public ItemStack getPotionStack(){
      return this.potionBrewerStack.copy();
   }
   
   @Override
   public int getGlideTime(){
      return this.glideTime;
   }
   
   @Override
   public int getMaxGlideTime(){
      return ArchetypeConfig.getInt(ArchetypeRegistry.GLIDER_DURATION);
   }
   
   @Override
   public UUID getMountEntity(ArchetypeAbility ability){
      Pair<UUID,Float> data = this.mountData.get(ability);
      if(data != null){
         return data.getLeft();
      }else{
         return null;
      }
   }
   
   @Override
   public float getMountHealth(ArchetypeAbility ability){
      Pair<UUID,Float> data = this.mountData.get(ability);
      if(data != null){
         return data.getRight();
      }else{
         return 0f;
      }
   }
   
   @Override
   public boolean hasAbility(ArchetypeAbility ability){
      if(this.subArchetype == null) return false;
      return abilities.contains(ability);
   }
   
   @Override
   public int getDeathReductionSizeLevel(){
      return this.deathReductionSizeLevel;
   }
   
   @Override
   public SubArchetype getSubArchetype(){
      return this.subArchetype;
   }
   
   @Override
   public Archetype getArchetype(){
      if(this.subArchetype != null){
         return this.subArchetype.getArchetype();
      }
      return null;
   }
   
   @Override
   public SimpleInventory getMountInventory(){
      return this.mountInventory;
   }
   
   @Override
   public HorseMarking getHorseMarking(){
      return this.horseMarking;
   }
   
   @Override
   public HorseColor getHorseColor(){
      return this.horseColor;
   }
   
   @Override
   public int getGliderColor(){
      return this.gliderColor;
   }
   
   @Override
   public boolean canChangeArchetype(){
      return this.archetypeChangesAllowed > 0;
   }
   
   @Override
   public boolean giveReminders(){
      return this.giveReminders;
   }
   
   @Override
   public void setSubarchetype(SubArchetype subarchetype){
      this.subArchetype = subarchetype;
      this.calculateAbilities();
      resetDeathReductionSizeLevel();
   }
   
   @Override
   public void incrementDeathReductionSizeLevel(){
      this.deathReductionSizeLevel++;
      MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
      MiscUtils.attributeEffect(player, EntityAttributes.SCALE,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
      
      double scale = -(1 - Math.pow(0.5,this.deathReductionSizeLevel));
      MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"death_reduction_size_level"),false);
      MiscUtils.attributeEffect(player, EntityAttributes.SCALE,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"death_reduction_size_level"),false);
   }
   
   @Override
   public void resetDeathReductionSizeLevel(){
      this.deathReductionSizeLevel = 0;
      MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
      MiscUtils.attributeEffect(player, EntityAttributes.SCALE,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
   }
   
   @Override
   public void setAbilityCooldown(ArchetypeAbility ability, int ticks){
      this.abilityCooldowns.put(ability,ticks);
   }
   
   @Override
   public void tick(){
      abilityCooldowns.forEach((ability, cooldown) -> abilityCooldowns.put(ability, Math.max(0,cooldown-1)));
      
      if(this.glideTime > 0){
         this.glideTime++;
         
         if(this.glideTime >= getMaxGlideTime()){
            this.glideTime = 0;
            setAbilityCooldown(ArchetypeRegistry.GLIDER,ArchetypeConfig.getInt(ArchetypeRegistry.GLIDER_COOLDOWN));
         }
      }
      
      if(this.giveItemsCooldown > 0) this.giveItemsCooldown--;
      
      if(ArchetypeConfig.getBoolean(ArchetypeRegistry.CAN_ALWAYS_CHANGE_ARCHETYPE) && !this.canChangeArchetype()){
         this.archetypeChangesAllowed++;
      }
   }
   
   @Override
   public void resetAbilityCooldowns(){
      abilityCooldowns.forEach((ability, cooldown) -> abilityCooldowns.put(ability,0));
   }
   
   @Override
   public void setPotionType(Pair<Item, RegistryEntry<Potion>> pair){
      this.potionBrewerStack = PotionContentsComponent.createStack(pair.getLeft(),pair.getRight());
   }
   
   @Override
   public void startGlideTimer(){
      this.glideTime = 1;
   }
   
   @Override
   public void setMountEntity(ArchetypeAbility ability, UUID uuid){
      Pair<UUID,Float> prev = this.mountData.get(ability);
      if(prev != null){
         this.mountData.put(ability, new Pair<>(uuid,prev.getRight()));
      }else{
         this.mountData.put(ability, new Pair<>(uuid,0f));
      }
   }
   
   @Override
   public void setMountHealth(ArchetypeAbility ability, float health){
      Pair<UUID,Float> prev = this.mountData.get(ability);
      if(prev != null){
         this.mountData.put(ability, new Pair<>(prev.getLeft(),health));
      }else{
         this.mountData.put(ability, new Pair<>(null,health));
      }
   }
   
   @Override
   public void setHorseVariant(HorseColor color, HorseMarking marking){
      this.horseMarking = marking;
      this.horseColor = color;
   }
   
   @Override
   public void setGliderColor(int color){
      this.gliderColor = color;
   }
   
   @Override
   public void changeArchetype(SubArchetype archetype){
      setSubarchetype(archetype);
      this.giveItemsCooldown = 0;
      giveAbilityItems(true);
      this.archetypeChangesAllowed = Math.max(0,this.archetypeChangesAllowed-1);
   }
   
   @Override
   public void increaseAllowedChanges(int num){
      this.archetypeChangesAllowed = Math.max(0, this.archetypeChangesAllowed + num);
   }
   
   @Override
   public void setReminders(boolean reminders){
      this.giveReminders = reminders;
   }
   
   @Override
   public boolean giveAbilityItems(boolean shortCooldown){
      if(this.giveItemsCooldown > 0) return false;
      
      List<ArchetypeAbility> abilities = getAbilities();
      PlayerInventory inv = player.getInventory();
      for(ArchetypeAbility ability : abilities){
         if(!ability.isActive()) continue;
         for(Item item : ITEMS){
            if(item instanceof AbilityItem abilityItem && ability.equals(abilityItem.ability)){
               boolean found = false;
               for(int i = 0; i < inv.size(); i++){
                  ItemStack stack = inv.getStack(i);
                  if(stack.isOf(abilityItem)){
                     found = true;
                     break;
                  }
               }
               if(!found){
                  MiscUtils.giveStacks(player, new ItemStack(item));
                  break;
               }
            }
         }
      }
      this.giveItemsCooldown = shortCooldown ? 100 : 1200;
      
      return true;
   }
}
