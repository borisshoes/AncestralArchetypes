package net.borisshoes.ancestralarchetypes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.ladysnake.pal.VanillaAbilities;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.datastorage.DataKey;
import net.borisshoes.borislib.datastorage.DataRegistry;
import net.borisshoes.borislib.utils.CodecUtils;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.ParticleEffectUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

import java.util.*;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.CONFIG;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.ITEMS;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.SLOW_HOVER_ABILITY;

public class PlayerArchetypeData {
   
   private static final Codec<CooldownEntry> COOLDOWN_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
         Codec.INT.fieldOf("cooldown").forGetter(CooldownEntry::getCooldown),
         Codec.INT.fieldOf("duration").forGetter(CooldownEntry::getTotalDuration)
   ).apply(instance, CooldownEntry::new));
   
   private static final Codec<Tuple<UUID, Float>> MOUNT_DATA_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
         CodecUtils.UUID_CODEC.optionalFieldOf("id").forGetter(t -> Optional.ofNullable(t.getA())),
         Codec.FLOAT.optionalFieldOf("hp", 1f).forGetter(Tuple::getB)
   ).apply(instance, (uuid, hp) -> new Tuple<>(uuid.orElse(null), hp)));
   
   // First part of the data
   private record CodecPart1(UUID playerID, String username, boolean giveReminders, boolean gliderActive, boolean hoverActive, boolean fortifyActive, int deathReductionSizeLevel, float glideTime,
         float hoverTime, float fortifyTime, float savedFlySpeed, int fungusBoostTime, int gliderColor, int helmetColor, String gliderTrimMaterial, String helmetTrimMaterial
   ){}
   
   // Second part of the data
   private record CodecPart2(int archetypeChangesAllowed, int giveItemsCooldown, float healthUpdate, ItemStack potionBrewerStack, int horseMarking, int horseColor, String mountName,
         String subArchetype, Map<String, CooldownEntry> cooldowns, Map<String, Tuple<UUID, Float>> mountData, List<ItemStackWithSlot> mountInventory, List<ItemStackWithSlot> backpackInventory
   ){}
   
   private static final MapCodec<CodecPart1> CODEC_PART1 = RecordCodecBuilder.mapCodec(instance -> instance.group(
         CodecUtils.UUID_CODEC.fieldOf("playerID").forGetter(CodecPart1::playerID),
         Codec.STRING.optionalFieldOf("username", "").forGetter(CodecPart1::username),
         Codec.BOOL.optionalFieldOf("giveReminders", CONFIG.getBoolean(ArchetypeRegistry.REMINDERS_ON_BY_DEFAULT)).forGetter(CodecPart1::giveReminders),
         Codec.BOOL.optionalFieldOf("gliderActive", false).forGetter(CodecPart1::gliderActive),
         Codec.BOOL.optionalFieldOf("hoverActive", false).forGetter(CodecPart1::hoverActive),
         Codec.BOOL.optionalFieldOf("fortifyActive", false).forGetter(CodecPart1::fortifyActive),
         Codec.INT.optionalFieldOf("deathReductionSizeLevel", 0).forGetter(CodecPart1::deathReductionSizeLevel),
         Codec.FLOAT.optionalFieldOf("glideTime", 0f).forGetter(CodecPart1::glideTime),
         Codec.FLOAT.optionalFieldOf("hoverTime", 0f).forGetter(CodecPart1::hoverTime),
         Codec.FLOAT.optionalFieldOf("fortifyTime", 0f).forGetter(CodecPart1::fortifyTime),
         Codec.FLOAT.optionalFieldOf("savedFlySpeed", 0.05f).forGetter(CodecPart1::savedFlySpeed),
         Codec.INT.optionalFieldOf("fungusBoostTime", 0).forGetter(CodecPart1::fungusBoostTime),
         Codec.INT.optionalFieldOf("gliderColor", 0xFFFFFF).forGetter(CodecPart1::gliderColor),
         Codec.INT.optionalFieldOf("helmetColor", 0xA06540).forGetter(CodecPart1::helmetColor),
         Codec.STRING.optionalFieldOf("gliderTrimMaterial", "").forGetter(CodecPart1::gliderTrimMaterial),
         Codec.STRING.optionalFieldOf("helmetTrimMaterial", "").forGetter(CodecPart1::helmetTrimMaterial)
   ).apply(instance, CodecPart1::new));
   
   private static final MapCodec<CodecPart2> CODEC_PART2 = RecordCodecBuilder.mapCodec(instance -> instance.group(
         Codec.INT.optionalFieldOf("archetypeChangesAllowed", 0).forGetter(CodecPart2::archetypeChangesAllowed),
         Codec.INT.optionalFieldOf("giveItemsCooldown", 0).forGetter(CodecPart2::giveItemsCooldown),
         Codec.FLOAT.optionalFieldOf("loginHealth", 20f).forGetter(CodecPart2::healthUpdate),
         ItemStack.CODEC.optionalFieldOf("potionBrewerStack", ItemStack.EMPTY).forGetter(CodecPart2::potionBrewerStack),
         Codec.INT.optionalFieldOf("horseMarking", 0).forGetter(CodecPart2::horseMarking),
         Codec.INT.optionalFieldOf("horseColor", 0).forGetter(CodecPart2::horseColor),
         Codec.STRING.optionalFieldOf("mountName", "").forGetter(CodecPart2::mountName),
         Codec.STRING.optionalFieldOf("subArchetype", "").forGetter(CodecPart2::subArchetype),
         Codec.unboundedMap(Codec.STRING, COOLDOWN_ENTRY_CODEC).optionalFieldOf("cooldowns", Collections.emptyMap()).forGetter(CodecPart2::cooldowns),
         Codec.unboundedMap(Codec.STRING, MOUNT_DATA_ENTRY_CODEC).optionalFieldOf("mountData", Collections.emptyMap()).forGetter(CodecPart2::mountData),
         ItemStackWithSlot.CODEC.listOf().optionalFieldOf("mountInventory", Collections.emptyList()).forGetter(CodecPart2::mountInventory),
         ItemStackWithSlot.CODEC.listOf().optionalFieldOf("backpackInventory", Collections.emptyList()).forGetter(CodecPart2::backpackInventory)
   ).apply(instance, CodecPart2::new));
   
   
   private CodecPart1 toCodecPart1(){
      return new CodecPart1(playerID, username, giveReminders, gliderActive, hoverActive, fortifyActive, deathReductionSizeLevel, glideTime, hoverTime, fortifyTime, savedFlySpeed, fungusBoostTime, gliderColor, helmetColor, gliderTrimMaterial == null ? "" : gliderTrimMaterial.getRegisteredName(), helmetTrimMaterial == null ? "" : helmetTrimMaterial.getRegisteredName());
   }
   
   private CodecPart2 toCodecPart2(){
      Map<String, CooldownEntry> cooldownMap = new HashMap<>();
      abilityCooldowns.forEach((ability, entry) -> cooldownMap.put(ability.id(), entry));
      
      Map<String, Tuple<UUID, Float>> mountDataMap = new HashMap<>();
      mountData.forEach((ability, entry) -> mountDataMap.put(ability.id(), entry));
      
      List<ItemStackWithSlot> mountInvList = new ArrayList<>();
      for(int i = 0; i < mountInventory.getItems().size(); ++i){
         ItemStack itemStack = mountInventory.getItems().get(i);
         if(!itemStack.isEmpty()) mountInvList.add(new ItemStackWithSlot(i, itemStack));
      }
      
      List<ItemStackWithSlot> backpackInvList = new ArrayList<>();
      for(int i = 0; i < backpackInventory.getItems().size(); ++i){
         ItemStack itemStack = backpackInventory.getItems().get(i);
         if(!itemStack.isEmpty()) backpackInvList.add(new ItemStackWithSlot(i, itemStack));
      }
      
      return new CodecPart2(archetypeChangesAllowed, giveItemsCooldown, healthUpdate, potionBrewerStack, horseMarking.getId(), horseColor.getId(), mountName == null ? "" : mountName, subArchetype != null ? subArchetype.getId() : "", cooldownMap, mountDataMap, mountInvList, backpackInvList);
   }
   
   public static final Codec<PlayerArchetypeData> CODEC = RecordCodecBuilder.<PlayerArchetypeData>mapCodec(instance -> instance.group(
         CODEC_PART1.forGetter(PlayerArchetypeData::toCodecPart1),
         CODEC_PART2.forGetter(PlayerArchetypeData::toCodecPart2)
   ).apply(instance, PlayerArchetypeData::new)).codec();
   
   public static final DataKey<PlayerArchetypeData> KEY = DataRegistry.register(DataKey.ofPlayer(Identifier.fromNamespaceAndPath(MOD_ID, "playerdata"), CODEC,PlayerArchetypeData::new));
   
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
   
   private PlayerArchetypeData(CodecPart1 part1, CodecPart2 part2){
      this.playerID = part1.playerID();
      this.username = part1.username();
      this.giveReminders = part1.giveReminders();
      this.gliderActive = part1.gliderActive();
      this.hoverActive = part1.hoverActive();
      this.fortifyActive = part1.fortifyActive();
      this.deathReductionSizeLevel = part1.deathReductionSizeLevel();
      this.glideTime = part1.glideTime();
      this.hoverTime = part1.hoverTime();
      this.fortifyTime = part1.fortifyTime();
      this.savedFlySpeed = part1.savedFlySpeed();
      this.fungusBoostTime = part1.fungusBoostTime();
      this.gliderColor = part1.gliderColor();
      this.helmetColor = part1.helmetColor();
      
      // Resolve trim materials from registry (requires server to be available)
      // Wrapped in try-catch to prevent codec failures from registry lookups
      try{
         if(BorisLib.SERVER != null && !part1.gliderTrimMaterial().isEmpty()){
            this.gliderTrimMaterial = BorisLib.SERVER.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL)
                  .get(Identifier.parse(part1.gliderTrimMaterial())).orElse(null);
         }
      }catch(Exception e){
         AncestralArchetypes.LOGGER.warn("Failed to resolve glider trim material '{}' for player {}: {}", part1.gliderTrimMaterial(), part1.playerID(), e.getMessage());
         this.gliderTrimMaterial = null;
      }
      
      try{
         if(BorisLib.SERVER != null && !part1.helmetTrimMaterial().isEmpty()){
            this.helmetTrimMaterial = BorisLib.SERVER.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL)
                  .get(Identifier.parse(part1.helmetTrimMaterial())).orElse(null);
         }
      }catch(Exception e){
         AncestralArchetypes.LOGGER.warn("Failed to resolve helmet trim material '{}' for player {}: {}", part1.helmetTrimMaterial(), part1.playerID(), e.getMessage());
         this.helmetTrimMaterial = null;
      }
      
      this.archetypeChangesAllowed = part2.archetypeChangesAllowed();
      this.giveItemsCooldown = part2.giveItemsCooldown();
      this.healthUpdate = part2.healthUpdate();
      this.potionBrewerStack = part2.potionBrewerStack();
      this.horseMarking = Markings.byId(part2.horseMarking());
      this.horseColor = Variant.byId(part2.horseColor());
      this.mountName = part2.mountName().isEmpty() ? null : part2.mountName();
      
      // Resolve subarchetype from registry
      // Wrapped in try-catch to prevent codec failures from invalid identifiers
      try{
         if(!part2.subArchetype().isEmpty()){
            this.subArchetype = ArchetypeRegistry.SUBARCHETYPES.getValue(Identifier.fromNamespaceAndPath(MOD_ID, part2.subArchetype()));
         }
      }catch(Exception e){
         AncestralArchetypes.LOGGER.warn("Failed to resolve subarchetype '{}' for player {}: {}", part2.subArchetype(), part1.playerID(), e.getMessage());
         this.subArchetype = null;
      }
      this.calculateAbilities();
      
      // Parse cooldowns from map
      // Wrapped in try-catch to prevent codec failures from invalid ability identifiers
      part2.cooldowns().forEach((key, entry) -> {
         try{
            ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.getValue(Identifier.fromNamespaceAndPath(MOD_ID, key));
            if(ability != null){
               this.abilityCooldowns.put(ability, entry);
            }else{
               AncestralArchetypes.LOGGER.warn("Failed to find ability '{}' when loading cooldowns for player {}", key, part1.playerID());
            }
         }catch(Exception e){
            AncestralArchetypes.LOGGER.warn("Failed to parse ability '{}' when loading cooldowns for player {}: {}", key, part1.playerID(), e.getMessage());
         }
      });
      
      // Parse mount data from map
      // Wrapped in try-catch to prevent codec failures from invalid ability identifiers
      part2.mountData().forEach((key, entry) -> {
         try{
            ArchetypeAbility ability = ArchetypeRegistry.ABILITIES.getValue(Identifier.fromNamespaceAndPath(MOD_ID, key));
            if(ability != null){
               this.mountData.put(ability, entry);
            }else{
               AncestralArchetypes.LOGGER.warn("Failed to find ability '{}' when loading mount data for player {}", key, part1.playerID());
            }
         }catch(Exception e){
            AncestralArchetypes.LOGGER.warn("Failed to parse ability '{}' when loading mount data for player {}: {}", key, part1.playerID(), e.getMessage());
         }
      });
      
      // Parse mount inventory
      // Wrapped in try-catch to prevent codec failures from corrupted inventory data
      try{
         for(ItemStackWithSlot stackWithSlot : part2.mountInventory()){
            if(stackWithSlot.isValidInContainer(this.mountInventory.getItems().size())){
               this.mountInventory.getItems().set(stackWithSlot.slot(), stackWithSlot.stack());
            }
         }
      }catch(Exception e){
         AncestralArchetypes.LOGGER.warn("Failed to load mount inventory for player {}: {}", part1.playerID(), e.getMessage());
      }
      
      // Parse backpack inventory
      // Wrapped in try-catch to prevent codec failures from corrupted inventory data
      try{
         for(ItemStackWithSlot stackWithSlot : part2.backpackInventory()){
            if(stackWithSlot.isValidInContainer(this.backpackInventory.getItems().size())){
               this.backpackInventory.getItems().set(stackWithSlot.slot(), stackWithSlot.stack());
            }
         }
      }catch(Exception e){
         AncestralArchetypes.LOGGER.warn("Failed to load backpack inventory for player {}: {}", part1.playerID(), e.getMessage());
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
      return this.archetypeChangesAllowed > 0;
   }
   
   public boolean giveReminders(){
      return this.giveReminders;
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
