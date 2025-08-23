package net.borisshoes.ancestralarchetypes.cca;

import net.borisshoes.ancestralarchetypes.*;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.ancestralarchetypes.utils.MiscUtils;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EquipmentSlot;
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
   private boolean gliderActive;
   private int deathReductionSizeLevel;
   private float glideTime;
   private int gliderColor = 0xFFFFFF;
   private int archetypeChangesAllowed = ArchetypeConfig.getInt(ArchetypeRegistry.STARTING_ARCHETYPE_CHANGES);
   private int giveItemsCooldown;
   private float healthUpdate;
   private ItemStack potionBrewerStack = ItemStack.EMPTY;
   private HorseMarking horseMarking = HorseMarking.NONE;
   private HorseColor horseColor = HorseColor.CHESTNUT;
   private String mountName = null;
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
      this.deathReductionSizeLevel = tag.getInt("deathReductionSizeLevel",0);
      this.glideTime = tag.getFloat("glideTime",0f);
      this.gliderActive = tag.getBoolean("gliderActive",false);
      this.archetypeChangesAllowed = tag.getInt("archetypeChangesAllowed",0);
      if(tag.contains("gliderColor")) this.gliderColor = tag.getInt("gliderColor",0xffffff);
      this.giveReminders = tag.getBoolean("giveReminders",ArchetypeConfig.getBoolean(ArchetypeRegistry.REMINDERS_ON_BY_DEFAULT));
      this.subArchetype = ArchetypeRegistry.SUBARCHETYPES.get(Identifier.of(MOD_ID, tag.getString("subArchetype","")));
      this.giveItemsCooldown = tag.getInt("giveItemsCooldown",0);
      this.healthUpdate = tag.getFloat("loginHealth",20f);
      this.calculateAbilities();
      
      abilityCooldowns.clear();
      if(tag.contains("cooldowns")){
         NbtCompound cooldownTag = tag.getCompound("cooldowns").orElse(new NbtCompound());
         for(String key : cooldownTag.getKeys()){
            ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.get(Identifier.of(MOD_ID, key));
            if(ability != null){
               abilityCooldowns.put(ability,cooldownTag.getInt(key,0));
            }
         }
      }
      
      if(tag.contains("potionBrewerStack")){
         this.potionBrewerStack = ItemStack.fromNbt(wrapperLookup,tag.getCompound("potionBrewerStack").orElse(new NbtCompound())).orElse(ItemStack.EMPTY);
      }
      
      if(tag.contains("horseMarking")){
         this.horseMarking = HorseMarking.byIndex(tag.getInt("horseMarking",0));
      }
      if(tag.contains("horseColor")){
         this.horseColor = HorseColor.byIndex(tag.getInt("horseColor",0));
      }
      if(tag.contains("mountName")){
         this.mountName = tag.getString("mountName","");
      }
      
      this.mountData.clear();
      if(tag.contains("mountData")){
         NbtCompound mountDataTag = tag.getCompound("mountData").orElse(new NbtCompound());
         for(String key : mountDataTag.getKeys()){
            ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.get(Identifier.of(MOD_ID, key));
            if(ability != null){
               NbtCompound entryTag = mountDataTag.getCompound(key).orElse(new NbtCompound());
               UUID uuid = MiscUtils.getUUIDOrNull(entryTag.getString("id",""));
               mountData.put(ability,new Pair<>(uuid,entryTag.getFloat("hp",1f)));
            }
         }
      }
      
      NbtList nbtList = tag.getList("mountInventory").orElse(new NbtList());
      for (int i = 0; i < nbtList.size(); ++i) {
         NbtCompound nbtCompound = nbtList.getCompound(i).orElse(new NbtCompound());
         int j = nbtCompound.getByte("Slot", (byte) 0) & 0xFF;
         if (j >= mountInventory.size()) continue;
         mountInventory.setStack(j, ItemStack.fromNbt(wrapperLookup, nbtCompound).orElse(ItemStack.EMPTY));
      }
   }
   
   @Override
   public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup wrapperLookup){
      tag.putInt("deathReductionSizeLevel",this.deathReductionSizeLevel);
      tag.putInt("gliderColor",this.gliderColor);
      tag.putInt("horseMarking",this.horseMarking.getIndex());
      tag.putInt("horseColor",this.horseColor.getIndex());
      tag.putInt("archetypeChangesAllowed",this.archetypeChangesAllowed);
      tag.putInt("giveItemsCooldown",this.giveItemsCooldown);
      tag.putFloat("glideTime",this.glideTime);
      tag.putFloat("loginHealth",this.healthUpdate);
      tag.putBoolean("giveReminders",this.giveReminders);
      tag.putBoolean("gliderActive",this.gliderActive);
      tag.putString("subArchetype",this.subArchetype != null ? this.subArchetype.getId() : "");
      if(this.mountName != null) tag.putString("mountName",this.mountName);
      
      NbtCompound cooldownTag = new NbtCompound();
      abilityCooldowns.forEach((ability, cooldown) -> cooldownTag.putInt(ability.getId(),cooldown));
      tag.put("cooldowns",cooldownTag);
      tag.put("potionBrewerStack",potionBrewerStack.toNbt(wrapperLookup));
      
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
   public float getGlideTime(){
      return this.glideTime;
   }
   
   @Override
   public int getMaxGlideTime(){
      return ArchetypeConfig.getInt(ArchetypeRegistry.GLIDER_DURATION);
   }
   
   @Override
   public boolean isGliderActive(){
      return this.gliderActive;
   }
   
   @Override
   public void setGliderActive(boolean active){
   
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
   public String getMountName(){
      return this.mountName;
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
   public void changeDeathReductionSizeLevel(boolean decrease){
      if(this.deathReductionSizeLevel > 0 && decrease){
         this.deathReductionSizeLevel--;
      }else if(!decrease){
         this.deathReductionSizeLevel++;
      }else{
         return;
      }
      
      MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
      MiscUtils.attributeEffect(player, EntityAttributes.SCALE,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
      
      double scale = -(1 - Math.pow(0.5,this.deathReductionSizeLevel));
      MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),false);
      MiscUtils.attributeEffect(player, EntityAttributes.SCALE,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),false);
      player.setHealth(player.getMaxHealth());
   }
   
   
   @Override
   public void resetDeathReductionSizeLevel(){
      this.deathReductionSizeLevel = 0;
      MiscUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
      MiscUtils.attributeEffect(player, EntityAttributes.SCALE,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
   }
   
   @Override
   public void setAbilityCooldown(ArchetypeAbility ability, int ticks){
      this.abilityCooldowns.put(ability,ticks);
   }
   
   @Override
   public void tick(){
      abilityCooldowns.forEach((ability, cooldown) -> abilityCooldowns.put(ability, Math.max(0,cooldown-1)));
      
      boolean wasGliderActive = this.gliderActive;
      if(player.getEquippedStack(EquipmentSlot.CHEST).isOf(ArchetypeRegistry.GLIDER_ITEM) && player.isGliding()){
         if(!wasGliderActive) this.gliderActive = true;
         this.glideTime--;
      }else{
         if(wasGliderActive){
            this.gliderActive = false;
            setAbilityCooldown(ArchetypeRegistry.GLIDER,ArchetypeConfig.getInt(ArchetypeRegistry.GLIDER_COOLDOWN));
         }
         if(getAbilityCooldown(ArchetypeRegistry.GLIDER) == 0){
            this.glideTime = (float) Math.min(ArchetypeConfig.getInt(ArchetypeRegistry.GLIDER_DURATION),this.glideTime + ArchetypeConfig.getDouble(ArchetypeRegistry.GLIDER_RECOVERY_TIME));
         }
      }
      
      if(this.glideTime < 0){
         this.glideTime = 0;
         setAbilityCooldown(ArchetypeRegistry.GLIDER,ArchetypeConfig.getInt(ArchetypeRegistry.GLIDER_COOLDOWN));
      }
      
      if(this.giveItemsCooldown > 0) this.giveItemsCooldown--;
      
      if(ArchetypeConfig.getBoolean(ArchetypeRegistry.CAN_ALWAYS_CHANGE_ARCHETYPE) && !this.canChangeArchetype()){
         this.archetypeChangesAllowed++;
      }
   }
   
   @Override
   public void resetAbilityCooldowns(){
      abilityCooldowns.forEach((ability, cooldown) -> abilityCooldowns.put(ability,0));
      this.glideTime = ArchetypeConfig.getInt(ArchetypeRegistry.GLIDER_DURATION);
   }
   
   @Override
   public void setPotionType(Pair<Item, RegistryEntry<Potion>> pair){
      this.potionBrewerStack = pair == null ? ItemStack.EMPTY : PotionContentsComponent.createStack(pair.getLeft(),pair.getRight());
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
   public void setMountName(String name){
      this.mountName = name;
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
   public void setHealthUpdate(float health){
      if(health >= 0) this.healthUpdate = health;
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
   
   @Override
   public float getHealthUpdate(){
      return healthUpdate;
   }
}
