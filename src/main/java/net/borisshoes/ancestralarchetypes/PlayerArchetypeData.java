package net.borisshoes.ancestralarchetypes;

import com.mojang.datafixers.util.Pair;
import io.github.ladysnake.pal.VanillaAbilities;
import net.borisshoes.ancestralarchetypes.callbacks.MetamorphTNTShieldCallback;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.ancestralarchetypes.misc.ArchetypeUtils;
import net.borisshoes.ancestralarchetypes.misc.EcholocationVibrationSystem;
import net.borisshoes.ancestralarchetypes.misc.MetamorphTypes;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.datastorage.DataKey;
import net.borisshoes.borislib.datastorage.DataRegistry;
import net.borisshoes.borislib.datastorage.StorableData;
import net.borisshoes.borislib.utils.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.ITEMS;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.SLOW_HOVER_ABILITY;
import static net.borisshoes.ancestralarchetypes.items.MetamorphHeadItem.METAMORPH_HIDDEN_MODEL;
import static net.borisshoes.ancestralarchetypes.items.MetamorphHeadItem.METAMORPH_VISIBLE_MODEL;

public class PlayerArchetypeData implements StorableData {
   
   public static final DataKey<PlayerArchetypeData> KEY = DataRegistry.register(DataKey.ofPlayer(archetypesId("playerdata"), PlayerArchetypeData::new));
   
   private final UUID playerID;
   private String username = "";
   private boolean giveReminders = CONFIG.getBoolean(ArchetypeRegistry.REMINDERS_ON_BY_DEFAULT);
   private boolean gliderActive;
   private boolean hoverActive;
   private boolean fortifyActive;
   private int deathReductionSizeLevel;
   private float glideTime;
   private float hoverTime;
   private float fortifyTime;
   private int metamorphTime;
   private int metamorphFuseTime;
   private boolean metamorphTntShield;
   private float savedFlySpeed = 0.05f;
   private int leapCooldown;
   private int leapCharge;
   private int gliderColor = 0xFFFFFF;
   private int helmetColor = 0xA06540;
   private long ticksSinceArchetypeChange = CONFIG.getInt(ArchetypeRegistry.ARCHETYPE_CHANGE_COOLDOWN);
   private Holder<TrimMaterial> gliderTrimMaterial;
   private Holder<TrimMaterial> helmetTrimMaterial;
   private int archetypeChangesAllowed = CONFIG.getInt(ArchetypeRegistry.STARTING_ARCHETYPE_CHANGES);
   private int giveItemsCooldown;
   private float healthUpdate;
   private MetamorphTypes activeMetamorph;
   private EcholocationVibrationSystem vibrationSystem;
   private ItemStack potionBrewerStack = ItemStack.EMPTY;
   private Markings horseMarking = Markings.NONE;
   private Variant horseColor = Variant.CHESTNUT;
   private String mountName = null;
   private SubArchetype subArchetype;
   private final Set<ArchetypeAbility> abilities = new HashSet<>();
   private final HashMap<ArchetypeAbility, CooldownEntry> abilityCooldowns = new HashMap<>();
   private final HashMap<ArchetypeAbility, Pair<UUID, Float>> mountData = new HashMap<>();
   private final SimpleContainer mountInventory = new SimpleContainer(54);
   private final SimpleContainer backpackInventory = new SimpleContainer(18);
   
   public PlayerArchetypeData(UUID playerID){
      this.playerID = playerID;
   }
   
   /**
    * Converts an ItemStack to SNBT string using the server's registry access
    */
   private static String itemStackToSnbt(ItemStack stack){
      if(stack.isEmpty() || BorisLib.SERVER == null) return "";
      try{
         RegistryOps<Tag> ops = BorisLib.SERVER.registryAccess().createSerializationContext(NbtOps.INSTANCE);
         Tag tag = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
         return tag.toString();
      }catch(Exception e){
         AncestralArchetypes.LOGGER.warn("Failed to serialize ItemStack to SNBT: {}", e.getMessage());
         return "";
      }
   }
   
   /**
    * Parses an ItemStack from SNBT string using the server's registry access
    */
   private static ItemStack itemStackFromSnbt(String snbt){
      if(snbt == null || snbt.isEmpty() || BorisLib.SERVER == null) return ItemStack.EMPTY;
      try{
         CompoundTag tag = TagParser.parseCompoundFully(snbt);
         RegistryOps<Tag> ops = BorisLib.SERVER.registryAccess().createSerializationContext(NbtOps.INSTANCE);
         return ItemStack.CODEC.decode(ops, tag).getOrThrow().getFirst();
      }catch(Exception e){
         AncestralArchetypes.LOGGER.warn("Failed to parse ItemStack from SNBT '{}': {}", snbt, e.getMessage());
         return ItemStack.EMPTY;
      }
   }
   
   @Override
   public void read(ValueInput view){
      this.username = view.getStringOr("username", "");
      this.giveReminders = view.getBooleanOr("giveReminders", CONFIG.getBoolean(ArchetypeRegistry.REMINDERS_ON_BY_DEFAULT));
      this.gliderActive = view.getBooleanOr("gliderActive", false);
      this.hoverActive = view.getBooleanOr("hoverActive", false);
      this.fortifyActive = view.getBooleanOr("fortifyActive", false);
      this.deathReductionSizeLevel = view.getIntOr("deathReductionSizeLevel", 0);
      this.glideTime = view.getFloatOr("glideTime", 0f);
      this.hoverTime = view.getFloatOr("hoverTime", 0f);
      this.fortifyTime = view.getFloatOr("fortifyTime", 0f);
      this.savedFlySpeed = view.getFloatOr("savedFlySpeed", 0.05f);
      this.leapCooldown = view.getIntOr("leapCooldown", 0);
      this.leapCharge = view.getIntOr("leapCharge", 0);
      this.gliderColor = view.getIntOr("gliderColor", 0xFFFFFF);
      this.helmetColor = view.getIntOr("helmetColor", 0xA06540);
      this.archetypeChangesAllowed = view.getIntOr("archetypeChangesAllowed", CONFIG.getInt(ArchetypeRegistry.STARTING_ARCHETYPE_CHANGES));
      this.giveItemsCooldown = view.getIntOr("giveItemsCooldown", 0);
      this.healthUpdate = view.getFloatOr("loginHealth", 20f);
      this.horseMarking = Markings.byId(view.getIntOr("horseMarking", 0));
      this.horseColor = Variant.byId(view.getIntOr("horseColor", 0));
      this.mountName = view.getStringOr("mountName", "");
      if(this.mountName.isEmpty()) this.mountName = null;
      this.ticksSinceArchetypeChange = view.getLongOr("ticksSinceArchetypeChange", CONFIG.getInt(ArchetypeRegistry.ARCHETYPE_CHANGE_COOLDOWN));
      this.metamorphTime = view.getIntOr("metamorphTime", 0);
      this.metamorphFuseTime = view.getIntOr("metamorphFuseTime", 0);
      this.metamorphTntShield = view.getBooleanOr("metamorphTntShield", false);
      this.activeMetamorph = MetamorphTypes.fromString(view.getStringOr("activeMetamorph", ""));
      
      // Resolve trim materials from registry
      try{
         String gliderTrimStr = view.getStringOr("gliderTrimMaterial", "");
         if(BorisLib.SERVER != null && !gliderTrimStr.isEmpty()){
            this.gliderTrimMaterial = BorisLib.SERVER.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL)
                  .get(Identifier.parse(gliderTrimStr)).orElse(null);
         }
      }catch(Exception e){
         AncestralArchetypes.LOGGER.warn("Failed to resolve glider trim material: {}", e.getMessage());
         this.gliderTrimMaterial = null;
      }
      
      try{
         String helmetTrimStr = view.getStringOr("helmetTrimMaterial", "");
         if(BorisLib.SERVER != null && !helmetTrimStr.isEmpty()){
            this.helmetTrimMaterial = BorisLib.SERVER.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL)
                  .get(Identifier.parse(helmetTrimStr)).orElse(null);
         }
      }catch(Exception e){
         AncestralArchetypes.LOGGER.warn("Failed to resolve helmet trim material: {}", e.getMessage());
         this.helmetTrimMaterial = null;
      }
      
      // Resolve subarchetype from registry
      try{
         String subArchetypeStr = view.getStringOr("subArchetype", "");
         if(!subArchetypeStr.isEmpty()){
            this.subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(archetypesId(subArchetypeStr));
         }
      }catch(Exception e){
         AncestralArchetypes.LOGGER.warn("Failed to resolve subarchetype: {}", e.getMessage());
         this.subArchetype = null;
      }
      this.calculateAbilities();
      
      // Parse cooldowns
      abilityCooldowns.clear();
      CompoundTag cooldownTag = view.read("cooldowns", CompoundTag.CODEC).orElse(new CompoundTag());
      for(String key : cooldownTag.keySet()){
         try{
            CompoundTag compound = cooldownTag.getCompound(key).orElse(new CompoundTag());
            ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.getValue(archetypesId(key));
            if(ability != null){
               abilityCooldowns.put(ability, new CooldownEntry(compound.getIntOr("cooldown", 0), compound.getIntOr("duration", 0)));
            }
         }catch(Exception e){
            AncestralArchetypes.LOGGER.warn("Failed to parse cooldown for ability '{}': {}", key, e.getMessage());
         }
      }
      
      // Parse potion brewer stack from SNBT string
      String potionSnbt = view.getStringOr("potionBrewerStack", "");
      this.potionBrewerStack = itemStackFromSnbt(potionSnbt);
      
      // Parse mount data
      this.mountData.clear();
      CompoundTag mountDataTag = view.read("mountData", CompoundTag.CODEC).orElse(new CompoundTag());
      for(String key : mountDataTag.keySet()){
         try{
            ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.getValue(archetypesId(key));
            if(ability != null){
               CompoundTag entryTag = mountDataTag.getCompound(key).orElse(new CompoundTag());
               UUID uuid = AlgoUtils.getUUID(entryTag.getStringOr("id", ""));
               mountData.put(ability, new Pair<>(uuid, entryTag.getFloatOr("hp", 1f)));
            }
         }catch(Exception e){
            AncestralArchetypes.LOGGER.warn("Failed to parse mount data for ability '{}': {}", key, e.getMessage());
         }
      }
      
      // Parse mount inventory using ItemStackWithSlot codec for backwards compatibility
      this.mountInventory.clearContent();
      for(ItemStackWithSlot stackWithSlot : view.listOrEmpty("mountInventory", ItemStackWithSlot.CODEC)){
         if(stackWithSlot.isValidInContainer(this.mountInventory.getItems().size())){
            this.mountInventory.getItems().set(stackWithSlot.slot(), stackWithSlot.stack());
         }
      }
      
      // Parse backpack inventory using ItemStackWithSlot codec for backwards compatibility
      this.backpackInventory.clearContent();
      for(ItemStackWithSlot stackWithSlot : view.listOrEmpty("backpackInventory", ItemStackWithSlot.CODEC)){
         if(stackWithSlot.isValidInContainer(this.backpackInventory.getItems().size())){
            this.backpackInventory.getItems().set(stackWithSlot.slot(), stackWithSlot.stack());
         }
      }
   }
   
   @Override
   public void writeNbt(CompoundTag tag){
      tag.putString("username", username);
      tag.putBoolean("giveReminders", giveReminders);
      tag.putBoolean("gliderActive", gliderActive);
      tag.putBoolean("hoverActive", hoverActive);
      tag.putBoolean("fortifyActive", fortifyActive);
      tag.putInt("deathReductionSizeLevel", deathReductionSizeLevel);
      tag.putFloat("glideTime", glideTime);
      tag.putFloat("hoverTime", hoverTime);
      tag.putFloat("fortifyTime", fortifyTime);
      tag.putFloat("savedFlySpeed", savedFlySpeed);
      tag.putInt("leapCooldown", leapCooldown);
      tag.putInt("leapCharge", leapCharge);
      tag.putInt("gliderColor", gliderColor);
      tag.putInt("helmetColor", helmetColor);
      tag.putInt("archetypeChangesAllowed", archetypeChangesAllowed);
      tag.putInt("giveItemsCooldown", giveItemsCooldown);
      tag.putFloat("loginHealth", healthUpdate);
      tag.putInt("horseMarking", horseMarking.getId());
      tag.putInt("horseColor", horseColor.getId());
      tag.putLong("ticksSinceArchetypeChange", ticksSinceArchetypeChange);
      tag.putInt("metamorphTime", metamorphTime);
      tag.putInt("metamorphFuseTime", metamorphFuseTime);
      tag.putBoolean("metamorphTntShield", metamorphTntShield);
      tag.putString("activeMetamorph", activeMetamorph != null ? activeMetamorph.name() : "");
      
      if(mountName != null) tag.putString("mountName", mountName);
      tag.putString("subArchetype", subArchetype != null ? subArchetype.getId() : "");
      tag.putString("gliderTrimMaterial", gliderTrimMaterial == null ? "" : gliderTrimMaterial.getRegisteredName());
      tag.putString("helmetTrimMaterial", helmetTrimMaterial == null ? "" : helmetTrimMaterial.getRegisteredName());
      
      // Serialize potion brewer stack as SNBT string
      if(!potionBrewerStack.isEmpty()){
         tag.putString("potionBrewerStack", itemStackToSnbt(potionBrewerStack));
      }
      
      // Serialize cooldowns
      CompoundTag cooldownTag = new CompoundTag();
      for(Map.Entry<ArchetypeAbility, CooldownEntry> entry : abilityCooldowns.entrySet()){
         CompoundTag cooldownAbilityTag = new CompoundTag();
         cooldownAbilityTag.putInt("cooldown", entry.getValue().getCooldown());
         cooldownAbilityTag.putInt("duration", entry.getValue().getTotalDuration());
         cooldownTag.put(entry.getKey().id(), cooldownAbilityTag);
      }
      tag.put("cooldowns", cooldownTag);
      
      // Serialize mount data
      CompoundTag mountDataTag = new CompoundTag();
      mountData.forEach((ability, pair) -> {
         CompoundTag tagEntry = new CompoundTag();
         tagEntry.putString("id", pair.getFirst() != null ? pair.getFirst().toString() : "");
         tagEntry.putFloat("hp", pair.getSecond());
         mountDataTag.put(ability.id(), tagEntry);
      });
      tag.put("mountData", mountDataTag);
      
      // Serialize mount inventory using ItemStackWithSlot codec
      if(BorisLib.SERVER != null){
         RegistryOps<Tag> ops = BorisLib.SERVER.registryAccess().createSerializationContext(NbtOps.INSTANCE);
         ListTag mountInvList = new ListTag();
         for(int i = 0; i < mountInventory.getItems().size(); ++i){
            ItemStack itemStack = mountInventory.getItems().get(i);
            if(!itemStack.isEmpty()){
               ItemStackWithSlot.CODEC.encodeStart(ops, new ItemStackWithSlot(i, itemStack)).result().ifPresent(mountInvList::add);
            }
         }
         tag.put("mountInventory", mountInvList);
         
         ListTag backpackInvList = new ListTag();
         for(int i = 0; i < backpackInventory.getItems().size(); ++i){
            ItemStack itemStack = backpackInventory.getItems().get(i);
            if(!itemStack.isEmpty()){
               ItemStackWithSlot.CODEC.encodeStart(ops, new ItemStackWithSlot(i, itemStack)).result().ifPresent(backpackInvList::add);
            }
         }
         tag.put("backpackInventory", backpackInvList);
      }
   }
   
   private void calculateAbilities(){
      this.abilities.clear();
      if(this.subArchetype == null) return;
      abilities.addAll(subArchetype.getActualAbilities());
      
      if(activeMetamorph == MetamorphTypes.SCULK){
         abilities.add(ArchetypeRegistry.NEARSIGHTED);
         abilities.add(ArchetypeRegistry.ECHOLOCATION);
      }else if(activeMetamorph == MetamorphTypes.GOLD || activeMetamorph == MetamorphTypes.IRON || activeMetamorph == MetamorphTypes.NETHERITE){
         abilities.remove(ArchetypeRegistry.BOUNCY);
         abilities.remove(ArchetypeRegistry.JUMPY);
      }else if(activeMetamorph == MetamorphTypes.WOOL || activeMetamorph == MetamorphTypes.ICE){
         abilities.remove(ArchetypeRegistry.BOUNCY);
      }
   }
   
   public Set<ArchetypeAbility> getAbilities(){
      return this.abilities;
   }
   
   public int getAbilityCooldown(ArchetypeAbility ability){
      return abilityCooldowns.getOrDefault(ability, new CooldownEntry(0)).getCooldown();
   }
   
   public float getAbilityCooldownPercent(ArchetypeAbility ability){
      return abilityCooldowns.getOrDefault(ability, new CooldownEntry(0)).getPercentage();
   }
   
   public ItemStack getPotionStack(){
      return this.potionBrewerStack.copy();
   }
   
   public float getGlideTime(){
      return this.glideTime;
   }
   
   public int getMaxGlideTime(){
      return CONFIG.getInt(ArchetypeRegistry.GLIDER_DURATION);
   }
   
   public float getHoverTime(){
      return this.hoverTime;
   }
   
   public int getMaxHoverTime(){
      return CONFIG.getInt(ArchetypeRegistry.SLOW_HOVER_FLIGHT_DURATION);
   }
   
   public float getFortifyTime(){
      return this.fortifyTime;
   }
   
   public int getMaxFortifyTime(){
      return CONFIG.getInt(ArchetypeRegistry.FORTIFY_DURATION);
   }
   
   public boolean isFortifyActive(){
      return this.fortifyActive;
   }
   
   public void setFortifyActive(boolean fortifyActive){
      this.fortifyActive = fortifyActive;
   }
   
   public UUID getMountEntity(ArchetypeAbility ability){
      Pair<UUID, Float> data = this.mountData.get(ability);
      if(data != null){
         return data.getFirst();
      }else{
         return null;
      }
   }
   
   public float getMountHealth(ArchetypeAbility ability){
      Pair<UUID, Float> data = this.mountData.get(ability);
      if(data != null){
         return data.getSecond();
      }else{
         return 0f;
      }
   }
   
   public boolean hasAbility(ArchetypeAbility ability){
      if(this.subArchetype == null) return false;
      return abilities.contains(ability);
   }
   
   public int getDeathReductionSizeLevel(){
      return this.deathReductionSizeLevel;
   }
   
   public UUID getPlayerID(){
      return playerID;
   }
   
   public String getUsername(){
      return this.username;
   }
   
   private void setUsername(String username){
      this.username = username != null ? username : "";
   }
   
   public SubArchetype getSubArchetype(){
      return this.subArchetype;
   }
   
   public Archetype getArchetype(){
      if(this.subArchetype != null){
         return this.subArchetype.getArchetype();
      }
      return null;
   }
   
   public SimpleContainer getMountInventory(){
      return this.mountInventory;
   }
   
   public SimpleContainer getBackpackInventory(){
      return this.backpackInventory;
   }
   
   public Markings getHorseMarking(){
      return this.horseMarking;
   }
   
   public Variant getHorseColor(){
      return this.horseColor;
   }
   
   public String getMountName(){
      return this.mountName;
   }
   
   public int getGliderColor(){
      return this.gliderColor;
   }
   
   public int getHelmetColor(){
      return this.helmetColor;
   }
   
   public Holder<TrimMaterial> getGliderTrimMaterial(){
      return this.gliderTrimMaterial;
   }
   
   public Holder<TrimMaterial> getHelmetTrimMaterial(){
      return this.helmetTrimMaterial;
   }
   
   public void metamorph(MetamorphTypes type, ServerPlayer player){
      this.activeMetamorph = type;
      this.metamorphFuseTime = 0;
      this.metamorphTntShield = false;
      this.metamorphTime = type == null ? 0 : CONFIG.getInt(ArchetypeRegistry.METAMORPH_ABILITY_DURATION);
      calculateAbilities();
      resetVibrationSystem(player);
      
      if(this.activeMetamorph == null){
         ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
         String hiddenHelmetModel = archetypes$ITEM_DATA.getStringProperty(headStack, METAMORPH_HIDDEN_MODEL);
         if(!hiddenHelmetModel.isEmpty()){
            ArchetypeUtils.removeMetamorphHelmetTags(headStack);
         }else if(headStack.is(ArchetypeRegistry.METAMORPH_HELMET_ITEM)){
            player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
         }
      }
   }
   
   public boolean isMetamorphed(){
      return this.activeMetamorph != null;
   }
   
   public MetamorphTypes getMetamorph(){
      return this.activeMetamorph;
   }
   
   public int getMetamorphTime(){
      return this.metamorphTime;
   }
   
   public int getMetamorphFuseTime(){
      return metamorphFuseTime;
   }
   
   public boolean canMetamorphTntDeathShield(){
      return !this.metamorphTntShield;
   }
   
   public void metamorphIgniteFire(ServerPlayer player){
      metamorphTNTPrime(player, CONFIG.getInt(ArchetypeRegistry.METAMORPH_TNT_FIRE_FUSE_TIME));
   }
   
   public void metamorphIgniteExplosion(ServerPlayer player){
      metamorphTNTPrime(player, CONFIG.getInt(ArchetypeRegistry.METAMORPH_TNT_EXPLOSION_FUSE_TIME));
   }
   
   public void metamorphIgniteDeath(ServerPlayer player){
      int fuseTime = CONFIG.getInt(ArchetypeRegistry.METAMORPH_TNT_DEATH_FUSE_TIME);
      if(this.metamorphFuseTime <= 0){
         metamorphTNTPrime(player, fuseTime);
      }
      if(!metamorphTntShield){
         float curAbs = player.getAbsorptionAmount();
         float addedAbs = CONFIG.getFloat((ArchetypeRegistry.METAMORPH_TNT_DEATH_ABSORPTION_HP));
         BorisLib.addTickTimerCallback(new MetamorphTNTShieldCallback(fuseTime, player, addedAbs));
         MinecraftUtils.addMaxAbsorption(player, archetypesId(ArchetypeRegistry.BERRY_EATER.id()), addedAbs);
         player.setAbsorptionAmount((curAbs + addedAbs));
      }
      metamorphTntShield = true;
   }
   
   private void metamorphTNTPrime(ServerPlayer player, int time){
      if(activeMetamorph == MetamorphTypes.TNT && metamorphFuseTime == 0){
         metamorphFuseTime = time;
         SoundUtils.playSound(player.level(), BlockPos.containing(player.position()), SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 2.0f, 0.5f);
      }
   }
   
   public boolean canChangeArchetype(){
      return this.archetypeChangesAllowed > 0 && this.ticksSinceArchetypeChange > CONFIG.getInt(ArchetypeRegistry.ARCHETYPE_CHANGE_COOLDOWN);
   }
   
   public boolean giveReminders(){
      return this.giveReminders;
   }
   
   public long getTicksSinceArchetypeChange(){
      return this.ticksSinceArchetypeChange;
   }
   
   public void setSubarchetype(ServerPlayer player, SubArchetype subarchetype){
      if(!player.getUUID().equals(playerID)) return;
      this.subArchetype = subarchetype;
      metamorph(null, player);
      this.calculateAbilities();
      resetDeathReductionSizeLevel(player);
      resetVibrationSystem(player);
   }
   
   public void changeDeathReductionSizeLevel(ServerPlayer player, boolean decrease){
      if(!player.getUUID().equals(playerID)) return;
      if(this.deathReductionSizeLevel > 0 && decrease){
         this.deathReductionSizeLevel--;
      }else if(!decrease){
         this.deathReductionSizeLevel++;
      }else{
         return;
      }
      
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, archetypesId("death_reduction_size_level"), true);
      MinecraftUtils.attributeEffect(player, Attributes.SCALE, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, archetypesId("death_reduction_size_level"), true);
      
      double scale = -(1 - Math.pow(0.5, this.deathReductionSizeLevel));
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH, scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, archetypesId("death_reduction_size_level"), false);
      MinecraftUtils.attributeEffect(player, Attributes.SCALE, scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, archetypesId("death_reduction_size_level"), false);
      player.setHealth(player.getMaxHealth());
   }
   
   
   public void resetDeathReductionSizeLevel(ServerPlayer player){
      if(!player.getUUID().equals(playerID)) return;
      this.deathReductionSizeLevel = 0;
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, archetypesId("death_reduction_size_level"), true);
      MinecraftUtils.attributeEffect(player, Attributes.SCALE, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, archetypesId("death_reduction_size_level"), true);
   }
   
   public void setAbilityCooldown(ArchetypeAbility ability, int ticks){
      this.abilityCooldowns.put(ability, new CooldownEntry(ticks));
   }
   
   public void playerJoin(ServerPlayer player){
      this.username = player.getScoreboardName();
      calculateAbilities();
      resetVibrationSystem(player);
   }
   
   public void tick(ServerPlayer player){
      if(!player.getUUID().equals(playerID)) return;
      if(username.isBlank()){
         username = player.getScoreboardName();
      }
      abilityCooldowns.forEach((ability, cooldown) -> cooldown.tick());
      
      handleGlider(player);
      handleHover(player);
      handleFortify(player);
      handleLeap(player);
      handleMetamorph(player);
      
      if(this.giveItemsCooldown > 0) this.giveItemsCooldown--;
      
      if(CONFIG.getBoolean(ArchetypeRegistry.CAN_ALWAYS_CHANGE_ARCHETYPE) && !this.canChangeArchetype()){
         this.archetypeChangesAllowed++;
      }
      
      ticksSinceArchetypeChange++;
   }
   
   private void handleMetamorph(ServerPlayer player){
      if(this.activeMetamorph == null) return;
      if(this.metamorphTime > 0){
         this.metamorphTime--;
         if(this.metamorphTime <= 0 && this.metamorphFuseTime <= 0){
            metamorph(null, player);
            return;
         }
      }
      
      ItemStack headSlot = player.getItemBySlot(EquipmentSlot.HEAD);
      if(headSlot.isEmpty()){
         ItemStack helmet = new ItemStack(ArchetypeRegistry.METAMORPH_HELMET_ITEM);
         Identifier modelId = this.activeMetamorph.getBlock().asItem().getDefaultInstance().get(DataComponents.ITEM_MODEL);
         helmet.set(DataComponents.ITEM_MODEL, modelId);
         archetypes$ITEM_DATA.putProperty(helmet, METAMORPH_VISIBLE_MODEL, modelId.toString());
         player.setItemSlot(EquipmentSlot.HEAD, helmet);
      }else{
         ArchetypeUtils.addMetamorphHelmetTags(headSlot, this.activeMetamorph);
      }
      
      if(this.metamorphFuseTime > 0){
         if(--this.metamorphFuseTime == 0){
            this.metamorphTntShield = true;
            float explosionPower = CONFIG.getFloat(ArchetypeRegistry.METAMORPH_TNT_EXPLOSION_POWER);
            boolean damageBlocks = CONFIG.getBoolean(ArchetypeRegistry.METAMORPH_TNT_DAMAGES_BLOCKS);
            DamageSource dmgSource = player.damageSources().source(ArchetypeRegistry.METAMORPH_TNT, player, player);
            player.hurtServer(player.level(), player.damageSources().source(ArchetypeRegistry.METAMORPH_TNT_EXECUTE, player, player), Float.MAX_VALUE);
            player.level().explode(null, dmgSource, null, player.getX(), player.getY(), player.getZ(), explosionPower, false, damageBlocks ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
         }else{
            String time = TextUtils.readableDouble((double) this.metamorphFuseTime / 20.0, 1);
            ChatFormatting style = this.metamorphFuseTime % 20 > 10 ? ChatFormatting.RED : ChatFormatting.GOLD;
            MutableComponent message = Component.literal("")
                  .append(Component.object(MetamorphTypes.TNT.getSprite()))
                  .append(Component.literal(" ⚠ ").withStyle(style))
                  .append(Component.literal(time + " ").withStyle(style))
                  .append(Component.translatable("text.ancestralarchetypes.seconds").withStyle(style))
                  .append(Component.literal(" ⚠ ").withStyle(style))
                  .append(Component.object(MetamorphTypes.TNT.getSprite()));
            player.sendSystemMessage(message, true);
            
            Vec3 playerCenter = player.position().add(0, player.getBbHeight() / 2.0, 0);
            player.level().sendParticles(ParticleTypes.SMOKE, playerCenter.x, playerCenter.y, playerCenter.z, 3, 0.25, 0.25, 0.25, 0.1);
            player.level().sendParticles(ParticleTypes.FLAME, playerCenter.x, playerCenter.y, playerCenter.z, 1, 0.25, 0.25, 0.25, 0.1);
         }
         return;
      }
      
      if(this.metamorphTime % 5 == 0){
         MutableComponent blockComp = Component.object(this.activeMetamorph.getSprite());
         int maxMetamorphTime = CONFIG.getInt(ArchetypeRegistry.METAMORPH_ABILITY_DURATION);
         double percentage = (double) this.metamorphTime / maxMetamorphTime;
         TextUtils.energyBar(player, percentage, 10,
               Component.literal("").append(ArchetypeRegistry.METAMORPH.getName().withColor(getSubArchetype().getColor())).append(": ").append(blockComp).append(" "),
               Component.literal(" ").append(blockComp),
               (style) -> style.withColor(getSubArchetype().getColor()));
      }
      
      if(this.activeMetamorph == MetamorphTypes.ICE){
         double freezeRange = CONFIG.getDouble(ArchetypeRegistry.METAMORPH_ICE_FREEZE_RANGE);
         List<LivingEntity> freezers = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(freezeRange), other -> {
            if(other.isAlliedTo(player)) return false;
            if(other.getUUID().equals(playerID)) return false;
            return other.canFreeze() && !other.isDeadOrDying();
         });
         freezers.forEach(freezer -> freezer.setTicksFrozen(Math.min(freezer.getTicksRequiredToFreeze() + 20, freezer.getTicksFrozen() + 10)));
         
         int coeff = (int) ParticleEffectUtils.particleDensityCoeff(freezeRange);
         for(int i = 0; i < coeff; i++){
            Vec3 point = MathUtils.randomSpherePoint(player.position(), freezeRange);
            player.level().sendParticles(ParticleTypes.SNOWFLAKE, point.x, point.y, point.z, 1, 0, 0, 0, 0.025);
         }
         
         // TODO Add friction when it gets added
      }else if(this.activeMetamorph == MetamorphTypes.MAGMA){
         double fireRange = CONFIG.getDouble(ArchetypeRegistry.METAMORPH_MAGMA_RANGE);
         int fireTime = CONFIG.getInt(ArchetypeRegistry.METAMORPH_MAGMA_FIRE_DURATION);
         List<LivingEntity> burners = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(fireRange), other -> {
            if(other.isAlliedTo(player)) return false;
            if(other.getUUID().equals(playerID)) return false;
            return !other.fireImmune() && !other.isDeadOrDying();
         });
         burners.forEach(burner -> burner.setRemainingFireTicks(fireTime));
         
         int coeff = (int) ParticleEffectUtils.particleDensityCoeff(fireRange);
         for(int i = 0; i < coeff; i++){
            Vec3 point = MathUtils.randomSpherePoint(player.position(), fireRange);
            player.level().sendParticles(ParticleTypes.SMALL_FLAME, point.x, point.y, point.z, 1, 0, 0, 0, 0.05);
         }
      }else if(this.activeMetamorph == MetamorphTypes.BOOKSHELF && this.metamorphTime % 5 == 0){
         for(BlockPos blockPos : BlockPos.betweenClosed(player.getBoundingBox().inflate(4))){
            if(player.level().getBlockState(blockPos).is(Blocks.ENCHANTING_TABLE)){
               TrailParticleOption particle = new TrailParticleOption(Vec3.atCenterOf(blockPos), 0xfcf7ea, 60);
               player.level().sendParticles(particle, player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(), 2, 0.5, 0.5, 0.5, 0);
            }
         }
      }
   }
   
   private void handleFortify(ServerPlayer player){
      if(!player.getUUID().equals(playerID)) return;
      if(!this.fortifyActive){
         this.fortifyTime = (float) Math.min(CONFIG.getInt(ArchetypeRegistry.FORTIFY_DURATION), this.fortifyTime + CONFIG.getDouble(ArchetypeRegistry.FORTIFY_RECOVERY_TIME));
      }else{
         this.fortifyTime--;
         if((int) this.fortifyTime % 2 == 0){
            double height = player.getBbHeight() / 2 * (Math.sin(Math.PI * 2.0 / 60.0 * fortifyTime) + 1);
            ParticleEffectUtils.circle((ServerLevel) player.level(), null, player.position().add(0, height, 0), ParticleTypes.END_ROD, player.getBbWidth(), (int) (player.getBbWidth() * 12), 1, 0, 0);
         }
      }
   }
   
   private void handleLeap(ServerPlayer player){
      if(!player.getUUID().equals(playerID)) return;
      if((!player.isShiftKeyDown() || !player.onGround()) && this.leapCharge > 0) this.leapCharge = 0;
      if(this.leapCooldown > 0){
         TextUtils.energyBar(player, (double) this.leapCooldown / CONFIG.getInt(ArchetypeRegistry.LEAP_COOLDOWN), 10,
               Component.literal("\uD83E\uDD7E ").withColor(getSubArchetype().getColor()),
               Component.literal(" \uD83E\uDD7E").withColor(getSubArchetype().getColor()),
               (style) -> style.withColor(getSubArchetype().getColor()));
         
         this.leapCooldown--;
      }
      if(!hasAbility(ArchetypeRegistry.LEAP) || this.leapCooldown > 0) return;
      
      if(player.isShiftKeyDown() && player.onGround()){
         int maxLeapCharge = CONFIG.getInt(ArchetypeRegistry.LEAP_MAX_CHARGE_TIME);
         if(this.leapCharge < maxLeapCharge){
            this.leapCharge++;
         }
         
         double percentage = (double) this.leapCharge / maxLeapCharge;
         MutableComponent leapMessage = Component.literal("");
         leapMessage.append(Component.literal("\uD83E\uDD7E [").withColor(getSubArchetype().getColor()));
         for(int i = 0; i < 20; i++){
            if(percentage * 20 > i){
               leapMessage.append(Component.literal("|").withColor(getArchetype().color()));
            }else{
               leapMessage.append(Component.literal(".").withColor(getArchetype().color()));
            }
         }
         leapMessage.append(Component.literal("] \uD83E\uDD7E").withColor(getSubArchetype().getColor()));
         player.sendSystemMessage(leapMessage, true);
      }
   }
   
   private void handleGlider(ServerPlayer player){
      if(!player.getUUID().equals(playerID)) return;
      boolean wasGliderActive = this.gliderActive;
      boolean gliderEquipped = player.getItemBySlot(EquipmentSlot.CHEST).is(ArchetypeRegistry.GLIDER_ITEM) || player.getItemBySlot(EquipmentSlot.CHEST).is(ArchetypeRegistry.END_GLIDER_ITEM);
      if(gliderEquipped && player.isFallFlying()){
         if(!wasGliderActive) this.gliderActive = true;
         this.glideTime--;
      }else{
         if(wasGliderActive){
            this.gliderActive = false;
            setAbilityCooldown(ArchetypeRegistry.WING_GLIDER, CONFIG.getInt(ArchetypeRegistry.GLIDER_COOLDOWN));
            setAbilityCooldown(ArchetypeRegistry.ENDER_GLIDER, CONFIG.getInt(ArchetypeRegistry.GLIDER_COOLDOWN));
         }
         if(getAbilityCooldown(ArchetypeRegistry.WING_GLIDER) + getAbilityCooldown(ArchetypeRegistry.ENDER_GLIDER) == 0){
            this.glideTime = (float) Math.min(CONFIG.getInt(ArchetypeRegistry.GLIDER_DURATION), this.glideTime + CONFIG.getDouble(ArchetypeRegistry.GLIDER_RECOVERY_TIME));
         }
      }
      
      if(this.glideTime < 0){
         this.glideTime = 0;
         setAbilityCooldown(ArchetypeRegistry.WING_GLIDER, CONFIG.getInt(ArchetypeRegistry.GLIDER_COOLDOWN));
         setAbilityCooldown(ArchetypeRegistry.ENDER_GLIDER, CONFIG.getInt(ArchetypeRegistry.GLIDER_COOLDOWN));
      }
   }
   
   private void handleHover(ServerPlayer player){
      if(!player.getUUID().equals(playerID)) return;
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
            SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 1.25f);
            SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.HARNESS_GOGGLES_DOWN, SoundSource.PLAYERS, 0.5f, 0.8f);
            this.savedFlySpeed = player.getAbilities().getFlyingSpeed();
            player.getAbilities().setFlyingSpeed((float) (CONFIG.getDouble(ArchetypeRegistry.SLOW_HOVER_FLIGHT_SPEED)));
            ((ServerPlayer) player).connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
         }
         this.hoverTime--;
         if(player.getRandom().nextDouble() < 0.4)
            ((ServerPlayer) player).level().sendParticles(ParticleTypes.POOF, player.getX(), player.getY() - 0.5, player.getZ(), 1, 0.2, 0.2, 0.2, 0.01);
      }else{
         if(wasHovering){
            this.hoverActive = false;
            setAbilityCooldown(ArchetypeRegistry.SLOW_HOVER, CONFIG.getInt(ArchetypeRegistry.SLOW_HOVER_FLIGHT_COOLDOWN));
            SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 0.85f);
            SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.HARNESS_GOGGLES_UP, SoundSource.PLAYERS, 0.5f, 0.8f);
            player.getAbilities().setFlyingSpeed(this.savedFlySpeed);
            ((ServerPlayer) player).connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
         }
         if(getAbilityCooldown(ArchetypeRegistry.SLOW_HOVER) == 0){
            this.hoverTime = (float) Math.min(CONFIG.getInt(ArchetypeRegistry.SLOW_HOVER_FLIGHT_DURATION), this.hoverTime + CONFIG.getDouble(ArchetypeRegistry.SLOW_HOVER_FLIGHT_RECOVERY_TIME));
         }
      }
      
      if(this.hoverTime < 0){
         this.hoverTime = 0;
         setAbilityCooldown(ArchetypeRegistry.SLOW_HOVER, CONFIG.getInt(ArchetypeRegistry.SLOW_HOVER_FLIGHT_COOLDOWN));
      }
   }
   
   public void resetAbilityCooldowns(){
      abilityCooldowns.forEach((ability, cooldown) -> abilityCooldowns.put(ability, new CooldownEntry(0)));
      this.glideTime = getMaxGlideTime();
      this.hoverTime = getMaxHoverTime();
      this.fortifyTime = getMaxFortifyTime();
      this.ticksSinceArchetypeChange = CONFIG.getInt(ArchetypeRegistry.ARCHETYPE_CHANGE_COOLDOWN);
   }
   
   public void setPotionType(Pair<Item, Holder<Potion>> pair){
      this.potionBrewerStack = pair == null ? ItemStack.EMPTY : PotionContents.createItemStack(pair.getFirst(), pair.getSecond());
   }
   
   public void setMountEntity(ArchetypeAbility ability, UUID uuid){
      Pair<UUID, Float> prev = this.mountData.get(ability);
      if(prev != null){
         this.mountData.put(ability, new Pair<>(uuid, prev.getSecond()));
      }else{
         this.mountData.put(ability, new Pair<>(uuid, 0f));
      }
   }
   
   public void setMountHealth(ArchetypeAbility ability, float health){
      Pair<UUID, Float> prev = this.mountData.get(ability);
      if(prev != null){
         this.mountData.put(ability, new Pair<>(prev.getFirst(), health));
      }else{
         this.mountData.put(ability, new Pair<>(null, health));
      }
   }
   
   public void setMountName(String name){
      this.mountName = name;
   }
   
   public void setHorseVariant(Variant color, Markings marking){
      this.horseMarking = marking;
      this.horseColor = color;
   }
   
   public void setGliderColor(int color){
      this.gliderColor = color;
   }
   
   public void setHelmetColor(int color){
      this.helmetColor = color;
   }
   
   public void setGliderTrimMaterial(Holder<TrimMaterial> material){
      this.gliderTrimMaterial = material;
   }
   
   public void setHelmetTrimMaterial(Holder<TrimMaterial> material){
      this.helmetTrimMaterial = material;
   }
   
   public int getLeapCharge(){
      return leapCharge;
   }
   
   public void leap(ServerPlayer player){
      double maxCharge = CONFIG.getInt(ArchetypeRegistry.LEAP_MAX_CHARGE_TIME);
      double leapPercentage = this.leapCharge / maxCharge;
      double leapMod = CONFIG.getDouble(ArchetypeRegistry.LEAP_JUMP_POWER_MODIFIER);
      double velocity = leapMod * (1.0 + leapPercentage * 0.75);
      double minVert = 15;
      double maxVert = 90;
      
      Vec2 lookAngles = player.getRotationVector();
      double newVert = (maxVert - minVert) / maxVert * lookAngles.x + minVert;
      Vec3 newVel = Vec3.directionFromRotation((float) newVert, lookAngles.y).scale(velocity);
      player.setDeltaMovement(newVel);
      player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), newVel));
      
      SoundUtils.playSound(player.level(), player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, (float) (0.25f + leapPercentage * 0.5f), 1.5f);
      player.level().sendParticles(ParticleTypes.POOF, player.getX(), player.getY(), player.getZ(), 5, 0.15, 0.15, 0.15, 0.05);
      
      this.leapCharge = 0;
      this.leapCooldown = CONFIG.getInt(ArchetypeRegistry.LEAP_COOLDOWN);
      player.setIgnoreFallDamageFromCurrentImpulse(true, player.isIgnoringFallDamageFromCurrentImpulse() && player.currentImpulseImpactPos.y <= player.position().y ? player.currentImpulseImpactPos : player.position());
   }
   
   public void changeArchetype(ServerPlayer player, SubArchetype archetype, boolean admin){
      if(!player.getUUID().equals(playerID)) return;
      setSubarchetype(player, archetype);
      this.giveItemsCooldown = 0;
      giveAbilityItems(player, true);
      if(!admin) this.archetypeChangesAllowed = Math.max(0, this.archetypeChangesAllowed - 1);
      if(!admin) this.ticksSinceArchetypeChange = 0;
   }
   
   public void increaseAllowedChanges(int num){
      this.archetypeChangesAllowed = Math.max(0, this.archetypeChangesAllowed + num);
   }
   
   public void setReminders(boolean reminders){
      this.giveReminders = reminders;
   }
   
   public void setHealthUpdate(float health){
      if(health >= 0) this.healthUpdate = health;
   }
   
   public boolean giveAbilityItems(ServerPlayer player, boolean shortCooldown){
      if(!player.getUUID().equals(playerID)) return false;
      if(this.giveItemsCooldown > 0) return false;
      
      Set<ArchetypeAbility> abilities = getAbilities();
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
   
   public float getHealthUpdate(){
      return healthUpdate;
   }
   
   public void resetVibrationSystem(ServerPlayer player){
      if(hasAbility(ArchetypeRegistry.ECHOLOCATION)){
         if(this.vibrationSystem == null){
            this.vibrationSystem = new EcholocationVibrationSystem(player);
         }else{
            this.vibrationSystem.updatePlayer(player);
         }
      }else if(this.vibrationSystem != null){
         this.vibrationSystem.unregister();
         this.vibrationSystem = null;
      }
   }
   
   public EcholocationVibrationSystem getVibrationSystem(){
      return vibrationSystem;
   }
   
   private static class CooldownEntry {
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
