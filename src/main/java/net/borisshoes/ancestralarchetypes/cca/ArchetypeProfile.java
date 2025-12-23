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
      this.calculateAbilities();
      
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
      return abilityCooldowns.getOrDefault(ability, new CooldownEntry(0)).getCooldown();
   }
   
   @Override
   public float getAbilityCooldownPercent(ArchetypeAbility ability){
      return abilityCooldowns.getOrDefault(ability, new CooldownEntry(0)).getPercentage();
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
      return CONFIG.getInt(ArchetypeRegistry.GLIDER_DURATION);
   }
   
   @Override
   public float getHoverTime(){
      return this.hoverTime;
   }
   
   @Override
   public int getMaxHoverTime(){
      return CONFIG.getInt(ArchetypeRegistry.SLOW_HOVER_FLIGHT_DURATION);
   }
   
   @Override
   public float getFortifyTime(){
      return this.fortifyTime;
   }
   
   @Override
   public int getMaxFortifyTime(){
      return CONFIG.getInt(ArchetypeRegistry.FORTIFY_DURATION);
   }
   
   @Override
   public boolean isFortifyActive(){
      return this.fortifyActive;
   }
   
   @Override
   public void setFortifyActive(boolean fortifyActive){
      this.fortifyActive = fortifyActive;
   }
   
   @Override
   public boolean isFungusBoosted(){
      return this.fungusBoostTime > 0;
   }
   
   @Override
   public void fungusBoost(){
      this.fungusBoostTime = CONFIG.getInt(ArchetypeRegistry.FUNGUS_SPEED_BOOST_DURATION);
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
      
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH,0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"death_reduction_size_level"),true);
      MinecraftUtils.attributeEffect(player, Attributes.SCALE,0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"death_reduction_size_level"),true);
      
      double scale = -(1 - Math.pow(0.5,this.deathReductionSizeLevel));
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH,scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"death_reduction_size_level"),false);
      MinecraftUtils.attributeEffect(player, Attributes.SCALE,scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"death_reduction_size_level"),false);
      player.setHealth(player.getMaxHealth());
   }
   
   
   @Override
   public void resetDeathReductionSizeLevel(){
      this.deathReductionSizeLevel = 0;
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH,0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"death_reduction_size_level"),true);
      MinecraftUtils.attributeEffect(player, Attributes.SCALE,0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"death_reduction_size_level"),true);
   }
   
   @Override
   public void setAbilityCooldown(ArchetypeAbility ability, int ticks){
      this.abilityCooldowns.put(ability, new CooldownEntry(ticks));
   }
   
   @Override
   public void tick(){
      abilityCooldowns.forEach((ability, cooldown) -> cooldown.tick());
      
      handleGlider();
      handleHover();
      handleFortify();
      
      if(this.fungusBoostTime > 0){
         this.fungusBoostTime--;
         ((ServerLevel)player.level()).sendParticles(new DustParticleOptions(0x20c7b1,0.75f),player.position().x(),player.position().y()+player.getBbHeight()/2.0,player.position().z(),1,player.getBbWidth()*0.65,player.getBbHeight()/2.0,player.getBbWidth()*0.65,1);
      }
      if(this.giveItemsCooldown > 0) this.giveItemsCooldown--;
      
      if(CONFIG.getBoolean(ArchetypeRegistry.CAN_ALWAYS_CHANGE_ARCHETYPE) && !this.canChangeArchetype()){
         this.archetypeChangesAllowed++;
      }
   }
   
   private void handleFortify(){
      if(!this.fortifyActive){
         this.fortifyTime = (float) Math.min(CONFIG.getInt(ArchetypeRegistry.FORTIFY_DURATION),this.fortifyTime + CONFIG.getDouble(ArchetypeRegistry.FORTIFY_RECOVERY_TIME));
      }else{
         this.fortifyTime--;
         if((int)this.fortifyTime % 2 == 0){
            double height = player.getBbHeight()/2*(Math.sin(Math.PI*2.0/60.0*fortifyTime)+1);
            ParticleEffectUtils.circle((ServerLevel) player.level(),null,player.position().add(0,height,0), ParticleTypes.END_ROD,player.getBbWidth(),(int)(player.getBbWidth()*12),1,0,0);
         }
      }
   }
   
   private void handleGlider(){
      boolean wasGliderActive = this.gliderActive;
      boolean gliderEquipped = player.getItemBySlot(EquipmentSlot.CHEST).is(ArchetypeRegistry.GLIDER_ITEM) || player.getItemBySlot(EquipmentSlot.CHEST).is(ArchetypeRegistry.END_GLIDER_ITEM);
      if(gliderEquipped && player.isFallFlying()){
         if(!wasGliderActive) this.gliderActive = true;
         this.glideTime--;
      }else{
         if(wasGliderActive){
            this.gliderActive = false;
            setAbilityCooldown(ArchetypeRegistry.WING_GLIDER,CONFIG.getInt(ArchetypeRegistry.GLIDER_COOLDOWN));
            setAbilityCooldown(ArchetypeRegistry.ENDER_GLIDER,CONFIG.getInt(ArchetypeRegistry.GLIDER_COOLDOWN));
         }
         if(getAbilityCooldown(ArchetypeRegistry.WING_GLIDER) + getAbilityCooldown(ArchetypeRegistry.ENDER_GLIDER) == 0){
            this.glideTime = (float) Math.min(CONFIG.getInt(ArchetypeRegistry.GLIDER_DURATION),this.glideTime + CONFIG.getDouble(ArchetypeRegistry.GLIDER_RECOVERY_TIME));
         }
      }
      
      if(this.glideTime < 0){
         this.glideTime = 0;
         setAbilityCooldown(ArchetypeRegistry.WING_GLIDER,CONFIG.getInt(ArchetypeRegistry.GLIDER_COOLDOWN));
         setAbilityCooldown(ArchetypeRegistry.ENDER_GLIDER,CONFIG.getInt(ArchetypeRegistry.GLIDER_COOLDOWN));
      }
   }
   
   private void handleHover(){
      boolean canHover = !player.isCreative() && player.getItemBySlot(EquipmentSlot.HEAD).is(ArchetypeRegistry.SLOW_HOVER_ITEM) && getAbilityCooldown(ArchetypeRegistry.SLOW_HOVER) == 0 && this.hoverTime > 0;
      if(SLOW_HOVER_ABILITY.grants(player, VanillaAbilities.ALLOW_FLYING) && !canHover){
         SLOW_HOVER_ABILITY.revokeFrom(player, VanillaAbilities.ALLOW_FLYING);
      }else if(!SLOW_HOVER_ABILITY.grants(player, VanillaAbilities.ALLOW_FLYING) && canHover){
         SLOW_HOVER_ABILITY.grantTo(player, VanillaAbilities.ALLOW_FLYING);
      }
      boolean hovering = !player.isCreative() && VanillaAbilities.ALLOW_FLYING.getTracker(player).isEnabled() &&
            VanillaAbilities.ALLOW_FLYING.getTracker(player).isGrantedBy(SLOW_HOVER_ABILITY) &&
            VanillaAbilities.FLYING.isEnabledFor(player);
      boolean wasHovering = this.hoverActive;
      if(hovering){
         if(!wasHovering){
            this.hoverActive = true;
            SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS,0.5f,1.25f);
            SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.HARNESS_GOGGLES_DOWN, SoundSource.PLAYERS,0.5f,0.8f);
            this.savedFlySpeed = player.getAbilities().getFlyingSpeed();
            player.getAbilities().setFlyingSpeed((float) (CONFIG.getDouble(ArchetypeRegistry.SLOW_HOVER_FLIGHT_SPEED)));
            ((ServerPlayer)player).connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
         }
         this.hoverTime--;
         if(player.getRandom().nextDouble() < 0.4) ((ServerPlayer)player).level().sendParticles(ParticleTypes.POOF,player.getX(),player.getY()-0.5,player.getZ(),1,0.2,0.2,0.2,0.01);
      }else{
         if(wasHovering){
            this.hoverActive = false;
            setAbilityCooldown(ArchetypeRegistry.SLOW_HOVER,CONFIG.getInt(ArchetypeRegistry.SLOW_HOVER_FLIGHT_COOLDOWN));
            SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS,0.5f,0.85f);
            SoundUtils.playSound(player.level(),player.blockPosition(), SoundEvents.HARNESS_GOGGLES_UP, SoundSource.PLAYERS,0.5f,0.8f);
            player.getAbilities().setFlyingSpeed(this.savedFlySpeed);
            ((ServerPlayer)player).connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
         }
         if(getAbilityCooldown(ArchetypeRegistry.SLOW_HOVER) == 0){
            this.hoverTime = (float) Math.min(CONFIG.getInt(ArchetypeRegistry.SLOW_HOVER_FLIGHT_DURATION),this.hoverTime + CONFIG.getDouble(ArchetypeRegistry.SLOW_HOVER_FLIGHT_RECOVERY_TIME));
         }
      }
      
      if(this.hoverTime < 0){
         this.hoverTime = 0;
         setAbilityCooldown(ArchetypeRegistry.SLOW_HOVER,CONFIG.getInt(ArchetypeRegistry.SLOW_HOVER_FLIGHT_COOLDOWN));
      }
   }
   
   @Override
   public void resetAbilityCooldowns(){
      abilityCooldowns.forEach((ability, cooldown) -> abilityCooldowns.put(ability, new CooldownEntry(0)));
      this.glideTime = getMaxGlideTime();
      this.hoverTime = getMaxHoverTime();
      this.fortifyTime = getMaxFortifyTime();
   }
   
   @Override
   public void setPotionType(Tuple<Item, Holder<Potion>> pair){
      this.potionBrewerStack = pair == null ? ItemStack.EMPTY : PotionContents.createItemStack(pair.getA(),pair.getB());
   }
   
   @Override
   public void setMountEntity(ArchetypeAbility ability, UUID uuid){
      Tuple<UUID,Float> prev = this.mountData.get(ability);
      if(prev != null){
         this.mountData.put(ability, new Tuple<>(uuid,prev.getB()));
      }else{
         this.mountData.put(ability, new Tuple<>(uuid,0f));
      }
   }
   
   @Override
   public void setMountHealth(ArchetypeAbility ability, float health){
      Tuple<UUID,Float> prev = this.mountData.get(ability);
      if(prev != null){
         this.mountData.put(ability, new Tuple<>(prev.getA(),health));
      }else{
         this.mountData.put(ability, new Tuple<>(null,health));
      }
   }
   
   @Override
   public void setMountName(String name){
      this.mountName = name;
   }
   
   @Override
   public void setHorseVariant(Variant color, Markings marking){
      this.horseMarking = marking;
      this.horseColor = color;
   }
   
   @Override
   public void setGliderColor(int color){
      this.gliderColor = color;
   }
   
   @Override
   public void setHelmetColor(int color){
      this.helmetColor = color;
   }
   
   public void setGliderTrimMaterial(Holder<TrimMaterial> material){
      this.gliderTrimMaterial = material;
   }
   
   public void setHelmetTrimMaterial(Holder<TrimMaterial> material){
      this.helmetTrimMaterial = material;
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
      Inventory inv = player.getInventory();
      for(ArchetypeAbility ability : abilities){
         if(!ability.active()) continue;
         for(Item item : ITEMS){
            if(item instanceof AbilityItem abilityItem && ability.equals(abilityItem.ability)){
               boolean found = false;
               for(int i = 0; i < inv.getContainerSize(); i++){
                  ItemStack stack = inv.getItem(i);
                  if(stack.is(abilityItem)){
                     found = true;
                     break;
                  }
               }
               if(!found){
                  MinecraftUtils.giveStacks(player, new ItemStack(item));
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
