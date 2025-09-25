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
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrimMaterial;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.*;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.ITEMS;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.SLOW_HOVER_ABILITY;

public class ArchetypeProfile implements IArchetypeProfile {
   
   private final PlayerEntity player;
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
   private RegistryEntry<ArmorTrimMaterial> gliderTrimMaterial;
   private RegistryEntry<ArmorTrimMaterial> helmetTrimMaterial;
   private int archetypeChangesAllowed = CONFIG.getInt(ArchetypeRegistry.STARTING_ARCHETYPE_CHANGES);
   private int giveItemsCooldown;
   private float healthUpdate;
   private ItemStack potionBrewerStack = ItemStack.EMPTY;
   private HorseMarking horseMarking = HorseMarking.NONE;
   private HorseColor horseColor = HorseColor.CHESTNUT;
   private String mountName = null;
   private SubArchetype subArchetype;
   private final ArrayList<ArchetypeAbility> abilities = new ArrayList<>();
   private final HashMap<ArchetypeAbility,CooldownEntry> abilityCooldowns = new HashMap<>();
   private final HashMap<ArchetypeAbility,Pair<UUID,Float>> mountData = new HashMap<>();
   private final SimpleInventory mountInventory = new SimpleInventory(54);
   private final SimpleInventory backpackInventory = new SimpleInventory(18);
   
   public ArchetypeProfile(PlayerEntity player){
      this.player = player;
   }
   
   @Override
   public void readData(ReadView view){
      this.deathReductionSizeLevel = view.getInt("deathReductionSizeLevel",0);
      this.glideTime = view.getFloat("glideTime",0f);
      this.hoverTime = view.getFloat("hoverTime",0f);
      this.fortifyTime = view.getFloat("fortifyTime",0f);
      this.savedFlySpeed = view.getFloat("savedFlySpeed",0.05f);
      this.gliderActive = view.getBoolean("gliderActive",false);
      this.hoverActive = view.getBoolean("hoverActive", false);
      this.fortifyActive = view.getBoolean("fortifyActive", false);
      this.archetypeChangesAllowed = view.getInt("archetypeChangesAllowed",0);
      this.fungusBoostTime = view.getInt("fungusBoostTime",0);
      this.gliderColor = view.getInt("gliderColor",0xffffff);
      this.helmetColor = view.getInt("helmetColor",0xA06540);
      this.gliderTrimMaterial = BorisLib.SERVER.getRegistryManager().getOrThrow(RegistryKeys.TRIM_MATERIAL).getEntry(Identifier.of(view.getString("gliderTrimMaterial",""))).orElse(null);
      this.helmetTrimMaterial = BorisLib.SERVER.getRegistryManager().getOrThrow(RegistryKeys.TRIM_MATERIAL).getEntry(Identifier.of(view.getString("helmetTrimMaterial",""))).orElse(null);
      this.giveReminders = view.getBoolean("giveReminders",CONFIG.getBoolean(ArchetypeRegistry.REMINDERS_ON_BY_DEFAULT));
      this.subArchetype = ArchetypeRegistry.SUBARCHETYPES.get(Identifier.of(MOD_ID, view.getString("subArchetype","")));
      this.giveItemsCooldown = view.getInt("giveItemsCooldown",0);
      this.healthUpdate = view.getFloat("loginHealth",20f);
      this.calculateAbilities();
      
      abilityCooldowns.clear();
      NbtCompound cooldownTag = view.read("cooldowns",NbtCompound.CODEC).orElse(new NbtCompound());
      for(String key : cooldownTag.getKeys()){
         NbtCompound compound = cooldownTag.getCompound(key).orElse(new NbtCompound());
         ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.get(Identifier.of(MOD_ID, key));
         if(ability != null){
            abilityCooldowns.put(ability, new CooldownEntry(compound.getInt("cooldown", 0), compound.getInt("duration", 0)));
         }
      }
      
      this.potionBrewerStack = view.read("potionBrewerStack",ItemStack.CODEC).orElse(ItemStack.EMPTY);
      this.horseMarking = HorseMarking.byIndex(view.getInt("horseMarking",0));
      this.horseColor = HorseColor.byIndex(view.getInt("horseColor",0));
      this.mountName = view.getString("mountName","");
      
      this.mountData.clear();
      NbtCompound mountDataTag = view.read("mountData",NbtCompound.CODEC).orElse(new NbtCompound());
      for(String key : mountDataTag.getKeys()){
         ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.get(Identifier.of(MOD_ID, key));
         if(ability != null){
            NbtCompound entryTag = mountDataTag.getCompound(key).orElse(new NbtCompound());
            UUID uuid = AlgoUtils.getUUID(entryTag.getString("id",""));
            mountData.put(ability,new Pair<>(uuid,entryTag.getFloat("hp",1f)));
         }
      }
      
      this.mountInventory.clear();
      for(StackWithSlot stackWithSlot : view.getTypedListView("mountInventory", StackWithSlot.CODEC)){
         if(stackWithSlot.isValidSlot(this.mountInventory.getHeldStacks().size())){
            this.mountInventory.getHeldStacks().set(stackWithSlot.slot(), stackWithSlot.stack());
         }
      }
      
      this.backpackInventory.clear();
      for(StackWithSlot stackWithSlot : view.getTypedListView("backpackInventory", StackWithSlot.CODEC)){
         if(stackWithSlot.isValidSlot(this.backpackInventory.getHeldStacks().size())){
            this.backpackInventory.getHeldStacks().set(stackWithSlot.slot(), stackWithSlot.stack());
         }
      }
   }
   
   @Override
   public void writeData(WriteView view){
      view.putInt("deathReductionSizeLevel",this.deathReductionSizeLevel);
      view.putInt("gliderColor",this.gliderColor);
      view.putInt("helmetColor",this.helmetColor);
      view.putInt("horseMarking",this.horseMarking.getIndex());
      view.putInt("horseColor",this.horseColor.getIndex());
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
      view.putString("gliderTrimMaterial",this.gliderTrimMaterial == null ? "" : this.gliderTrimMaterial.getIdAsString());
      view.putString("helmetTrimMaterial",this.helmetTrimMaterial == null ? "" : this.helmetTrimMaterial.getIdAsString());
      if(this.mountName != null) view.putString("mountName",this.mountName);
      
      NbtCompound cooldownTag = new NbtCompound();
      for(Map.Entry<ArchetypeAbility, CooldownEntry> entry : abilityCooldowns.entrySet()){
         NbtCompound cooldownAbilityTag = new NbtCompound();
         cooldownAbilityTag.putInt("cooldown",entry.getValue().getCooldown());
         cooldownAbilityTag.putInt("duration",entry.getValue().getTotalDuration());
         cooldownTag.put(entry.getKey().getId(),cooldownAbilityTag);
      }
      view.put("cooldowns",NbtCompound.CODEC,cooldownTag);
      if(!this.potionBrewerStack.isEmpty()) view.put("potionBrewerStack",ItemStack.CODEC,potionBrewerStack);
      
      NbtCompound mountDataTag = new NbtCompound();
      mountData.forEach((ability, pair) -> {
         NbtCompound tagEntry = new NbtCompound();
         tagEntry.putString("id", pair.getLeft() != null ? pair.getLeft().toString() : "");
         tagEntry.putFloat("hp", pair.getRight());
         mountDataTag.put(ability.getId(),tagEntry);
      });
      view.put("mountData",NbtCompound.CODEC,mountDataTag);
      
      WriteView.ListAppender<StackWithSlot> listAppender = view.getListAppender("mountInventory", StackWithSlot.CODEC);
      for(int i = 0; i < this.mountInventory.getHeldStacks().size(); ++i) {
         ItemStack itemStack = this.mountInventory.getHeldStacks().get(i);
         if (!itemStack.isEmpty()) {
            listAppender.add(new StackWithSlot(i, itemStack));
         }
      }
      listAppender.isEmpty();
      
      listAppender = view.getListAppender("backpackInventory", StackWithSlot.CODEC);
      for(int i = 0; i < this.backpackInventory.getHeldStacks().size(); ++i) {
         ItemStack itemStack = this.backpackInventory.getHeldStacks().get(i);
         if (!itemStack.isEmpty()) {
            listAppender.add(new StackWithSlot(i, itemStack));
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
   public SimpleInventory getBackpackInventory(){
      return this.backpackInventory;
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
   public int getHelmetColor(){
      return this.helmetColor;
   }
   
   public RegistryEntry<ArmorTrimMaterial> getGliderTrimMaterial(){
      return this.gliderTrimMaterial;
   }
   
   public RegistryEntry<ArmorTrimMaterial> getHelmetTrimMaterial(){
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
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
      MinecraftUtils.attributeEffect(player, EntityAttributes.SCALE,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
      
      double scale = -(1 - Math.pow(0.5,this.deathReductionSizeLevel));
      MinecraftUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),false);
      MinecraftUtils.attributeEffect(player, EntityAttributes.SCALE,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),false);
      player.setHealth(player.getMaxHealth());
   }
   
   
   @Override
   public void resetDeathReductionSizeLevel(){
      this.deathReductionSizeLevel = 0;
      MinecraftUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
      MinecraftUtils.attributeEffect(player, EntityAttributes.SCALE,0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
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
         ((ServerWorld)player.getWorld()).spawnParticles(new DustParticleEffect(0x20c7b1,0.75f),player.getPos().getX(),player.getPos().getY()+player.getHeight()/2.0,player.getPos().getZ(),1,player.getWidth()*0.65,player.getHeight()/2.0,player.getWidth()*0.65,1);
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
            double height = player.getHeight()/2*(Math.sin(Math.PI*2.0/60.0*fortifyTime)+1);
            ParticleEffectUtils.circle((ServerWorld) player.getWorld(),null,player.getPos().add(0,height,0),ParticleTypes.END_ROD,player.getWidth(),(int)(player.getWidth()*12),1,0,0);
         }
      }
   }
   
   private void handleGlider(){
      boolean wasGliderActive = this.gliderActive;
      boolean gliderEquipped = player.getEquippedStack(EquipmentSlot.CHEST).isOf(ArchetypeRegistry.GLIDER_ITEM) || player.getEquippedStack(EquipmentSlot.CHEST).isOf(ArchetypeRegistry.END_GLIDER_ITEM);
      if(gliderEquipped && player.isGliding()){
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
      boolean canHover = !player.isCreative() && player.getEquippedStack(EquipmentSlot.HEAD).isOf(ArchetypeRegistry.SLOW_HOVER_ITEM) && getAbilityCooldown(ArchetypeRegistry.SLOW_HOVER) == 0 && this.hoverTime > 0;
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
            SoundUtils.playSound(player.getWorld(),player.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS,0.5f,1.25f);
            SoundUtils.playSound(player.getWorld(),player.getBlockPos(), SoundEvents.ENTITY_HAPPY_GHAST_HARNESS_GOGGLES_DOWN, SoundCategory.PLAYERS,0.5f,0.8f);
            this.savedFlySpeed = player.getAbilities().getFlySpeed();
            player.getAbilities().setFlySpeed((float) (CONFIG.getDouble(ArchetypeRegistry.SLOW_HOVER_FLIGHT_SPEED)));
            ((ServerPlayerEntity)player).networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));
         }
         this.hoverTime--;
         if(player.getRandom().nextDouble() < 0.4) ((ServerPlayerEntity)player).getWorld().spawnParticles(ParticleTypes.POOF,player.getX(),player.getY()-0.5,player.getZ(),1,0.2,0.2,0.2,0.01);
      }else{
         if(wasHovering){
            this.hoverActive = false;
            setAbilityCooldown(ArchetypeRegistry.SLOW_HOVER,CONFIG.getInt(ArchetypeRegistry.SLOW_HOVER_FLIGHT_COOLDOWN));
            SoundUtils.playSound(player.getWorld(),player.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS,0.5f,0.85f);
            SoundUtils.playSound(player.getWorld(),player.getBlockPos(), SoundEvents.ENTITY_HAPPY_GHAST_HARNESS_GOGGLES_UP, SoundCategory.PLAYERS,0.5f,0.8f);
            player.getAbilities().setFlySpeed(this.savedFlySpeed);
            ((ServerPlayerEntity)player).networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));
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
   public void setHelmetColor(int color){
      this.helmetColor = color;
   }
   
   public void setGliderTrimMaterial(RegistryEntry<ArmorTrimMaterial> material){
      this.gliderTrimMaterial = material;
   }
   
   public void setHelmetTrimMaterial(RegistryEntry<ArmorTrimMaterial> material){
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
