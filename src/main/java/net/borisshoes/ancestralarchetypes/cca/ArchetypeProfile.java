package net.borisshoes.ancestralarchetypes.cca;

import io.github.ladysnake.pal.VanillaAbilities;
import net.borisshoes.ancestralarchetypes.Archetype;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.SubArchetype;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.utils.AlgoUtils;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.ParticleEffectUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.*;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.ITEMS;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.SLOW_HOVER_ABILITY;

public class ArchetypeProfile implements IArchetypeProfile {
   
   private final Player player;
   private boolean giveReminders = CONFIG.getBoolean(ArchetypeRegistry.REMINDERS_ON_BY_DEFAULT);
   private boolean gliderActive;
   private boolean hoverActive;
   private boolean fortifyActive;
   private int deathReductionSizeLevel;
   private float glideTime;
   private float hoverTime;
   private float fortifyTime;
   private float savedFlySpeed = 0.05f;
   private int fungusBoostTime;
   private int gliderColor = 0xFFFFFF;
   private int helmetColor = 0xA06540;
   private Holder<TrimMaterial> gliderTrimMaterial;
   private Holder<TrimMaterial> helmetTrimMaterial;
   private int archetypeChangesAllowed = CONFIG.getInt(ArchetypeRegistry.STARTING_ARCHETYPE_CHANGES);
   private int giveItemsCooldown;
   private float healthUpdate;
   private ItemStack potionBrewerStack = ItemStack.EMPTY;
   private Markings horseMarking = Markings.NONE;
   private Variant horseColor = Variant.CHESTNUT;
   private String mountName = null;
   private SubArchetype subArchetype;
   private final ArrayList<ArchetypeAbility> abilities = new ArrayList<>();
   private final HashMap<ArchetypeAbility,CooldownEntry> abilityCooldowns = new HashMap<>();
   private final HashMap<ArchetypeAbility, Tuple<UUID,Float>> mountData = new HashMap<>();
   private final SimpleContainer mountInventory = new SimpleContainer(54);
   private final SimpleContainer backpackInventory = new SimpleContainer(18);
   
   public ArchetypeProfile(Player player){
      this.player = player;
   }
   
   @Override
   public boolean hasData(){
      if(this.subArchetype != null) return true;
      if(!this.mountInventory.isEmpty()) return true;
      if(!this.backpackInventory.isEmpty()) return true;
      return false;
   }
   
   @Override
   public void clearData(){
      this.giveReminders = CONFIG.getBoolean(ArchetypeRegistry.REMINDERS_ON_BY_DEFAULT);
      this.gliderActive = false;
      this.hoverActive = false;
      this.fortifyActive = false;
      this.deathReductionSizeLevel = 0;
      this.glideTime = 0f;
      this.hoverTime = 0f;
      this.fortifyTime = 0f;
      this.savedFlySpeed = 0.05f;
      this.fungusBoostTime = 0;
      this.gliderColor = 0xFFFFFF;
      this.helmetColor = 0xA06540;
      this.gliderTrimMaterial = null;
      this.helmetTrimMaterial = null;
      this.archetypeChangesAllowed = 0;
      this.giveItemsCooldown = 0;
      this.healthUpdate = 20f;
      this.potionBrewerStack = ItemStack.EMPTY;
      this.horseMarking = Markings.NONE;
      this.horseColor = Variant.CHESTNUT;
      this.mountName = null;
      this.subArchetype = null;
      this.abilities.clear();
      this.abilityCooldowns.clear();
      this.mountData.clear();
      this.mountInventory.clearContent();
      this.backpackInventory.clearContent();
   }
   
   @Override
   public void readData(ValueInput view){
      this.deathReductionSizeLevel = view.getIntOr("deathReductionSizeLevel",0);
      this.glideTime = view.getFloatOr("glideTime",0f);
      this.hoverTime = view.getFloatOr("hoverTime",0f);
      this.fortifyTime = view.getFloatOr("fortifyTime",0f);
      this.savedFlySpeed = view.getFloatOr("savedFlySpeed",0.05f);
      this.gliderActive = view.getBooleanOr("gliderActive",false);
      this.hoverActive = view.getBooleanOr("hoverActive", false);
      this.fortifyActive = view.getBooleanOr("fortifyActive", false);
      this.archetypeChangesAllowed = view.getIntOr("archetypeChangesAllowed",0);
      this.fungusBoostTime = view.getIntOr("fungusBoostTime",0);
      this.gliderColor = view.getIntOr("gliderColor",0xffffff);
      this.helmetColor = view.getIntOr("helmetColor",0xA06540);
      this.gliderTrimMaterial = BorisLib.SERVER.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).get(Identifier.parse(view.getStringOr("gliderTrimMaterial",""))).orElse(null);
      this.helmetTrimMaterial = BorisLib.SERVER.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL).get(Identifier.parse(view.getStringOr("helmetTrimMaterial",""))).orElse(null);
      this.giveReminders = view.getBooleanOr("giveReminders",CONFIG.getBoolean(ArchetypeRegistry.REMINDERS_ON_BY_DEFAULT));
      this.subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID, view.getStringOr("subArchetype","")));
      this.giveItemsCooldown = view.getIntOr("giveItemsCooldown",0);
      this.healthUpdate = view.getFloatOr("loginHealth",20f);
      
      abilityCooldowns.clear();
      CompoundTag cooldownTag = view.read("cooldowns", CompoundTag.CODEC).orElse(new CompoundTag());
      for(String key : cooldownTag.keySet()){
         CompoundTag compound = cooldownTag.getCompound(key).orElse(new CompoundTag());
         ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.getValue(Identifier.fromNamespaceAndPath(MOD_ID, key));
         if(ability != null){
            abilityCooldowns.put(ability, new CooldownEntry(compound.getIntOr("cooldown", 0), compound.getIntOr("duration", 0)));
         }
      }
      
      this.potionBrewerStack = view.read("potionBrewerStack", ItemStack.CODEC).orElse(ItemStack.EMPTY);
      this.horseMarking = Markings.byId(view.getIntOr("horseMarking",0));
      this.horseColor = Variant.byId(view.getIntOr("horseColor",0));
      this.mountName = view.getStringOr("mountName","");
      
      this.mountData.clear();
      CompoundTag mountDataTag = view.read("mountData", CompoundTag.CODEC).orElse(new CompoundTag());
      for(String key : mountDataTag.keySet()){
         ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.getValue(Identifier.fromNamespaceAndPath(MOD_ID, key));
         if(ability != null){
            CompoundTag entryTag = mountDataTag.getCompound(key).orElse(new CompoundTag());
            UUID uuid = AlgoUtils.getUUID(entryTag.getStringOr("id",""));
            mountData.put(ability,new Tuple<>(uuid,entryTag.getFloatOr("hp",1f)));
         }
      }
      
      this.mountInventory.clearContent();
      for(ItemStackWithSlot stackWithSlot : view.listOrEmpty("mountInventory", ItemStackWithSlot.CODEC)){
         if(stackWithSlot.isValidInContainer(this.mountInventory.getItems().size())){
            this.mountInventory.getItems().set(stackWithSlot.slot(), stackWithSlot.stack());
         }
      }
      
      this.backpackInventory.clearContent();
      for(ItemStackWithSlot stackWithSlot : view.listOrEmpty("backpackInventory", ItemStackWithSlot.CODEC)){
         if(stackWithSlot.isValidInContainer(this.backpackInventory.getItems().size())){
            this.backpackInventory.getItems().set(stackWithSlot.slot(), stackWithSlot.stack());
         }
      }
   }
   
   @Override
   public void writeData(ValueOutput view){
      view.putInt("deathReductionSizeLevel",this.deathReductionSizeLevel);
      view.putInt("gliderColor",this.gliderColor);
      view.putInt("helmetColor",this.helmetColor);
      view.putInt("horseMarking",this.horseMarking.getId());
      view.putInt("horseColor",this.horseColor.getId());
      view.putInt("archetypeChangesAllowed",this.archetypeChangesAllowed);
      view.putInt("giveItemsCooldown",this.giveItemsCooldown);
      view.putInt("fungusBoostTime",this.fungusBoostTime);
      view.putFloat("glideTime",this.glideTime);
      view.putFloat("hoverTime",this.hoverTime);
      view.putFloat("fortifyTime",this.fortifyTime);
      view.putFloat("savedFlySpeed",this.savedFlySpeed);
      view.putFloat("loginHealth",this.healthUpdate);
      view.putBoolean("giveReminders",this.giveReminders);
      view.putBoolean("gliderActive",this.gliderActive);
      view.putBoolean("hoverActive",this.hoverActive);
      view.putBoolean("fortifyActive",this.fortifyActive);
      view.putString("subArchetype",this.subArchetype != null ? this.subArchetype.getId() : "");
      view.putString("gliderTrimMaterial",this.gliderTrimMaterial == null ? "" : this.gliderTrimMaterial.getRegisteredName());
      view.putString("helmetTrimMaterial",this.helmetTrimMaterial == null ? "" : this.helmetTrimMaterial.getRegisteredName());
      if(this.mountName != null) view.putString("mountName",this.mountName);
      
      CompoundTag cooldownTag = new CompoundTag();
      for(Map.Entry<ArchetypeAbility, CooldownEntry> entry : abilityCooldowns.entrySet()){
         CompoundTag cooldownAbilityTag = new CompoundTag();
         cooldownAbilityTag.putInt("cooldown",entry.getValue().getCooldown());
         cooldownAbilityTag.putInt("duration",entry.getValue().getTotalDuration());
         cooldownTag.put(entry.getKey().id(),cooldownAbilityTag);
      }
      view.store("cooldowns", CompoundTag.CODEC,cooldownTag);
      if(!this.potionBrewerStack.isEmpty()) view.store("potionBrewerStack", ItemStack.CODEC,potionBrewerStack);
      
      CompoundTag mountDataTag = new CompoundTag();
      mountData.forEach((ability, pair) -> {
         CompoundTag tagEntry = new CompoundTag();
         tagEntry.putString("id", pair.getA() != null ? pair.getA().toString() : "");
         tagEntry.putFloat("hp", pair.getB());
         mountDataTag.put(ability.id(),tagEntry);
      });
      view.store("mountData", CompoundTag.CODEC,mountDataTag);
      
      ValueOutput.TypedOutputList<ItemStackWithSlot> listAppender = view.list("mountInventory", ItemStackWithSlot.CODEC);
      for(int i = 0; i < this.mountInventory.getItems().size(); ++i) {
         ItemStack itemStack = this.mountInventory.getItems().get(i);
         if (!itemStack.isEmpty()) {
            listAppender.add(new ItemStackWithSlot(i, itemStack));
         }
      }
      listAppender.isEmpty();
      
      listAppender = view.list("backpackInventory", ItemStackWithSlot.CODEC);
      for(int i = 0; i < this.backpackInventory.getItems().size(); ++i) {
         ItemStack itemStack = this.backpackInventory.getItems().get(i);
         if (!itemStack.isEmpty()) {
            listAppender.add(new ItemStackWithSlot(i, itemStack));
         }
      }
      listAppender.isEmpty();
   }
   
   @Override
   public List<ArchetypeAbility> getAbilities(){
      return new ArrayList<>(this.abilities);
   }
   
   @Override
   public int getAbilityCooldown(ArchetypeAbility ability){
      return abilityCooldowns.getOrDefault(ability, new CooldownEntry(0)).getCooldown();
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
   public float getHoverTime(){
      return this.hoverTime;
   }
   
   @Override
   public float getFortifyTime(){
      return this.fortifyTime;
   }
   
   @Override
   public boolean isFortifyActive(){
      return this.fortifyActive;
   }
   
   @Override
   public UUID getMountEntity(ArchetypeAbility ability){
      Tuple<UUID,Float> data = this.mountData.get(ability);
      if(data != null){
         return data.getA();
      }else{
         return null;
      }
   }
   
   @Override
   public float getMountHealth(ArchetypeAbility ability){
      Tuple<UUID,Float> data = this.mountData.get(ability);
      if(data != null){
         return data.getB();
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
   public SimpleContainer getMountInventory(){
      return this.mountInventory;
   }
   
   @Override
   public SimpleContainer getBackpackInventory(){
      return this.backpackInventory;
   }
   
   @Override
   public Markings getHorseMarking(){
      return this.horseMarking;
   }
   
   @Override
   public Variant getHorseColor(){
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
   public int getHelmetColor(){
      return this.helmetColor;
   }
   
   public Holder<TrimMaterial> getGliderTrimMaterial(){
      return this.gliderTrimMaterial;
   }
   
   public Holder<TrimMaterial> getHelmetTrimMaterial(){
      return this.helmetTrimMaterial;
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
   public float getHealthUpdate(){
      return healthUpdate;
   }
   
   
   private static class CooldownEntry{
      private final int totalDuration;
      private int cooldown;
      public CooldownEntry(int duration){
         this.totalDuration = duration;
         this.cooldown = duration;
      }
      private CooldownEntry(int cooldown, int totalDuration){
         this.totalDuration = totalDuration;
         this.cooldown = cooldown;
      }
      
      public boolean isExpired(){
         return cooldown <= 0;
      }
      
      public boolean tick(){
         if(cooldown > 0) cooldown--;
         return isExpired();
      }
      
      public int getCooldown(){
         return cooldown;
      }
      
      public int getTotalDuration(){
         return totalDuration;
      }
      
      public float getPercentage(){
         if(totalDuration == 0) return 0;
         return (float) cooldown / (float) totalDuration;
      }
   }
}
