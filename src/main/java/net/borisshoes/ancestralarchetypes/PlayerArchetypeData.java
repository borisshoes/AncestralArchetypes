package net.borisshoes.ancestralarchetypes;

import io.github.ladysnake.pal.VanillaAbilities;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.datastorage.DataKey;
import net.borisshoes.borislib.datastorage.DataRegistry;
import net.borisshoes.borislib.datastorage.StorableData;
import net.borisshoes.borislib.utils.AlgoUtils;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.ParticleEffectUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.storage.ValueInput;

import java.util.*;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.ITEMS;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.SLOW_HOVER_ABILITY;

public class PlayerArchetypeData implements StorableData {
   
   public static final DataKey<PlayerArchetypeData> KEY = DataRegistry.register(DataKey.ofPlayer(Identifier.fromNamespaceAndPath(MOD_ID, "playerdata"), PlayerArchetypeData::new));
   
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
   private float savedFlySpeed = 0.05f;
   private int fungusBoostTime;
   private int gliderColor = 0xFFFFFF;
   private int helmetColor = 0xA06540;
   private long ticksSinceArchetypeChange = CONFIG.getInt(ArchetypeRegistry.ARCHETYPE_CHANGE_COOLDOWN);
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
   private final HashMap<ArchetypeAbility, CooldownEntry> abilityCooldowns = new HashMap<>();
   private final HashMap<ArchetypeAbility, Tuple<UUID, Float>> mountData = new HashMap<>();
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
      this.fungusBoostTime = view.getIntOr("fungusBoostTime", 0);
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
            this.subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID, subArchetypeStr));
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
            ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.getValue(Identifier.fromNamespaceAndPath(MOD_ID, key));
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
            ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.getValue(Identifier.fromNamespaceAndPath(MOD_ID, key));
            if(ability != null){
               CompoundTag entryTag = mountDataTag.getCompound(key).orElse(new CompoundTag());
               UUID uuid = AlgoUtils.getUUID(entryTag.getStringOr("id", ""));
               mountData.put(ability, new Tuple<>(uuid, entryTag.getFloatOr("hp", 1f)));
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
      tag.putInt("fungusBoostTime", fungusBoostTime);
      tag.putInt("gliderColor", gliderColor);
      tag.putInt("helmetColor", helmetColor);
      tag.putInt("archetypeChangesAllowed", archetypeChangesAllowed);
      tag.putInt("giveItemsCooldown", giveItemsCooldown);
      tag.putFloat("loginHealth", healthUpdate);
      tag.putInt("horseMarking", horseMarking.getId());
      tag.putInt("horseColor", horseColor.getId());
      tag.putLong("ticksSinceArchetypeChange", ticksSinceArchetypeChange);
      
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
         tagEntry.putString("id", pair.getA() != null ? pair.getA().toString() : "");
         tagEntry.putFloat("hp", pair.getB());
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
      abilities.addAll(Arrays.asList(this.subArchetype.getAbilities()));
      abilities.addAll(Arrays.asList(this.subArchetype.getArchetype().getAbilities()));
      abilities.removeIf(a1 -> abilities.stream().anyMatch(a2 -> a2.overrides(a1)));
   }
   
   /**
    * Copies all data from an old IArchetypeProfile into this PlayerArchetypeData.
    * Used for migrating from the old CCA-based storage to the new system.
    *
    * @param oldData The old profile to copy data from
    */
   public void copyFrom(IArchetypeProfile oldData, ServerPlayer player){
      if(oldData == null || !oldData.hasData()) return;
      this.username = player.getScoreboardName();
      
      // Copy subarchetype and recalculate abilities
      this.subArchetype = oldData.getSubArchetype();
      this.calculateAbilities();
      
      // Copy basic state
      this.giveReminders = oldData.giveReminders();
      this.deathReductionSizeLevel = oldData.getDeathReductionSizeLevel();
      this.glideTime = oldData.getGlideTime();
      this.hoverTime = oldData.getHoverTime();
      this.fortifyTime = oldData.getFortifyTime();
      this.fortifyActive = oldData.isFortifyActive();
      this.healthUpdate = oldData.getHealthUpdate();
      
      // Copy customization
      this.gliderColor = oldData.getGliderColor();
      this.helmetColor = oldData.getHelmetColor();
      this.gliderTrimMaterial = oldData.getGliderTrimMaterial();
      this.helmetTrimMaterial = oldData.getHelmetTrimMaterial();
      
      // Copy horse/mount settings
      this.horseMarking = oldData.getHorseMarking();
      this.horseColor = oldData.getHorseColor();
      this.mountName = oldData.getMountName();
      
      // Copy potion brewer stack
      this.potionBrewerStack = oldData.getPotionStack();
      
      // Copy archetype changes allowed (inverse of canChangeArchetype check)
      // If they can change, they have at least 1 remaining
      this.archetypeChangesAllowed = oldData.canChangeArchetype() ? 1 : 0;
      
      // Copy ability cooldowns
      for(ArchetypeAbility ability : oldData.getAbilities()){
         int cooldown = oldData.getAbilityCooldown(ability);
         if(cooldown > 0){
            this.abilityCooldowns.put(ability, new CooldownEntry(cooldown));
         }
      }
      
      // Copy mount data (UUID and health for each mount ability)
      for(ArchetypeAbility ability : oldData.getAbilities()){
         UUID mountId = oldData.getMountEntity(ability);
         float mountHealth = oldData.getMountHealth(ability);
         if(mountId != null || mountHealth > 0){
            this.mountData.put(ability, new Tuple<>(mountId, mountHealth));
         }
      }
      
      // Copy mount inventory
      SimpleContainer oldMountInv = oldData.getMountInventory();
      for(int i = 0; i < Math.min(oldMountInv.getItems().size(), this.mountInventory.getItems().size()); i++){
         ItemStack stack = oldMountInv.getItem(i);
         if(!stack.isEmpty()){
            this.mountInventory.setItem(i, stack.copy());
         }
      }
      
      // Copy backpack inventory
      SimpleContainer oldBackpackInv = oldData.getBackpackInventory();
      for(int i = 0; i < Math.min(oldBackpackInv.getItems().size(), this.backpackInventory.getItems().size()); i++){
         ItemStack stack = oldBackpackInv.getItem(i);
         if(!stack.isEmpty()){
            this.backpackInventory.setItem(i, stack.copy());
         }
      }
   }
   
   public List<ArchetypeAbility> getAbilities(){
      return new ArrayList<>(this.abilities);
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
   
   public boolean isFungusBoosted(){
      return this.fungusBoostTime > 0;
   }
   
   public void fungusBoost(){
      this.fungusBoostTime = CONFIG.getInt(ArchetypeRegistry.FUNGUS_SPEED_BOOST_DURATION);
   }
   
   public UUID getMountEntity(ArchetypeAbility ability){
      Tuple<UUID, Float> data = this.mountData.get(ability);
      if(data != null){
         return data.getA();
      }else{
         return null;
      }
   }
   
   public float getMountHealth(ArchetypeAbility ability){
      Tuple<UUID, Float> data = this.mountData.get(ability);
      if(data != null){
         return data.getB();
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
      this.calculateAbilities();
      resetDeathReductionSizeLevel(player);
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
      
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID, "death_reduction_size_level"), true);
      MinecraftUtils.attributeEffect(player, Attributes.SCALE, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID, "death_reduction_size_level"), true);
      
      double scale = -(1 - Math.pow(0.5, this.deathReductionSizeLevel));
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH, scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID, "death_reduction_size_level"), false);
      MinecraftUtils.attributeEffect(player, Attributes.SCALE, scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID, "death_reduction_size_level"), false);
      player.setHealth(player.getMaxHealth());
   }
   
   
   public void resetDeathReductionSizeLevel(ServerPlayer player){
      if(!player.getUUID().equals(playerID)) return;
      this.deathReductionSizeLevel = 0;
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID, "death_reduction_size_level"), true);
      MinecraftUtils.attributeEffect(player, Attributes.SCALE, 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID, "death_reduction_size_level"), true);
   }
   
   public void setAbilityCooldown(ArchetypeAbility ability, int ticks){
      this.abilityCooldowns.put(ability, new CooldownEntry(ticks));
   }
   
   public void playerJoin(ServerPlayer player){
      this.username = player.getScoreboardName();
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
      
      if(this.fungusBoostTime > 0){
         this.fungusBoostTime--;
         player.level().sendParticles(new DustParticleOptions(0x20c7b1, 0.75f), player.position().x(), player.position().y() + player.getBbHeight() / 2.0, player.position().z(), 1, player.getBbWidth() * 0.65, player.getBbHeight() / 2.0, player.getBbWidth() * 0.65, 1);
      }
      if(this.giveItemsCooldown > 0) this.giveItemsCooldown--;
      
      if(CONFIG.getBoolean(ArchetypeRegistry.CAN_ALWAYS_CHANGE_ARCHETYPE) && !this.canChangeArchetype()){
         this.archetypeChangesAllowed++;
      }
      
      ticksSinceArchetypeChange++;
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
   
   public void setPotionType(Tuple<Item, Holder<Potion>> pair){
      this.potionBrewerStack = pair == null ? ItemStack.EMPTY : PotionContents.createItemStack(pair.getA(), pair.getB());
   }
   
   public void setMountEntity(ArchetypeAbility ability, UUID uuid){
      Tuple<UUID, Float> prev = this.mountData.get(ability);
      if(prev != null){
         this.mountData.put(ability, new Tuple<>(uuid, prev.getB()));
      }else{
         this.mountData.put(ability, new Tuple<>(uuid, 0f));
      }
   }
   
   public void setMountHealth(ArchetypeAbility ability, float health){
      Tuple<UUID, Float> prev = this.mountData.get(ability);
      if(prev != null){
         this.mountData.put(ability, new Tuple<>(prev.getA(), health));
      }else{
         this.mountData.put(ability, new Tuple<>(null, health));
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
   
   public void changeArchetype(ServerPlayer player, SubArchetype archetype){
      if(!player.getUUID().equals(playerID)) return;
      setSubarchetype(player, archetype);
      this.giveItemsCooldown = 0;
      giveAbilityItems(player, true);
      this.archetypeChangesAllowed = Math.max(0, this.archetypeChangesAllowed - 1);
      this.ticksSinceArchetypeChange = 0;
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
   
   public float getHealthUpdate(){
      return healthUpdate;
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
