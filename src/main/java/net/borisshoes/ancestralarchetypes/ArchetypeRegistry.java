package net.borisshoes.ancestralarchetypes;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Lifecycle;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.PolymerCreativeModeTabUtils;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import io.github.ladysnake.pal.AbilitySource;
import io.github.ladysnake.pal.Pal;
import net.borisshoes.ancestralarchetypes.callbacks.GlowBerryShieldLoginCallback;
import net.borisshoes.ancestralarchetypes.callbacks.MetamorphTNTShieldLoginCallback;
import net.borisshoes.ancestralarchetypes.callbacks.WaxShieldLoginCallback;
import net.borisshoes.ancestralarchetypes.entities.CreakingHeartEntity;
import net.borisshoes.ancestralarchetypes.entities.LevitationBulletEntity;
import net.borisshoes.ancestralarchetypes.entities.SnowblastEntity;
import net.borisshoes.ancestralarchetypes.items.*;
import net.borisshoes.ancestralarchetypes.misc.ArchetypeUtils;
import net.borisshoes.ancestralarchetypes.misc.MetamorphTypes;
import net.borisshoes.borislib.BorisLib;
import net.borisshoes.borislib.callbacks.LoginCallback;
import net.borisshoes.borislib.config.ConfigSetting;
import net.borisshoes.borislib.config.IConfigSetting;
import net.borisshoes.borislib.config.values.BooleanConfigValue;
import net.borisshoes.borislib.config.values.DoubleConfigValue;
import net.borisshoes.borislib.config.values.IntConfigValue;
import net.borisshoes.borislib.gui.GraphicalItem;
import net.borisshoes.borislib.utils.TextUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.biome.Biome;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;
import static net.borisshoes.ancestralarchetypes.items.MetamorphHeadItem.METAMORPH_HELMET_TYPE;
import static net.borisshoes.borislib.BorisLib.registerGraphicItem;
import static net.borisshoes.borislib.utils.MinecraftUtils.makeEnchantComponent;

public class ArchetypeRegistry {
   public static final Registry<ArchetypeAbility> ABILITIES = new MappedRegistry<>(ResourceKey.createRegistryKey(archetypesId("ability")), Lifecycle.stable());
   public static final Registry<Archetype> ARCHETYPES = new MappedRegistry<>(ResourceKey.createRegistryKey(archetypesId("archetype")), Lifecycle.stable());
   public static final Registry<SubArchetype> SUBARCHETYPES = new MappedRegistry<>(ResourceKey.createRegistryKey(archetypesId("subarchetype")), Lifecycle.stable());
   public static final Registry<Item> ITEMS = new MappedRegistry<>(ResourceKey.createRegistryKey(archetypesId("item")), Lifecycle.stable());
   public static final Registry<IConfigSetting<?>> CONFIG_SETTINGS = new MappedRegistry<>(ResourceKey.createRegistryKey(archetypesId("config_settings")), Lifecycle.stable());
   public static final HashMap<Item, Pair<Float, Integer>> TUFF_FOODS = new HashMap<>();
   public static final HashMap<Item, Pair<Float, Integer>> COPPER_FOODS = new HashMap<>();
   public static final HashMap<Item, Pair<Float, Integer>> IRON_FOODS = new HashMap<>();
   public static final HashMap<Item, ResourceKey<Item>> ABILITY_ITEM_KEY_MAP = new HashMap<>();
   
   public static final TagKey<Item> CHOCOLATE_ALLERGY_FOODS = TagKey.create(Registries.ITEM, archetypesId("chocolate_allergy_foods"));
   public static final TagKey<Item> CARNIVORE_FOODS = TagKey.create(Registries.ITEM, archetypesId("carnivore_foods"));
   public static final TagKey<Item> SLIME_GROW_ITEMS = TagKey.create(Registries.ITEM, archetypesId("slime_grow_items"));
   public static final TagKey<Item> MAGMA_CUBE_GROW_ITEMS = TagKey.create(Registries.ITEM, archetypesId("magma_cube_grow_items"));
   public static final TagKey<Item> SULFUR_GROW_ITEMS = TagKey.create(Registries.ITEM, archetypesId("sulfur_grow_items"));
   public static final TagKey<Item> BACKPACK_DISALLOWED_ITEMS = TagKey.create(Registries.ITEM, archetypesId("backpack_disallowed_items"));
   public static final TagKey<Item> ABILITY_ITEMS = TagKey.create(Registries.ITEM, archetypesId("ability_items"));
   public static final TagKey<Biome> COLD_DAMAGE_EXCEPTION_BIOMES = TagKey.create(Registries.BIOME, archetypesId("cold_damage_exception_biomes"));
   public static final TagKey<Biome> COLD_DAMAGE_INCLUDE_BIOMES = TagKey.create(Registries.BIOME, archetypesId("cold_damage_include_biomes"));
   public static final TagKey<Biome> DRY_OUT_EXCEPTION_BIOMES = TagKey.create(Registries.BIOME, archetypesId("dry_out_exception_biomes"));
   public static final TagKey<Biome> DRY_OUT_INCLUDE_BIOMES = TagKey.create(Registries.BIOME, archetypesId("dry_out_include_biomes"));
   public static final TagKey<DamageType> NO_STARTLE = TagKey.create(Registries.DAMAGE_TYPE, archetypesId("no_startle"));
   
   public static final HashMap<MetamorphTypes, TagKey<Item>> METAMORPH_ITEMS = new HashMap<>();
   
   static{
      for(MetamorphTypes type : MetamorphTypes.values()){
         METAMORPH_ITEMS.put(type, TagKey.create(Registries.ITEM, archetypesId("metamorph_" + type.toString() + "_items")));
      }
   }
   
   public static final GraphicalItem.GraphicElement LOCKED_POTION = registerGraphicItem(new GraphicalItem.GraphicElement(archetypesId("locked_potion"), Items.POTION, false));
   public static final GraphicalItem.GraphicElement LOCKED_SPLASH_POTION = registerGraphicItem(new GraphicalItem.GraphicElement(archetypesId("locked_splash_potion"), Items.SPLASH_POTION, false));
   public static final GraphicalItem.GraphicElement LOCKED_LINGERING_POTION = registerGraphicItem(new GraphicalItem.GraphicElement(archetypesId("locked_lingering_potion"), Items.LINGERING_POTION, false));
   
   public static final EntityType<SnowblastEntity> SNOWBLAST_ENTITY = registerEntity("snowblast",
         EntityType.Builder.<SnowblastEntity>of(SnowblastEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).noLootTable().clientTrackingRange(4).updateInterval(3)
   );
   
   public static final EntityType<LevitationBulletEntity> LEVITATION_BULLET_ENTITY = registerEntity("levitation_bullet",
         EntityType.Builder.<LevitationBulletEntity>of(LevitationBulletEntity::new, MobCategory.MISC).sized(0.3125F, 0.3125F).noLootTable().clientTrackingRange(4).updateInterval(3)
   );
   
   public static final EntityType<CreakingHeartEntity> CREAKING_HEART_ENTITY = registerEntity("creaking_heart",
         EntityType.Builder.<CreakingHeartEntity>of(CreakingHeartEntity::new, MobCategory.MISC).sized(1.0f, 1.0f).noLootTable().clientTrackingRange(10).fireImmune()
   );
   
   public static final IConfigSetting<?> SPYGLASS_REVEALS_ARCHETYPE = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("spyglassRevealsArchetype", true)));
   
   public static final IConfigSetting<?> SPYGLASS_REVEAL_ALERTS_PLAYER = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("spyglassRevealAlertsPlayer", false)));
   
   public static final IConfigSetting<?> CAN_ALWAYS_CHANGE_ARCHETYPE = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("canAlwaysChangeArchetype", false)));
   
   public static final IConfigSetting<?> REMINDERS_ON_BY_DEFAULT = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("remindersOnByDefault", true)));
   
   public static final IConfigSetting<?> REDUCED_PARTICLES = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("reducedParticles", false)));
   
   public static final IConfigSetting<?> IGNORED_BY_MOB_TYPE = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("ignoredByMobType", true)));
   
   public static final IConfigSetting<?> RIDEABLE_TEAM_ONLY = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("rideableTeamOnly", true)));
   
   public static final IConfigSetting<?> LOG_COMMAND_USAGE = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("logCommandUsage", false)));
   
   public static final IConfigSetting<?> SPYGLASS_INVESTIGATE_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("spyglassInvestigateDuration", 150, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> CHANGES_PER_CHANGE_ITEM = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("changesPerChangeItem", 1, new IntConfigValue.IntLimits(0, 1000))));
   
   public static final IConfigSetting<?> ARCHETYPE_CHANGE_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("archetypeChangeCooldown", 0, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> STARTING_ARCHETYPE_CHANGES = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("startingArchetypeChanges", 1, new IntConfigValue.IntLimits(0, 1000))));
   
   public static final IConfigSetting<?> FIREBALL_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("fireballCooldown", 600, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> WIND_CHARGE_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("windChargeCooldown", 200, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> GLIDER_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("gliderDuration", 600, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> GLIDER_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("gliderCooldown", 100, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> SPIRIT_MOUNT_KILL_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("spiritMountKillCooldown", 8000, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> DAMAGE_STUN_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("damageStunDuration", 15, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> CAULDRON_INSTANT_EFFECT_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("cauldronInstantEffectCooldown", 900, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> GELATIAN_GROW_ITEM_EAT_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("gelatianGrowItemEatDuration", 500, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> WITHERING_EFFECT_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("witheringEffectDuration", 150, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> GUARDIAN_RAY_WINDUP = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("guardianRayWindup", 30, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> GUARDIAN_RAY_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("guardianRayCooldown", 600, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> GUARDIAN_RAY_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("guardianRayDuration", 85, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> VENOMOUS_POISON_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("venomousPoisonDuration", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> VENOMOUS_POISON_STRENGTH = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("venomousPoisonStrength", 1, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> MOONLIT_CAVE_SPIDER_VENOM_DURATION_PER_PHASE = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("moonlitCaveSpiderVenomDurationPerPhase", 50, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> WEAVING_WEB_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("weavingWebCooldown", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> SLOW_HOVER_FLIGHT_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("slowHoverFlightDuration", 12000, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> SLOW_HOVER_FLIGHT_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("slowHoverFlightCooldown", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> SNOW_BLAST_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("snowBlastCooldown", 500, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> SNOW_BLAST_SLOWNESS_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("snowBlastSlownessDuration", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> SNOW_BLAST_SLOWNESS_STRENGTH = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("snowBlastSlownessStrength", 3, new IntConfigValue.IntLimits(1, 10))));
   
   public static final IConfigSetting<?> LONG_TELEPORT_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("longTeleportCooldown", 600, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> RANDOM_TELEPORT_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("randomTeleportCooldown", 400, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> FORTIFY_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("fortifyDuration", 200, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> FORTIFY_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("fortifyCooldown", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> LEVITATION_BULLET_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("levitationBulletCooldown", 300, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> ENDERFLAME_FIREBALL_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("enderflameFireballCooldown", 600, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> ENDERFLAME_BUFFET_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("enderflameBuffetDuration", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> ENDERFLAME_BUFFET_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("enderflameBuffetCooldown", 800, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> LEVITATION_BULLET_COUNT = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("levitationBulletCount", 3, new IntConfigValue.IntLimits(1, 25))));
   
   public static final IConfigSetting<?> LEVITATION_BULLET_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("levitationBulletDuration", 200, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> LEVITATION_BULLET_LEVEL = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("levitationBulletLevel", 1, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> BLAZING_STRIKE_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("blazingStrikeDuration", 100, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> FUNGUS_SPEED_BOOST_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("fungusSpeedBoostDuration", 200, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> FUNGUS_SPEED_BOOST_CONSUME_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("fungusSpeedBoostConsumeDuration", 15, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> WAX_SHIELD_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("waxShieldDuration", 300, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> WAX_SHIELD_CONSUME_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("waxShieldConsumeDuration", 20, new IntConfigValue.IntLimits(1))));
   
   public static final IConfigSetting<?> CHOCOLATE_ALLERGY_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("chocolateAllergyDuration", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> CHOCOLATE_ALLERGY_AMPLIFIER = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("chocolateAllergyAmplifier", 2, new IntConfigValue.IntLimits(0, 255))));
   
   public static final IConfigSetting<?> LEAP_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("leapCooldown", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> LEAP_MAX_CHARGE_TIME = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("leapMaxChargeTime", 50, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> BIOME_ADAPTIVE_HOT_VULNERABILITY_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("biomeAdaptiveHotVulnerabilityDuration", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> BIOME_ADAPTIVE_TEMPERATE_FEEBLE_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("biomeAdaptiveTemperateFeebleDuration", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> BIOME_ADAPTIVE_COLD_SLOWNESS_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("biomeAdaptiveColdSlownessDuration", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> TONGUE_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("tongueCooldown", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> METAMORPH_EAT_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("metamorphEatDuration", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> METAMORPH_ABILITY_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("metamorphAbilityDuration", 6000, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> METAMORPH_ICE_FRICTION_REDUCTION = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphIceFrictionReduction", 0.02, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> METAMORPH_ICE_FREEZE_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphIceFreezeRange", 4.0, new DoubleConfigValue.DoubleLimits(0, 128))));
   
   public static final IConfigSetting<?> METAMORPH_ICE_DRAG_REDUCTION = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphIceDragReduction", 0.75, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> METAMORPH_WOOL_FALL_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphWoolFallModifier", 0.5, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> METAMORPH_IRON_PROJECTILE_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphIronProjectileModifier", 0.5, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> METAMORPH_IRON_BLAST_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphIronBlastModifier", 0.75, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> METAMORPH_IRON_KNOCKBACK_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphIronKnockbackModifier", 0.5, new DoubleConfigValue.DoubleLimits(0, 10))));
   
   public static final IConfigSetting<?> METAMORPH_NETHERITE_BLAST_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphNetheriteBlastModifier", 0.25, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> METAMORPH_NETHERITE_KNOCKBACK_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphNetheriteKnockbackModifier", 0.0, new DoubleConfigValue.DoubleLimits(0, 10))));
   
   public static final IConfigSetting<?> METAMORPH_TNT_EXPLOSION_POWER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphTntExplosionPower", 7.5, new DoubleConfigValue.DoubleLimits(0, 128))));
   
   public static final IConfigSetting<?> METAMORPH_TNT_DAMAGES_BLOCKS = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("metamorphTntDamagesBlocks", true)));
   
   public static final IConfigSetting<?> METAMORPH_TNT_FIRE_FUSE_TIME = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("metamorphTntFireFuseTime", 120, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> METAMORPH_TNT_DEATH_FUSE_TIME = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("metamorphTntDeathFuseTime", 60, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> METAMORPH_TNT_EXPLOSION_FUSE_TIME = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("metamorphTntExplosionFuseTime", 80, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> METAMORPH_GOLD_REGEN_RATE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphGoldRegenRate", 0.25, new DoubleConfigValue.DoubleLimits(0, 10))));
   
   public static final IConfigSetting<?> METAMORPH_MAGMA_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphMagmaRange", 2.5, new DoubleConfigValue.DoubleLimits(0, 128))));
   
   public static final IConfigSetting<?> METAMORPH_MAGMA_FIRE_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("metamorphMagmaFireDuration", 20, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> METAMORPH_TNT_DEATH_ABSORPTION_HP = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("metamorphTntDeathAbsorptionHp", 40.0, new DoubleConfigValue.DoubleLimits(0, 1024))));
   
   public static final IConfigSetting<?> SONIC_BLAST_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("sonicBlastCooldown", 500, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> SONIC_BLAST_CHARGE_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("sonicBlastChargeDuration", 100, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> BURROW_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("burrowCooldown", 600, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_RESPAWN_TIMER = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("creakingHeartRespawnTimer", 200, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_WEAKNESS_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("creakingHeartWeaknessDuration", 600, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_SLOWNESS_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("creakingHeartSlownessDuration", 200, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_COOLDOWN = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("creakingHeartCooldown", 1200, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> BERRY_EATER_EAT_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("berryEaterEatDuration", 30, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> BERRY_EATER_SWEET_REGEN_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("berryEaterSweetRegenDuration", 200, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> BERRY_EATER_SWEET_STRENGTH_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("berryEaterSweetStrengthDuration", 400, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> BERRY_EATER_GLOW_SPEED_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("berryEaterGlowSpeedDuration", 300, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> BERRY_EATER_GLOW_ABSORPTION_DURATION = registerConfigSetting(new ConfigSetting<>(
         new IntConfigValue("berryEaterGlowAbsorptionDuration", 600, new IntConfigValue.IntLimits(0))));
   
   public static final IConfigSetting<?> JUMPY_JUMP_BOOST = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("jumpyJumpBoost", 0.35, new DoubleConfigValue.DoubleLimits(-100, 100))));
   
   public static final IConfigSetting<?> CAULDRON_DRINKABLE_COOLDOWN_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("cauldronDrinkableCooldownModifier", 0.9, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> CAULDRON_THROWABLE_COOLDOWN_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("cauldronThrowableCooldownModifier", 1.5, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> SPIRIT_MOUNT_REGENERATION_RATE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("spiritMountRegenerationRate", 1.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> SNOWBALL_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("snowballDamage", 3.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> REGENERATION_RATE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("regenerationRate", 0.05, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> INSATIATBLE_HUNGER_RATE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("insatiableHungerRate", 0.25, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> PROJECTILE_RESISTANT_REDUCTION = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("projectileResistantReduction", 0.5, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> SOFT_HITTER_DAMAGE_REDUCTION = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("softhitterDamageReduction", 0.85, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> HARD_HITTER_DAMAGE_INCREASE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("hardhitterDamageModifier", 1.15, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> HARD_HITTER_KNOCKBACK_INCREASE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("hardhitterKnockbackIncrease", 0.5, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> HEALTH_SPRINT_CUTOFF = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("healthSprintCutoff", 0.33, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> KNOCKBACK_DECREASE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("knockbackReduction", 0.5, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> KNOCKBACK_INCREASE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("knockbackIncrease", 2.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> MOB_SNEAK_ATTACK_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("mobSneakAttackModifier", 2.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> PLAYER_SNEAK_ATTACK_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("playerSneakAttackModifier", 1.15, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> BIOME_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("biomeDamage", 2.5, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> FALL_DAMAGE_REDUCTION = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("fallDamageReduction", 0.5, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> COLD_DAMAGE_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("coldDamageModifier", 2.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> ADDED_STARVE_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("addedStarveDamage", 3.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> IMPALE_VULNERABLE_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("impaleVulnerableModifier", 2.5, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> SLOW_FALLER_TRIGGER_SPEED = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("slowFallerTriggerSpeed", 0.3, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> STARTLE_MIN_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("startleMinDamage", 1.1, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> TUFF_FOOD_HEALTH_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("tuffFoodHealthModifier", 1.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> TUFF_FOOD_DURATION_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("tuffFoodDurationModifier", 1.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> IRON_FOOD_HEALTH_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("ironFoodHealthModifier", 1.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> IRON_FOOD_DURATION_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("ironFoodDurationModifier", 1.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> COPPER_FOOD_HEALTH_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("copperFoodHealthModifier", 1.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> COPPER_FOOD_DURATION_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("copperFoodDurationModifier", 1.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> LONG_ARMS_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("longArmsRange", 0.5, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> MOUNTED_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("mountedRange", 1.5, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> MOONLIT_SLIME_HEALTH_PER_PHASE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("moonlitSlimeHealthPerPhase", 0.25, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> MOONLIT_SLIME_SIZE_PER_PHASE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("moonlitSlimeSizePerPhase", 0.125, new DoubleConfigValue.DoubleLimits(-1))));
   
   public static final IConfigSetting<?> SPEEDY_SPEED_BOOST = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("speedySpeedBoost", 0.25, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> SNEAKY_SPEED_BOOST = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("sneakySpeedBoost", 0.5, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> THORNY_REFLECTION_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("thornyReflectionModifier", 0.33, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> THORNY_REFLECTION_CAP = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("thornyReflectionCap", 20.0, new DoubleConfigValue.DoubleLimits())));
   
   public static final IConfigSetting<?> GUARDIAN_RAY_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("guardianRayDamage", 5.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> GREAT_SWIMMER_MOVE_SPEED_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("greatSwimmerMoveSpeedModifier", 0.25, new DoubleConfigValue.DoubleLimits(-100, 100))));
   
   public static final IConfigSetting<?> GREAT_SWIMMER_SLIPPERY_DAMAGE_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("greatSwimmerSlipperyDamageModifier", 0.8, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> SLIPPERY_DAMAGE_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("slipperyDamageModifier", 0.9, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> GLIDER_RECOVERY_TIME = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("gliderRecoveryTime", 0.1, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> HASTY_MINING_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("hastyMiningModifier", 1.9, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> HASTY_ATTACK_SPEED_INCREASE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("hastyAttackSpeedIncrease", 0.2, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> LIGHTWEIGHT_INCREASED_KNOCKBACK = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("lightweightIncreasedKnockback", 2.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> LIGHTWEIGHT_DRAG_REDUCTION = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("lightweightDragReduction", 0.35, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> BOUNCY_BOUNCINESS = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("bouncyBounciness", 0.95, new DoubleConfigValue.DoubleLimits(0, 1))));
   
   public static final IConfigSetting<?> RESILIENT_JOINTS_EXTRA_FALL_BLOCKS = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("resilientJointsExtraFallBlocks", 4.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> HURT_BY_WATER_RAIN_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("hurtByWaterRainDamage", 1.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> HURT_BY_WATER_SWIM_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("hurtByWaterSwimDamage", 4.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> SLOW_HOVER_FLIGHT_SPEED = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("slowHoverFlightSpeed", 0.015, new DoubleConfigValue.DoubleLimits(0, 10))));
   
   public static final IConfigSetting<?> SLOW_HOVER_FLIGHT_RECOVERY_TIME = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("slowHoverFlightRecoveryTime", 0.15, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> SNOW_BLAST_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("snowBlastDamage", 4.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> SHY_VIEWING_ANGLE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("shyViewingAngle", 12.5, new DoubleConfigValue.DoubleLimits(0, 180))));
   
   public static final IConfigSetting<?> SHY_NOTICING_ANGLE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("shyNoticingAngle", 100.0, new DoubleConfigValue.DoubleLimits(0, 180))));
   
   public static final IConfigSetting<?> LONG_TELEPORT_DISTANCE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("longTeleportDistance", 16.0, new DoubleConfigValue.DoubleLimits(1, 128))));
   
   public static final IConfigSetting<?> RANDOM_TELEPORT_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("randomTeleportRange", 12.0, new DoubleConfigValue.DoubleLimits(1, 128))));
   
   public static final IConfigSetting<?> FORTIFY_DAMAGE_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("fortifyDamageModifier", 0.15, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> FORTIFY_RECOVERY_TIME = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("fortifyRecoveryTime", 0.1, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> ENDERFLAME_BUFFET_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("enderflameBuffetDamage", 1.5, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> MOONLIT_CAVE_SPIDER_VENOM_STRENGTH_PER_PHASE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("moonlitCaveSpiderVenomStrengthPerPhase", 0.5, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> SNOW_BLAST_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("snowBlastRange", 5.0, new DoubleConfigValue.DoubleLimits(0))));
   
   public static final IConfigSetting<?> LEVITATION_BULLET_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("levitationBulletDamage", 4.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> LAVA_WALKER_SPEED_MULTIPLIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("lavaWalkerSpeedMultiplier", 3.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> FUNGUS_SPEED_BOOST_MULTIPLIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("fungusSpeedBoostMultiplier", 0.5, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> WAX_SHIELD_HEALTH = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("waxShieldHealth", 2.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> WAX_SHIELD_MAX_HEALTH = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("waxShieldMaxHealth", 20.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> VULNERABLE_IN_WATER_VULNERABILITY = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("vulnerableInWaterVulnerability", 0.5, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> VULNERABLE_IN_COLD_VULNERABILITY = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("vulnerableInColdVulnerability", 0.5, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> VULNERABLE_WHEN_DRY_VULNERABILITY = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("vulnerableWhenDryVulnerability", 0.5, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> SHORT_LEGGED_STEP_REDUCTION = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("shortLeggedStepReduction", 0.5, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> LONG_LEGGED_STEP_INCREASE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("longLeggedStepIncrease", 1.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> MOONLIT_FROG_HEALTH_PER_MOON_PHASE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("moonlitFrogHealthPerMoonPhase", 0.125, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> LEAP_JUMP_POWER_MODIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("leapJumpPowerModifier", 1.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> BIOME_ADAPTIVE_HOT_VULNERABILITY = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("biomeAdaptiveHotVulnerability", 0.25, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> BIOME_ADAPTIVE_TEMPERATE_FEEBLE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("biomeAdaptiveTemperateFeeble", 0.25, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> BIOME_ADAPTIVE_COLD_SLOWNESS_AMPLIFIER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("biomeAdaptiveColdSlownessAmplifier", 0.4, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> TONGUE_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("tongueRange", 10.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> TONGUE_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("tongueDamage", 3.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> NEARSIGHT_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("nearsightRange", 5.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> ECHOLOCATION_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("echolocationRange", 24.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> SONIC_BLAST_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("sonicBlastRange", 24.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> SONIC_BLAST_DAMAGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("sonicBlastDamage", 16.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> SONIC_BLAST_WIDTH = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("sonicBlastWidth", 5.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> SONIC_BLAST_KNOCKBACK = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("sonicBlastKnockback", 1.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> BURROW_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("burrowRange", 32.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_RESISTANCE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("creakingHeartResistance", 0.45, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_STRENGTH = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("creakingHeartStrength", 0.5, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("creakingHeartRange", 32.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_WEAKNESS = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("creakingHeartWeakness", 0.1, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_SLOWNESS = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("creakingHeartSlowness", 0.3, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_SCALE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("creakingHeartScale", 1.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_HEALTH = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("creakingHeartHealth", 40.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_ARMOR = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("creakingHeartArmor", 6.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> CREAKING_HEART_TOUGHNESS = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("creakingHeartToughness", 10.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> DAYLIGHT_WEAK_WEAKNESS = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("daylightWeakWeakness", 0.25, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> PACK_HUNTER_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("packHunterRange", 16.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> PACK_HUNTER_STRENGTH_PER_ALLY = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("packHunterStrengthPerAlly", 0.1, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> PACK_HUNTER_STRENGTH_PER_PACK_HUNTER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("packHunterStrengthPerPackHunter", 0.2, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> PACK_HUNTER_STRENGTH_MAX = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("packHunterStrengthMax", 1.5, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> PACK_HUNTER_SPEED_PER_ALLY = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("packHunterSpeedPerAlly", 0.05, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> PACK_HUNTER_SPEED_PER_PACK_HUNTER = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("packHunterSpeedPerPackHunter", 0.1, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> PACK_HUNTER_SPEED_MAX = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("packHunterSpeedMax", 1.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> PACK_HUNTER_ALLY_TEAMED_WITH_ROGUE = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("packHunterAllyTeamedWithRogue", true)));
   
   public static final IConfigSetting<?> PACK_HUNTER_ALLY_UNTEAMED_WITH_ROGUE = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("packHunterAllyUnteamedWithRogue", false)));
   
   public static final IConfigSetting<?> PACK_HUNTER_ALLY_UNTEAMED_WITH_TEAMED = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("packHunterAllyUnteamedWithTeamed", false)));
   
   public static final IConfigSetting<?> BERRY_EATER_SWEET_REGEN_RATE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("berryEaterSweetRegenRate", 0.035, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> BERRY_EATER_SWEET_STRENGTH = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("berryEaterSweetStrength", 0.25, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> BERRY_EATER_GLOW_SPEED = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("berryEaterGlowSpeed", 0.25, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> BERRY_EATER_GLOW_ABSORPTION = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("berryEaterGlowAbsorption", 2.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> SKIDDISH_RANGE = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("skiddishRange", 16.0, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> SKIDDISH_SPEED = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("skiddishSpeed", 0.15, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> SKIDDISH_WEAKNESS = registerConfigSetting(new ConfigSetting<>(
         new DoubleConfigValue("skiddishWeakness", 0.15, new DoubleConfigValue.DoubleLimits(0.0))));
   
   public static final IConfigSetting<?> SKIDDISH_ALLY_TEAMED_WITH_ROGUE = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("skiddishAllyTeamedWithRogue", false)));
   
   public static final IConfigSetting<?> SKIDDISH_ALLY_UNTEAMED_WITH_ROGUE = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("skiddishAllyUnteamedWithRogue", false)));
   
   public static final IConfigSetting<?> SKIDDISH_ALLY_UNTEAMED_WITH_TEAMED = registerConfigSetting(new ConfigSetting<>(
         new BooleanConfigValue("skiddishAllyUnteamedWithTeamed", false)));
   
   public static final ArchetypeAbility GOOD_SWIMMER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("good_swimmer").setDisplayStack(new ItemStackTemplate(Items.COD)).build());
   public static final ArchetypeAbility GREAT_SWIMMER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("great_swimmer").setReliantConfigs(GREAT_SWIMMER_MOVE_SPEED_MODIFIER, GREAT_SWIMMER_SLIPPERY_DAMAGE_MODIFIER).setDisplayStack(new ItemStackTemplate(Items.TROPICAL_FISH)).build());
   public static final ArchetypeAbility DRIES_OUT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("dries_out").setReliantConfigs(BIOME_DAMAGE).setDisplayStack(new ItemStackTemplate(Items.SPONGE)).build());
   public static final ArchetypeAbility IMPALE_VULNERABLE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("impale_vulnerable").setReliantConfigs(IMPALE_VULNERABLE_MODIFIER).setDisplayStack(new ItemStackTemplate(Items.TRIDENT)).build());
   public static final ArchetypeAbility REGEN_WHEN_LOW = register(new ArchetypeAbility.ArchetypeAbilityBuilder("regen_when_low").setReliantConfigs(REGENERATION_RATE).setDisplayStack(new ItemStackTemplate(Items.GOLDEN_APPLE)).build());
   public static final ArchetypeAbility FIRE_IMMUNE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("fire_immune").setDisplayStack(new ItemStackTemplate(Items.MAGMA_CREAM)).build());
   public static final ArchetypeAbility DAMAGED_BY_COLD = register(new ArchetypeAbility.ArchetypeAbilityBuilder("damaged_by_cold").setReliantConfigs(BIOME_DAMAGE, SNOWBALL_DAMAGE, COLD_DAMAGE_MODIFIER).setDisplayStack(new ItemStackTemplate(Items.SNOWBALL)).build());
   public static final ArchetypeAbility FIREBALL_VOLLEY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("fireball_volley").setReliantConfigs(FIREBALL_COOLDOWN).setDisplayStack(new ItemStackTemplate(Items.FIRE_CHARGE)).setActive().build());
   public static final ArchetypeAbility POTION_BREWER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("potion_brewer").setReliantConfigs(CAULDRON_DRINKABLE_COOLDOWN_MODIFIER, CAULDRON_THROWABLE_COOLDOWN_MODIFIER, CAULDRON_INSTANT_EFFECT_COOLDOWN).setDisplayStack(new ItemStackTemplate(Items.CAULDRON)).setActive().build());
   public static final ArchetypeAbility BOUNCY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("bouncy").setReliantConfigs(BOUNCY_BOUNCINESS).setDisplayStack(new ItemStackTemplate(Items.SLIME_BLOCK)).build());
   public static final ArchetypeAbility JUMPY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("jumpy").setReliantConfigs(JUMPY_JUMP_BOOST).setDisplayStack(new ItemStackTemplate(Items.RABBIT_FOOT)).build());
   public static final ArchetypeAbility SLIME_TOTEM = register(new ArchetypeAbility.ArchetypeAbilityBuilder("slime_totem").setReliantConfigs(GELATIAN_GROW_ITEM_EAT_DURATION).setDisplayStack(new ItemStackTemplate(Items.TOTEM_OF_UNDYING)).build());
   public static final ArchetypeAbility MAGMA_TOTEM = register(new ArchetypeAbility.ArchetypeAbilityBuilder("magma_totem").setReliantConfigs(GELATIAN_GROW_ITEM_EAT_DURATION).setDisplayStack(new ItemStackTemplate(Items.TOTEM_OF_UNDYING)).build());
   public static final ArchetypeAbility SULFUR_TOTEM = register(new ArchetypeAbility.ArchetypeAbilityBuilder("sulfur_totem").setReliantConfigs(GELATIAN_GROW_ITEM_EAT_DURATION).setDisplayStack(new ItemStackTemplate(Items.TOTEM_OF_UNDYING)).build());
   public static final ArchetypeAbility INSATIABLE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("insatiable").setReliantConfigs(INSATIATBLE_HUNGER_RATE, ADDED_STARVE_DAMAGE).setDisplayStack(new ItemStackTemplate(Items.ROTTEN_FLESH)).build());
   public static final ArchetypeAbility SLOW_FALLER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("slow_faller").setReliantConfigs(SLOW_FALLER_TRIGGER_SPEED).setDisplayStack(new ItemStackTemplate(Items.FEATHER)).build());
   public static final ArchetypeAbility PROJECTILE_RESISTANT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("projectile_resistant").setReliantConfigs(PROJECTILE_RESISTANT_REDUCTION).setDisplayStack(new ItemStackTemplate(Items.SHIELD)).build());
   public static final ArchetypeAbility SOFT_HITTER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("soft_hitter").setReliantConfigs(SOFT_HITTER_DAMAGE_REDUCTION).setDisplayStack(new ItemStackTemplate(Items.WOODEN_SWORD)).build());
   public static final ArchetypeAbility HARD_HITTER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("hard_hitter").setReliantConfigs(HARD_HITTER_KNOCKBACK_INCREASE, HARD_HITTER_DAMAGE_INCREASE).setDisplayStack(new ItemStackTemplate(Items.IRON_SWORD)).build());
   public static final ArchetypeAbility WIND_CHARGE_VOLLEY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("wind_charge_volley").setReliantConfigs(WIND_CHARGE_COOLDOWN).setDisplayStack(new ItemStackTemplate(Items.WIND_CHARGE)).setActive().build());
   public static final ArchetypeAbility WING_GLIDER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("glider").setReliantConfigs(GLIDER_COOLDOWN, GLIDER_DURATION, GLIDER_RECOVERY_TIME).setDisplayStack(new ItemStackTemplate(Items.ELYTRA)).setActive().build());
   public static final ArchetypeAbility NO_REGEN = register(new ArchetypeAbility.ArchetypeAbilityBuilder("no_regen").setDisplayStack(new ItemStackTemplate(Items.POISONOUS_POTATO)).build());
   public static final ArchetypeAbility COPPER_EATER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("copper_eater").setReliantConfigs(COPPER_FOOD_DURATION_MODIFIER, COPPER_FOOD_HEALTH_MODIFIER).setDisplayStack(new ItemStackTemplate(Items.COPPER_INGOT)).build());
   public static final ArchetypeAbility HEALTH_BASED_SPRINT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("health_based_sprint").setReliantConfigs(HEALTH_SPRINT_CUTOFF).setDisplayStack(new ItemStackTemplate(Items.DIAMOND_BOOTS)).build());
   public static final ArchetypeAbility IRON_EATER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("iron_eater").setReliantConfigs(IRON_FOOD_DURATION_MODIFIER, IRON_FOOD_HEALTH_MODIFIER).setDisplayStack(new ItemStackTemplate(Items.IRON_INGOT)).build());
   public static final ArchetypeAbility TUFF_EATER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("tuff_eater").setReliantConfigs(TUFF_FOOD_DURATION_MODIFIER, TUFF_FOOD_HEALTH_MODIFIER).setDisplayStack(new ItemStackTemplate(Items.TUFF)).build());
   public static final ArchetypeAbility HALF_SIZED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("half_sized").setDisplayStack(new ItemStackTemplate(Items.LEATHER_HELMET)).build());
   public static final ArchetypeAbility TALL_SIZED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("tall_sized").setDisplayStack(new ItemStackTemplate(Items.CHAINMAIL_HELMET)).build());
   public static final ArchetypeAbility GIANT_SIZED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("giant_sized").setDisplayStack(new ItemStackTemplate(Items.IRON_HELMET)).setOverrides(TALL_SIZED).build());
   public static final ArchetypeAbility LONG_ARMS = register(new ArchetypeAbility.ArchetypeAbilityBuilder("long_arms").setReliantConfigs(LONG_ARMS_RANGE).setDisplayStack(new ItemStackTemplate(Items.LEVER)).build());
   public static final ArchetypeAbility REDUCED_KNOCKBACK = register(new ArchetypeAbility.ArchetypeAbilityBuilder("reduced_knockback").setReliantConfigs(KNOCKBACK_DECREASE).setDisplayStack(new ItemStackTemplate(Items.IRON_CHESTPLATE)).build());
   public static final ArchetypeAbility INCREASED_KNOCKBACK = register(new ArchetypeAbility.ArchetypeAbilityBuilder("increased_knockback").setReliantConfigs(KNOCKBACK_INCREASE).setDisplayStack(new ItemStackTemplate(Items.LEATHER_CHESTPLATE)).build());
   public static final ArchetypeAbility HASTY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("hasty").setReliantConfigs(HASTY_MINING_MODIFIER, HASTY_ATTACK_SPEED_INCREASE).setDisplayStack(new ItemStackTemplate(Items.GOLDEN_PICKAXE)).build());
   public static final ArchetypeAbility HALVED_FALL_DAMAGE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("halved_fall_damage").setReliantConfigs(FALL_DAMAGE_REDUCTION).setDisplayStack(new ItemStackTemplate(Items.WOOL.lightGray())).build());
   public static final ArchetypeAbility NO_FALL_DAMAGE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("no_fall_damage").setDisplayStack(new ItemStackTemplate(Items.WOOL.white())).setOverrides(HALVED_FALL_DAMAGE).build());
   public static final ArchetypeAbility CARNIVORE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("carnivore").setDisplayStack(new ItemStackTemplate(Items.BEEF)).build());
   public static final ArchetypeAbility CAT_SCARE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("cat_scare").setDisplayStack(new ItemStackTemplate(Items.PHANTOM_MEMBRANE)).build());
   public static final ArchetypeAbility WOLF_SCARE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("wolf_scare").setDisplayStack(new ItemStackTemplate(Items.BONE)).build());
   public static final ArchetypeAbility SNEAK_ATTACK = register(new ArchetypeAbility.ArchetypeAbilityBuilder("sneak_attack").setReliantConfigs(MOB_SNEAK_ATTACK_MODIFIER, PLAYER_SNEAK_ATTACK_MODIFIER).setDisplayStack(new ItemStackTemplate(Items.DIAMOND_SWORD)).build());
   public static final ArchetypeAbility SPEEDY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("speedy").setReliantConfigs(SPEEDY_SPEED_BOOST).setDisplayStack(new ItemStackTemplate(Items.GOLDEN_BOOTS)).build());
   public static final ArchetypeAbility STUNNED_BY_DAMAGE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("stunned_by_damage").setReliantConfigs(DAMAGE_STUN_DURATION, STARTLE_MIN_DAMAGE).setDisplayStack(new ItemStackTemplate(Items.CHAINMAIL_CHESTPLATE)).build());
   public static final ArchetypeAbility HORSE_SPIRIT_MOUNT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("horse_spirit_mount").setReliantConfigs(SPIRIT_MOUNT_KILL_COOLDOWN, SPIRIT_MOUNT_REGENERATION_RATE).setDisplayStack(new ItemStackTemplate(Items.GOLDEN_HORSE_ARMOR)).setActive().build());
   public static final ArchetypeAbility DONKEY_SPIRIT_MOUNT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("donkey_spirit_mount").setReliantConfigs(SPIRIT_MOUNT_KILL_COOLDOWN, SPIRIT_MOUNT_REGENERATION_RATE).setDisplayStack(new ItemStackTemplate(Items.CHEST)).setActive().build());
   public static final ArchetypeAbility MOONLIT_SLIME = register(new ArchetypeAbility.ArchetypeAbilityBuilder("moonlit_slime").setReliantConfigs(MOONLIT_SLIME_HEALTH_PER_PHASE, MOONLIT_SLIME_SIZE_PER_PHASE).setDisplayStack(new ItemStackTemplate(Items.SEA_LANTERN)).build());
   public static final ArchetypeAbility MOONLIT_WITCH = register(new ArchetypeAbility.ArchetypeAbilityBuilder("moonlit_witch").setDisplayStack(new ItemStackTemplate(Items.SEA_LANTERN)).build());
   public static final ArchetypeAbility ANTIVENOM = register(new ArchetypeAbility.ArchetypeAbilityBuilder("antivenom").setDisplayStack(new ItemStackTemplate(Items.FERMENTED_SPIDER_EYE)).build());
   public static final ArchetypeAbility SLIPPERY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("slippery").setReliantConfigs(SLIPPERY_DAMAGE_MODIFIER).setDisplayStack(new ItemStackTemplate(Items.PHANTOM_MEMBRANE)).build());
   public static final ArchetypeAbility SNEAKY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("sneaky").setReliantConfigs(SNEAKY_SPEED_BOOST).setDisplayStack(new ItemStackTemplate(Items.LEATHER_BOOTS)).build());
   public static final ArchetypeAbility WITHERING = register(new ArchetypeAbility.ArchetypeAbilityBuilder("withering").setReliantConfigs(WITHERING_EFFECT_DURATION).setDisplayStack(new ItemStackTemplate(Items.WITHER_ROSE)).build());
   public static final ArchetypeAbility THORNY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("thorny").setReliantConfigs(THORNY_REFLECTION_CAP, THORNY_REFLECTION_MODIFIER).setDisplayStack(new ItemStackTemplate(Items.PRISMARINE_SHARD)).build());
   public static final ArchetypeAbility GUARDIAN_RAY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("guardian_ray").setReliantConfigs(GUARDIAN_RAY_COOLDOWN, GUARDIAN_RAY_DAMAGE, GUARDIAN_RAY_WINDUP, GUARDIAN_RAY_DURATION).setDisplayStack(new ItemStackTemplate(Items.PRISMARINE_CRYSTALS)).setActive().build());
   public static final ArchetypeAbility MOUNTED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("mounted").setReliantConfigs(MOUNTED_RANGE).setDisplayStack(new ItemStackTemplate(Items.SADDLE)).build());
   public static final ArchetypeAbility HURT_BY_WATER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("hurt_by_water").setReliantConfigs(HURT_BY_WATER_RAIN_DAMAGE, HURT_BY_WATER_SWIM_DAMAGE).setDisplayStack(new ItemStackTemplate(Items.WATER_BUCKET)).build());
   public static final ArchetypeAbility CAMEL_SPIRIT_MOUNT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("camel_spirit_mount").setReliantConfigs(SPIRIT_MOUNT_KILL_COOLDOWN, SPIRIT_MOUNT_REGENERATION_RATE).setDisplayStack(new ItemStackTemplate(Items.OAK_BOAT)).setActive().build());
   public static final ArchetypeAbility VENOMOUS = register(new ArchetypeAbility.ArchetypeAbilityBuilder("venomous").setReliantConfigs(VENOMOUS_POISON_DURATION, VENOMOUS_POISON_STRENGTH).setDisplayStack(new ItemStackTemplate(Items.SPIDER_EYE)).build());
   public static final ArchetypeAbility CLIMBING = register(new ArchetypeAbility.ArchetypeAbilityBuilder("climbing").setDisplayStack(new ItemStackTemplate(Items.LADDER)).build());
   public static final ArchetypeAbility MOONLIT_CAVE_SPIDER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("moonlit_cave_spider").setReliantConfigs(MOONLIT_CAVE_SPIDER_VENOM_STRENGTH_PER_PHASE, MOONLIT_CAVE_SPIDER_VENOM_DURATION_PER_PHASE).setDisplayStack(new ItemStackTemplate(Items.SEA_LANTERN)).build());
   public static final ArchetypeAbility WEAVING = register(new ArchetypeAbility.ArchetypeAbilityBuilder("weaving").setReliantConfigs(WEAVING_WEB_COOLDOWN).setDisplayStack(new ItemStackTemplate(Items.STRING)).setActive().build());
   public static final ArchetypeAbility SLOW_HOVER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("slow_hover").setReliantConfigs(SLOW_HOVER_FLIGHT_COOLDOWN, SLOW_HOVER_FLIGHT_DURATION, SLOW_HOVER_FLIGHT_SPEED, SLOW_HOVER_FLIGHT_RECOVERY_TIME).setDisplayStack(new ItemStackTemplate(Items.SNOW_BLOCK)).setActive().build());
   public static final ArchetypeAbility RIDEABLE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("rideable").setDisplayStack(new ItemStackTemplate(Items.SADDLE)).build());
   public static final ArchetypeAbility SNOW_BLAST = register(new ArchetypeAbility.ArchetypeAbilityBuilder("snow_blast").setReliantConfigs(SNOW_BLAST_COOLDOWN, SNOW_BLAST_RANGE, SNOW_BLAST_DAMAGE, SNOW_BLAST_SLOWNESS_DURATION, SNOW_BLAST_SLOWNESS_STRENGTH).setDisplayStack(new ItemStackTemplate(Items.SNOWBALL)).setActive().build());
   public static final ArchetypeAbility SILK_TOUCH = register(new ArchetypeAbility.ArchetypeAbilityBuilder("silk_touch").setDisplayStack(new ItemStackTemplate(Items.WOOL.white())).build());
   public static final ArchetypeAbility SHY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("shy").setReliantConfigs(SHY_NOTICING_ANGLE, SHY_VIEWING_ANGLE).setDisplayStack(new ItemStackTemplate(Items.CARVED_PUMPKIN)).build());
   public static final ArchetypeAbility LONG_TELEPORT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("long_teleport").setReliantConfigs(LONG_TELEPORT_DISTANCE, LONG_TELEPORT_COOLDOWN).setDisplayStack(new ItemStackTemplate(Items.ENDER_PEARL)).setActive().build());
   public static final ArchetypeAbility FORTIFY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("fortify").setReliantConfigs(FORTIFY_COOLDOWN, FORTIFY_DURATION, FORTIFY_RECOVERY_TIME, FORTIFY_DAMAGE_MODIFIER).setDisplayStack(new ItemStackTemplate(Items.SHIELD)).setActive().build());
   public static final ArchetypeAbility LEVITATION_BULLET = register(new ArchetypeAbility.ArchetypeAbilityBuilder("levitation_bullet").setReliantConfigs(LEVITATION_BULLET_COOLDOWN, LEVITATION_BULLET_COUNT).setDisplayStack(new ItemStackTemplate(Items.ARROW)).setActive().build());
   public static final ArchetypeAbility BACKPACK = register(new ArchetypeAbility.ArchetypeAbilityBuilder("backpack").setDisplayStack(new ItemStackTemplate(Items.DYED_BUNDLE.magenta(), DataComponentPatch.builder().remove(DataComponents.BUNDLE_CONTENTS).build())).setActive().build());
   public static final ArchetypeAbility RANDOM_TELEPORT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("random_teleport").setReliantConfigs(RANDOM_TELEPORT_COOLDOWN, RANDOM_TELEPORT_RANGE).setDisplayStack(new ItemStackTemplate(Items.CHORUS_FRUIT)).setActive().build());
   public static final ArchetypeAbility ENDER_GLIDER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("ender_glider").setReliantConfigs(GLIDER_COOLDOWN, GLIDER_DURATION, GLIDER_RECOVERY_TIME).setDisplayStack(new ItemStackTemplate(Items.ELYTRA)).setActive().build());
   public static final ArchetypeAbility ENDERFLAME = register(new ArchetypeAbility.ArchetypeAbilityBuilder("enderflame").setReliantConfigs(ENDERFLAME_BUFFET_COOLDOWN, ENDERFLAME_BUFFET_DAMAGE, ENDERFLAME_BUFFET_DURATION, ENDERFLAME_FIREBALL_COOLDOWN).setDisplayStack(new ItemStackTemplate(Items.DRAGON_BREATH)).setActive().build());
   public static final ArchetypeAbility MASSIVE_SIZED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("massive_sized").setDisplayStack(new ItemStackTemplate(Items.DIAMOND_HELMET)).setActive().build());
   public static final ArchetypeAbility RESILIENT_JOINTS = register(new ArchetypeAbility.ArchetypeAbilityBuilder("resilient_joints").setReliantConfigs(RESILIENT_JOINTS_EXTRA_FALL_BLOCKS).setDisplayStack(new ItemStackTemplate(Items.DIAMOND_LEGGINGS)).build());
   public static final ArchetypeAbility LIGHTWEIGHT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("lightweight").setReliantConfigs(LIGHTWEIGHT_INCREASED_KNOCKBACK, LIGHTWEIGHT_DRAG_REDUCTION).setDisplayStack(new ItemStackTemplate(Items.RABBIT_FOOT)).build());
   public static final ArchetypeAbility BLAZING_STRIKE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("blazing_strike").setReliantConfigs(BLAZING_STRIKE_DURATION).setDisplayStack(new ItemStackTemplate(Items.BLAZE_POWDER)).build());
   public static final ArchetypeAbility LAVA_WALKER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("lava_walker").setReliantConfigs(LAVA_WALKER_SPEED_MULTIPLIER).setDisplayStack(new ItemStackTemplate(Items.NETHERITE_BOOTS)).build());
   public static final ArchetypeAbility FUNGUS_SPEED_BOOST = register(new ArchetypeAbility.ArchetypeAbilityBuilder("fungus_speed_boost").setReliantConfigs(FUNGUS_SPEED_BOOST_DURATION, FUNGUS_SPEED_BOOST_CONSUME_DURATION, FUNGUS_SPEED_BOOST_MULTIPLIER).setDisplayStack(new ItemStackTemplate(Items.WARPED_FUNGUS)).build());
   public static final ArchetypeAbility WAX_SHIELD = register(new ArchetypeAbility.ArchetypeAbilityBuilder("wax_shield").setReliantConfigs(WAX_SHIELD_HEALTH, WAX_SHIELD_MAX_HEALTH, WAX_SHIELD_CONSUME_DURATION, WAX_SHIELD_DURATION).setDisplayStack(new ItemStackTemplate(Items.HONEYCOMB)).build());
   public static final ArchetypeAbility CHOCOLATE_ALLERGY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("chocolate_allergy").setReliantConfigs(CHOCOLATE_ALLERGY_AMPLIFIER, CHOCOLATE_ALLERGY_DURATION).setDisplayStack(new ItemStackTemplate(Items.COOKIE)).build());
   public static final ArchetypeAbility VULNERABLE_IN_WATER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("vulnerable_in_water").setReliantConfigs(VULNERABLE_IN_WATER_VULNERABILITY).setDisplayStack(new ItemStackTemplate(Items.WATER_BUCKET)).build());
   public static final ArchetypeAbility VULNERABLE_IN_COLD = register(new ArchetypeAbility.ArchetypeAbilityBuilder("vulnerable_in_cold").setReliantConfigs(VULNERABLE_IN_COLD_VULNERABILITY).setDisplayStack(new ItemStackTemplate(Items.PACKED_ICE)).build());
   public static final ArchetypeAbility VULNERABLE_WHEN_DRY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("vulnerable_when_dry").setReliantConfigs(VULNERABLE_WHEN_DRY_VULNERABILITY).setDisplayStack(new ItemStackTemplate(Items.SPONGE)).build());
   public static final ArchetypeAbility SHORT_LEGGED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("short_legged").setReliantConfigs(SHORT_LEGGED_STEP_REDUCTION).setDisplayStack(new ItemStackTemplate(Items.CHAINMAIL_LEGGINGS)).build());
   public static final ArchetypeAbility LONG_LEGGED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("long_legged").setReliantConfigs(LONG_LEGGED_STEP_INCREASE).setDisplayStack(new ItemStackTemplate(Items.IRON_LEGGINGS)).build());
   public static final ArchetypeAbility MOONLIT_FROG = register(new ArchetypeAbility.ArchetypeAbilityBuilder("moonlit_frog").setReliantConfigs(MOONLIT_FROG_HEALTH_PER_MOON_PHASE).setDisplayStack(new ItemStackTemplate(Items.SEA_LANTERN)).build());
   public static final ArchetypeAbility LEAP = register(new ArchetypeAbility.ArchetypeAbilityBuilder("leap").setReliantConfigs(LEAP_COOLDOWN, LEAP_MAX_CHARGE_TIME, LEAP_JUMP_POWER_MODIFIER).setDisplayStack(new ItemStackTemplate(Items.RABBIT_FOOT)).build());
   public static final ArchetypeAbility BIOME_ADAPTIVE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("biome_adaptive").setReliantConfigs(BIOME_ADAPTIVE_HOT_VULNERABILITY, BIOME_ADAPTIVE_HOT_VULNERABILITY_DURATION, BIOME_ADAPTIVE_TEMPERATE_FEEBLE, BIOME_ADAPTIVE_TEMPERATE_FEEBLE_DURATION, BIOME_ADAPTIVE_COLD_SLOWNESS_AMPLIFIER, BIOME_ADAPTIVE_COLD_SLOWNESS_DURATION).setDisplayStack(new ItemStackTemplate(Items.FROGSPAWN)).build());
   public static final ArchetypeAbility TONGUE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("tongue").setReliantConfigs(TONGUE_COOLDOWN, TONGUE_RANGE, TONGUE_DAMAGE).setDisplayStack(new ItemStackTemplate(Items.FISHING_ROD)).setActive().build());
   public static final ArchetypeAbility NEARSIGHTED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("nearsighted").setReliantConfigs(NEARSIGHT_RANGE).setDisplayStack(new ItemStackTemplate(Items.SPYGLASS)).build());
   public static final ArchetypeAbility ECHOLOCATION = register(new ArchetypeAbility.ArchetypeAbilityBuilder("echolocation").setReliantConfigs(ECHOLOCATION_RANGE).setDisplayStack(new ItemStackTemplate(Items.SCULK_SENSOR)).build());
   public static final ArchetypeAbility SONIC_BLAST = register(new ArchetypeAbility.ArchetypeAbilityBuilder("sonic_blast").setReliantConfigs(SONIC_BLAST_COOLDOWN, SONIC_BLAST_CHARGE_DURATION, SONIC_BLAST_RANGE, SONIC_BLAST_DAMAGE, SONIC_BLAST_WIDTH, SONIC_BLAST_KNOCKBACK).setDisplayStack(new ItemStackTemplate(Items.ECHO_SHARD)).setActive().build());
   public static final ArchetypeAbility DAYLIGHT_WEAK = register(new ArchetypeAbility.ArchetypeAbilityBuilder("daylight_weak").setReliantConfigs(DAYLIGHT_WEAK_WEAKNESS).setDisplayStack(new ItemStackTemplate(Items.OPEN_EYEBLOSSOM)).build());
   public static final ArchetypeAbility PACK_HUNTER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("pack_hunter").setReliantConfigs(PACK_HUNTER_RANGE, PACK_HUNTER_STRENGTH_PER_ALLY, PACK_HUNTER_STRENGTH_PER_PACK_HUNTER, PACK_HUNTER_STRENGTH_MAX, PACK_HUNTER_SPEED_PER_ALLY, PACK_HUNTER_SPEED_PER_PACK_HUNTER, PACK_HUNTER_SPEED_MAX, PACK_HUNTER_ALLY_TEAMED_WITH_ROGUE, PACK_HUNTER_ALLY_UNTEAMED_WITH_ROGUE, PACK_HUNTER_ALLY_UNTEAMED_WITH_TEAMED).setDisplayStack(new ItemStackTemplate(Items.WOLF_ARMOR)).build());
   public static final ArchetypeAbility BERRY_EATER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("berry_eater").setReliantConfigs(BERRY_EATER_EAT_DURATION, BERRY_EATER_SWEET_REGEN_RATE, BERRY_EATER_SWEET_REGEN_DURATION, BERRY_EATER_SWEET_STRENGTH, BERRY_EATER_SWEET_STRENGTH_DURATION, BERRY_EATER_GLOW_SPEED, BERRY_EATER_GLOW_SPEED_DURATION, BERRY_EATER_GLOW_ABSORPTION, BERRY_EATER_GLOW_ABSORPTION_DURATION).setDisplayStack(new ItemStackTemplate(Items.SWEET_BERRIES)).build());
   public static final ArchetypeAbility SKIDDISH = register(new ArchetypeAbility.ArchetypeAbilityBuilder("skiddish").setReliantConfigs(SKIDDISH_RANGE, SKIDDISH_SPEED, SKIDDISH_WEAKNESS, SKIDDISH_ALLY_TEAMED_WITH_ROGUE, SKIDDISH_ALLY_UNTEAMED_WITH_ROGUE, SKIDDISH_ALLY_UNTEAMED_WITH_TEAMED).setDisplayStack(new ItemStackTemplate(Items.RABBIT_HIDE)).build());
   public static final ArchetypeAbility SHORT_SIZED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("short_sized").setDisplayStack(new ItemStackTemplate(Items.GOLDEN_HELMET)).build());
   public static final ArchetypeAbility BURROW = register(new ArchetypeAbility.ArchetypeAbilityBuilder("burrow").setReliantConfigs(BURROW_COOLDOWN, BURROW_RANGE).setDisplayStack(new ItemStackTemplate(Items.STONE)).setActive().build());
   public static final ArchetypeAbility CREAKING_HEART = register(new ArchetypeAbility.ArchetypeAbilityBuilder("creaking_heart")
         .setReliantConfigs(CREAKING_HEART_RESPAWN_TIMER, CREAKING_HEART_RESISTANCE, CREAKING_HEART_STRENGTH, CREAKING_HEART_RANGE,
               CREAKING_HEART_WEAKNESS, CREAKING_HEART_WEAKNESS_DURATION, CREAKING_HEART_SLOWNESS, CREAKING_HEART_SLOWNESS_DURATION,
               CREAKING_HEART_COOLDOWN, CREAKING_HEART_ARMOR, CREAKING_HEART_TOUGHNESS, CREAKING_HEART_HEALTH, CREAKING_HEART_SCALE)
         .setDisplayStack(new ItemStackTemplate(Items.CREAKING_HEART)).setActive().build());
   public static final ArchetypeAbility METAMORPH = register(new ArchetypeAbility.ArchetypeAbilityBuilder("metamorph")
         .setReliantConfigs(METAMORPH_EAT_DURATION, METAMORPH_ABILITY_DURATION,
               METAMORPH_ICE_FRICTION_REDUCTION, METAMORPH_ICE_FREEZE_RANGE, METAMORPH_ICE_DRAG_REDUCTION, METAMORPH_WOOL_FALL_MODIFIER,
               METAMORPH_IRON_PROJECTILE_MODIFIER, METAMORPH_IRON_BLAST_MODIFIER, METAMORPH_IRON_KNOCKBACK_MODIFIER,
               METAMORPH_NETHERITE_BLAST_MODIFIER, METAMORPH_NETHERITE_KNOCKBACK_MODIFIER,
               METAMORPH_TNT_EXPLOSION_POWER, METAMORPH_TNT_DAMAGES_BLOCKS, METAMORPH_TNT_FIRE_FUSE_TIME,
               METAMORPH_TNT_DEATH_FUSE_TIME, METAMORPH_TNT_DEATH_ABSORPTION_HP, METAMORPH_TNT_EXPLOSION_FUSE_TIME, METAMORPH_GOLD_REGEN_RATE,
               METAMORPH_MAGMA_RANGE, METAMORPH_MAGMA_FIRE_DURATION)
         .setDisplayStack(new ItemStackTemplate(Items.SULFUR)).build());
   
   public static final Archetype AQUARIAN = register(new Archetype("aquarian", new ItemStackTemplate(Items.TROPICAL_FISH), 0x0f89f0));
   public static final Archetype CANID = register(new Archetype("canid", new ItemStackTemplate(Items.BONE), 0xc27224));
   public static final Archetype CENTAUR = register(new Archetype("centaur", new ItemStackTemplate(Items.SADDLE), 0xbd8918));
   public static final Archetype ENDERIAN = register(new Archetype("enderian", new ItemStackTemplate(Items.END_CRYSTAL), 0xc30ff0));
   public static final Archetype FELID = register(new Archetype("felid", new ItemStackTemplate(Items.STRING), 0xc6c55c));
   public static final Archetype GELATIAN = register(new Archetype("gelatian", new ItemStackTemplate(Items.SLIME_BLOCK), 0xafeb49));
   public static final Archetype GOLEM = register(new Archetype("golem", new ItemStackTemplate(Items.CHISELED_STONE_BRICKS), 0xa0a0ab));
   public static final Archetype INFERNAL = register(new Archetype("infernal", new ItemStackTemplate(Items.CRIMSON_NYLIUM), 0xe03f24));
   public static final Archetype SENTINEL = register(new Archetype("sentinel", new ItemStackTemplate(Items.DARK_OAK_LOG), 0x0a580a));
   public static final Archetype SWAMPER = register(new Archetype("swamper", new ItemStackTemplate(Items.SLIME_BLOCK), 0x4dca70));
   public static final Archetype WINDSWEPT = register(new Archetype("windswept", new ItemStackTemplate(Items.FEATHER), 0x98c9c6));
   
   public static final SubArchetype AXOLOTL = register(new SubArchetype("axolotl", EntityTypes.AXOLOTL, new ItemStackTemplate(Items.AXOLOTL_BUCKET), 0xe070ed, AQUARIAN, REGEN_WHEN_LOW, GOOD_SWIMMER, DRIES_OUT, IMPALE_VULNERABLE, SLIPPERY));
   public static final SubArchetype SALMON = register(new SubArchetype("salmon", EntityTypes.SALMON, new ItemStackTemplate(Items.SALMON), 0x8f1f63, AQUARIAN, GREAT_SWIMMER, GOOD_SWIMMER, DRIES_OUT, IMPALE_VULNERABLE, SLIPPERY));
   public static final SubArchetype GUARDIAN = register(new SubArchetype("guardian", EntityTypes.GUARDIAN, new ItemStackTemplate(Items.PRISMARINE_BRICKS), 0x449e92, AQUARIAN, GUARDIAN_RAY, THORNY, GOOD_SWIMMER, DRIES_OUT, IMPALE_VULNERABLE, SLIPPERY));
   public static final SubArchetype HORSE = register(new SubArchetype("horse", EntityTypes.HORSE, new ItemStackTemplate(Items.GOLDEN_HORSE_ARMOR), 0xbda329, CENTAUR, HORSE_SPIRIT_MOUNT, STUNNED_BY_DAMAGE, MOUNTED));
   public static final SubArchetype DONKEY = register(new SubArchetype("donkey", EntityTypes.DONKEY, new ItemStackTemplate(Items.CHEST), 0x9c6d11, CENTAUR, DONKEY_SPIRIT_MOUNT, STUNNED_BY_DAMAGE, MOUNTED));
   public static final SubArchetype CAMEL = register(new SubArchetype("camel", EntityTypes.CAMEL, new ItemStackTemplate(Items.SAND), 0xffc163, CENTAUR, CAMEL_SPIRIT_MOUNT, STUNNED_BY_DAMAGE, MOUNTED));
   public static final SubArchetype ENDERMAN = register(new SubArchetype("enderman", EntityTypes.ENDERMAN, new ItemStackTemplate(Items.ENDER_EYE), 0xca00e2, ENDERIAN, SHY, TALL_SIZED, LONG_ARMS, SILK_TOUCH, LONG_TELEPORT, PROJECTILE_RESISTANT, HURT_BY_WATER));
   public static final SubArchetype SHULKER = register(new SubArchetype("shulker", EntityTypes.SHULKER, new ItemStackTemplate(Items.SHULKER_SHELL), 0x7e597f, ENDERIAN, FORTIFY, LEVITATION_BULLET, BACKPACK, RANDOM_TELEPORT, HURT_BY_WATER));
   public static final SubArchetype ENDER_DRAGON = register(new SubArchetype("ender_dragon", EntityTypes.ENDER_DRAGON, new ItemStackTemplate(Items.DRAGON_EGG), 0x762f9f, ENDERIAN, MASSIVE_SIZED, ENDER_GLIDER, ENDERFLAME, REDUCED_KNOCKBACK, LONG_ARMS, RIDEABLE, HURT_BY_WATER));
   public static final SubArchetype CAT = register(new SubArchetype("cat", EntityTypes.CAT, new ItemStackTemplate(Items.PHANTOM_MEMBRANE), 0xf1ce8a, FELID, CAT_SCARE, NO_FALL_DAMAGE, SNEAKY, CARNIVORE, SPEEDY));
   public static final SubArchetype OCELOT = register(new SubArchetype("ocelot", EntityTypes.OCELOT, new ItemStackTemplate(Items.CHICKEN), 0xc5b900, FELID, SNEAK_ATTACK, HALVED_FALL_DAMAGE, CARNIVORE, SPEEDY));
   public static final SubArchetype COPPER_GOLEM = register(new SubArchetype("copper_golem", EntityTypes.COPPER_GOLEM, new ItemStackTemplate(Items.COPPER_BLOCK.weathering().unaffected()), 0xbc814d, GOLEM, COPPER_EATER, HALF_SIZED, LIGHTWEIGHT, SOFT_HITTER, RESILIENT_JOINTS, WAX_SHIELD, NO_REGEN, HEALTH_BASED_SPRINT, PROJECTILE_RESISTANT));
   public static final SubArchetype TUFF_GOLEM = register(new SubArchetype("tuff_golem", null, new ItemStackTemplate(Items.CHISELED_TUFF_BRICKS), 0x648076, GOLEM, TUFF_EATER, HASTY, NO_REGEN, HEALTH_BASED_SPRINT, PROJECTILE_RESISTANT));
   public static final SubArchetype IRON_GOLEM = register(new SubArchetype("iron_golem", EntityTypes.IRON_GOLEM, new ItemStackTemplate(Items.IRON_BLOCK), 0xbebebe, GOLEM, IRON_EATER, GIANT_SIZED, REDUCED_KNOCKBACK, LONG_ARMS, HARD_HITTER, NO_REGEN, HEALTH_BASED_SPRINT, PROJECTILE_RESISTANT));
   public static final SubArchetype BLAZE = register(new SubArchetype("blaze", EntityTypes.BLAZE, new ItemStackTemplate(Items.BLAZE_ROD), 0xe88a0f, INFERNAL, FIREBALL_VOLLEY, SLOW_FALLER, BLAZING_STRIKE, FIRE_IMMUNE, DAMAGED_BY_COLD));
   public static final SubArchetype WITHER_SKELETON = register(new SubArchetype("wither_skeleton", EntityTypes.WITHER_SKELETON, new ItemStackTemplate(Items.WITHER_SKELETON_SKULL), 0x423c3c, INFERNAL, WITHERING, TALL_SIZED, FIRE_IMMUNE, DAMAGED_BY_COLD));
   public static final SubArchetype STRIDER = register(new SubArchetype("strider", EntityTypes.STRIDER, new ItemStackTemplate(Items.STRING), 0x943019, INFERNAL, RIDEABLE, LAVA_WALKER, FUNGUS_SPEED_BOOST, FIRE_IMMUNE, DAMAGED_BY_COLD));
   public static final SubArchetype WITCH = register(new SubArchetype("witch", EntityTypes.WITCH, new ItemStackTemplate(Items.CAULDRON), 0x7a0fe8, SWAMPER, POTION_BREWER, MOONLIT_WITCH, ANTIVENOM));
   public static final SubArchetype CAVE_SPIDER = register(new SubArchetype("cave_spider", EntityTypes.CAVE_SPIDER, new ItemStackTemplate(Items.COBWEB), 0x1a7264, SWAMPER, HALF_SIZED, SOFT_HITTER, CLIMBING, VENOMOUS, LIGHTWEIGHT, MOONLIT_CAVE_SPIDER, WEAVING, RESILIENT_JOINTS, ANTIVENOM));
   public static final SubArchetype FROG = register(new SubArchetype("frog", EntityTypes.FROG, new ItemStackTemplate(Items.VERDANT_FROGLIGHT), 0x467243, SWAMPER, HALF_SIZED, SOFT_HITTER, MOONLIT_FROG, RESILIENT_JOINTS, ANTIVENOM, LEAP, BIOME_ADAPTIVE, TONGUE));
   public static final SubArchetype BREEZE = register(new SubArchetype("breeze", EntityTypes.BREEZE, new ItemStackTemplate(Items.WIND_CHARGE), 0x6ac1e6, WINDSWEPT, PROJECTILE_RESISTANT, SOFT_HITTER, LEAP, WIND_CHARGE_VOLLEY, SLOW_FALLER));
   public static final SubArchetype PARROT = register(new SubArchetype("parrot", EntityTypes.PARROT, new ItemStackTemplate(Items.ELYTRA), 0xb7d3df, WINDSWEPT, WING_GLIDER, LIGHTWEIGHT, CHOCOLATE_ALLERGY, SLOW_FALLER));
   public static final SubArchetype GHASTLING = register(new SubArchetype("ghastling", EntityTypes.HAPPY_GHAST, new ItemStackTemplate(Items.HARNESS.gray()), 0xa9e5e7, WINDSWEPT, SLOW_HOVER, DRIES_OUT, SNOW_BLAST, RIDEABLE, SLOW_FALLER));
   public static final SubArchetype SLIME = register(new SubArchetype("slime", EntityTypes.SLIME, new ItemStackTemplate(Items.SLIME_BLOCK), 0x05f905, GELATIAN, BOUNCY, JUMPY, SLIME_TOTEM, INSATIABLE));
   public static final SubArchetype MAGMA_CUBE = register(new SubArchetype("magma_cube", EntityTypes.MAGMA_CUBE, new ItemStackTemplate(Items.MAGMA_BLOCK), 0x943019, GELATIAN, BOUNCY, JUMPY, MAGMA_TOTEM, INSATIABLE, FIRE_IMMUNE, DAMAGED_BY_COLD, BLAZING_STRIKE));
   public static final SubArchetype SULFUR_CUBE = register(new SubArchetype("sulfur_cube", EntityTypes.SULFUR_CUBE, new ItemStackTemplate(Items.CHISELED_SULFUR), 0xD4E676, GELATIAN, BOUNCY, JUMPY, SULFUR_TOTEM, INSATIABLE, METAMORPH, SOFT_HITTER));
   public static final SubArchetype WARDEN = register(new SubArchetype("warden", EntityTypes.WARDEN, new ItemStackTemplate(Items.ECHO_SHARD), 0x32D6D3, SENTINEL, NEARSIGHTED, ECHOLOCATION, SONIC_BLAST, BURROW, GIANT_SIZED, REDUCED_KNOCKBACK, LONG_ARMS, HARD_HITTER));
   public static final SubArchetype CREAKING = register(new SubArchetype("creaking", EntityTypes.CREAKING, new ItemStackTemplate(Items.CREAKING_HEART), 0xA88A7A, SENTINEL, CREAKING_HEART, SHY, DAYLIGHT_WEAK));
   public static final SubArchetype WOLF = register(new SubArchetype("wolf", EntityTypes.WOLF, new ItemStackTemplate(Items.WOLF_ARMOR), 0xE6E3B3, CANID, CARNIVORE, PACK_HUNTER, WOLF_SCARE));
   public static final SubArchetype FOX = register(new SubArchetype("fox", EntityTypes.FOX, new ItemStackTemplate(Items.SWEET_BERRIES), 0xF7A957, CANID, CARNIVORE, BERRY_EATER, SKIDDISH, SHORT_SIZED, SPEEDY));
   
   public static final ResourceKey<? extends Registry<EquipmentAsset>> EQUIPMENT_ASSET_REGISTRY_KEY = ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("equipment_asset"));
   
   public static final ResourceKey<TrimPattern> HELMET_TRIM_PATTERN = ResourceKey.create(Registries.TRIM_PATTERN, archetypesId("aviator_helmet"));
   public static final ResourceKey<TrimPattern> HELMET_TRIM_PATTERN_ON = ResourceKey.create(Registries.TRIM_PATTERN, archetypesId("aviator_helmet_on"));
   public static final ResourceKey<TrimPattern> HELMET_TRIM_PATTERN_OFF = ResourceKey.create(Registries.TRIM_PATTERN, archetypesId("aviator_helmet_off"));
   public static final ResourceKey<TrimPattern> WING_GLIDER_TRIM_PATTERN = ResourceKey.create(Registries.TRIM_PATTERN, archetypesId("wing_glider"));
   public static final ResourceKey<TrimPattern> END_GLIDER_TRIM_PATTERN = ResourceKey.create(Registries.TRIM_PATTERN, archetypesId("end_glider"));
   
   public static final ResourceKey<DamageType> SONIC_BOOM = ResourceKey.create(Registries.DAMAGE_TYPE, archetypesId("sonic_boom"));
   public static final ResourceKey<DamageType> METAMORPH_TNT = ResourceKey.create(Registries.DAMAGE_TYPE, archetypesId("metamorph_tnt"));
   public static final ResourceKey<DamageType> METAMORPH_TNT_EXECUTE = ResourceKey.create(Registries.DAMAGE_TYPE, archetypesId("metamorph_tnt_execute"));
   
   public static final LoginCallback WAX_SHIELD_LOGIN = registerCallback(new WaxShieldLoginCallback());
   public static final LoginCallback BERRY_SHIELD_LOGIN = registerCallback(new GlowBerryShieldLoginCallback());
   public static final LoginCallback METAMORPH_TNT_SHIELD_LOGIN = registerCallback(new MetamorphTNTShieldLoginCallback());
   
   // PlayerAbilityLib Identifiers
   public static final AbilitySource SLOW_HOVER_ABILITY = Pal.getAbilitySource(archetypesId(SLOW_HOVER.id()), AbilitySource.RENEWABLE);
   
   public static final Item CHANGE_ITEM = registerItem("change_item", new ChangeItem(
         new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.change_item_description"))))
   ));
   public static final Item FIREBALL_VOLLEY_ITEM = registerItem(FIREBALL_VOLLEY.id(), new FireballVolleyItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.CONSUMABLE, Consumable.builder()
                     .consumeSeconds(5).animation(ItemUseAnimation.BOW).hasConsumeParticles(false)
                     .sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME)).build())
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.fireball_volley_description"))))
   ));
   public static final Item WIND_CHARGE_VOLLEY_ITEM = registerItem(WIND_CHARGE_VOLLEY.id(), new WindChargeVolleyItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.wind_charge_volley_description"))))
   ));
   public static final Item POTION_BREWER_ITEM = registerItem("potion_brewer", new PortableCauldronItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.potion_brewer_description"))))
   ));
   public static final Item GLIDER_ITEM = registerItem("glider", new WingGliderItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).durability(2048)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.glider_description"))))
               .component(DataComponents.GLIDER, Unit.INSTANCE)
               .component(DataComponents.DYED_COLOR, new DyedItemColor(0xeeeeee))
               .component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR, true).withHidden(DataComponents.TRIM, true))
               .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST).setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA).setAsset(ResourceKey.create(EQUIPMENT_ASSET_REGISTRY_KEY, archetypesId("glider"))).setDamageOnHurt(false).build())
   ));
   public static final Item HORSE_SPIRIT_MOUNT_ITEM = registerItem(HORSE_SPIRIT_MOUNT.id(), new HorseSpiritMountItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.horse_spirit_mount_description"))))
   ));
   public static final Item DONKEY_SPIRIT_MOUNT_ITEM = registerItem(DONKEY_SPIRIT_MOUNT.id(), new DonkeySpiritMountItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.donkey_spirit_mount_description"))))
   ));
   public static final Item CAMEL_SPIRIT_MOUNT_ITEM = registerItem(CAMEL_SPIRIT_MOUNT.id(), new CamelSpiritMountItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.camel_spirit_mount_description"))))
   ));
   public static final Item WEAVING_ITEM = registerItem(WEAVING.id(), new WeavingItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.weaving_web_description"))))
   ));
   public static final Item SLOW_HOVER_ITEM = registerItem(SLOW_HOVER.id(), new HoverItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).durability(2048)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.hover_helmet_description"))))
               .component(DataComponents.DYED_COLOR, new DyedItemColor(0xA06540))
               .component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR, true).withHidden(DataComponents.TRIM, true))
               .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setAsset(ResourceKey.create(EQUIPMENT_ASSET_REGISTRY_KEY, archetypesId("aviator_helmet_off"))).setDamageOnHurt(false).build())
   ));
   public static final Item SNOW_BLAST_ITEM = registerItem(SNOW_BLAST.id(), new SnowblastItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.snow_blast_description"))))
   ));
   public static final Item LONG_TELEPORT_ITEM = registerItem(LONG_TELEPORT.id(), new LongTeleportItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.long_teleport_description"))))
   ));
   public static final Item BURROW_ITEM = registerItem(BURROW.id(), new BurrowItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.burrow_description"))))
   ));
   public static final Item RANDOM_TELEPORT_ITEM = registerItem(RANDOM_TELEPORT.id(), new RandomTeleportItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.random_teleport_description"))))
   ));
   public static final Item FORTIFY_ITEM = registerItem(FORTIFY.id(), new FortifyItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.CONSUMABLE, Consumable.builder()
                     .consumeSeconds(72000).animation(ItemUseAnimation.BLOCK).hasConsumeParticles(false)
                     .sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME)).build())
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.fortify_description"))))
   ));
   public static final Item LEVITATION_BULLET_ITEM = registerItem(LEVITATION_BULLET.id(), new LevitationBulletItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.levitation_bullet_description"))))
   ));
   public static final Item BACKPACK_ITEM = registerItem(BACKPACK.id(), new BackpackItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.backpack_description"))))
   ));
   public static final Item END_GLIDER_ITEM = registerItem(ENDER_GLIDER.id(), new EndGliderItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).durability(2048)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.end_glider_description"))))
               .component(DataComponents.GLIDER, Unit.INSTANCE)
               .component(DataComponents.DYED_COLOR, new DyedItemColor(0xaaaaaa))
               .component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.DYED_COLOR, true).withHidden(DataComponents.TRIM, true))
               .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST).setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA).setAsset(ResourceKey.create(EQUIPMENT_ASSET_REGISTRY_KEY, archetypesId("end_glider"))).setDamageOnHurt(false).build())
   ));
   public static final Item ENDERFLAME_ITEM = registerItem(ENDERFLAME.id(), new EndflameItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.CONSUMABLE, Consumable.builder()
                     .consumeSeconds(10).animation(ItemUseAnimation.BOW).hasConsumeParticles(false)
                     .sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME)).build())
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.endflame_buffet_description"))))
   ));
   public static final Item GUARDIAN_RAY_ITEM = registerItem(GUARDIAN_RAY.id(), new GuardianRayItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.CONSUMABLE, Consumable.builder()
                     .consumeSeconds(72000).animation(ItemUseAnimation.BOW).hasConsumeParticles(false)
                     .sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME)).build())
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.guardian_ray_description"))))
   ));
   public static final Item SONIC_BLAST_ITEM = registerItem(SONIC_BLAST.id(), new SonicBlastItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.CONSUMABLE, Consumable.builder()
                     .consumeSeconds(72000).animation(ItemUseAnimation.BOW).hasConsumeParticles(false)
                     .sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME)).build())
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.sonic_blast_description"))))
   ));
   public static final Item TONGUE_ITEM = registerItem(TONGUE.id(), new TongueItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.tongue_description"))))
   ));
   public static final Item CREAKING_HEART_ITEM = registerItem(CREAKING_HEART.id(), new CreakingHeartItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
               .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("text.ancestralarchetypes.creaking_heart_description"))))
   ));
   public static final Item METAMORPH_HELMET_ITEM = registerItem("metamorph_helmet", new MetamorphHeadItem(
         new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
               .delayedComponent(DataComponents.ENCHANTMENTS, ctx -> makeEnchantComponent(new EnchantmentInstance(ctx.getOrThrow(Enchantments.VANISHING_CURSE), 1)))
               .component(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.ENCHANTMENTS, true))
   ));
   
   static{
      TUFF_FOODS.put(Items.TUFF, new Pair<>(2.0f, 20));
      TUFF_FOODS.put(Items.TUFF_SLAB, new Pair<>(1.0f, 10));
      TUFF_FOODS.put(Items.TUFF_STAIRS, new Pair<>(2.5f, 21));
      TUFF_FOODS.put(Items.TUFF_WALL, new Pair<>(2.5f, 21));
      TUFF_FOODS.put(Items.POLISHED_TUFF, new Pair<>(3.0f, 22));
      TUFF_FOODS.put(Items.POLISHED_TUFF_SLAB, new Pair<>(2.0f, 16));
      TUFF_FOODS.put(Items.POLISHED_TUFF_STAIRS, new Pair<>(3.5f, 23));
      TUFF_FOODS.put(Items.POLISHED_TUFF_WALL, new Pair<>(3.5f, 23));
      TUFF_FOODS.put(Items.TUFF_BRICKS, new Pair<>(4.0f, 24));
      TUFF_FOODS.put(Items.CHISELED_TUFF, new Pair<>(3.75f, 23));
      TUFF_FOODS.put(Items.TUFF_BRICK_SLAB, new Pair<>(3.0f, 19));
      TUFF_FOODS.put(Items.TUFF_BRICK_STAIRS, new Pair<>(4.5f, 25));
      TUFF_FOODS.put(Items.TUFF_BRICK_WALL, new Pair<>(4.5f, 25));
      TUFF_FOODS.put(Items.CHISELED_TUFF_BRICKS, new Pair<>(5.0f, 25));
      
      IRON_FOODS.put(Items.IRON_NUGGET, new Pair<>(0.4f, 2));
      IRON_FOODS.put(Items.RAW_IRON, new Pair<>(2.0f, 20));
      IRON_FOODS.put(Items.IRON_INGOT, new Pair<>(4.0f, 20));
      IRON_FOODS.put(Items.IRON_ORE, new Pair<>(3.0f, 25));
      IRON_FOODS.put(Items.DEEPSLATE_IRON_ORE, new Pair<>(4.0f, 25));
      IRON_FOODS.put(Items.IRON_SHOVEL, new Pair<>(5f, 15));
      IRON_FOODS.put(Items.IRON_SPEAR, new Pair<>(5f, 15));
      IRON_FOODS.put(Items.IRON_PICKAXE, new Pair<>(15f, 35));
      IRON_FOODS.put(Items.IRON_AXE, new Pair<>(15f, 35));
      IRON_FOODS.put(Items.IRON_HOE, new Pair<>(10f, 25));
      IRON_FOODS.put(Items.IRON_SWORD, new Pair<>(10f, 25));
      IRON_FOODS.put(Items.IRON_HELMET, new Pair<>(25f, 55));
      IRON_FOODS.put(Items.IRON_NAUTILUS_ARMOR, new Pair<>(25f, 55));
      IRON_FOODS.put(Items.IRON_CHESTPLATE, new Pair<>(40f, 85));
      IRON_FOODS.put(Items.IRON_LEGGINGS, new Pair<>(35f, 75));
      IRON_FOODS.put(Items.IRON_BOOTS, new Pair<>(20f, 45));
      IRON_FOODS.put(Items.IRON_HORSE_ARMOR, new Pair<>(35f, 75));
      IRON_FOODS.put(Items.RAW_IRON_BLOCK, new Pair<>(15f, 150));
      IRON_FOODS.put(Items.IRON_BLOCK, new Pair<>(25f, 125));
      IRON_FOODS.put(Items.IRON_BARS, new Pair<>(2f, 10));
      IRON_FOODS.put(Items.IRON_DOOR, new Pair<>(6f, 30));
      IRON_FOODS.put(Items.IRON_TRAPDOOR, new Pair<>(10f, 50));
      IRON_FOODS.put(Items.IRON_CHAIN, new Pair<>(5f, 20));
      IRON_FOODS.put(Items.LANTERN, new Pair<>(3.75f, 15));
      IRON_FOODS.put(Items.SOUL_LANTERN, new Pair<>(4f, 16));
      IRON_FOODS.put(Items.HEAVY_WEIGHTED_PRESSURE_PLATE, new Pair<>(5f, 25));
      IRON_FOODS.put(Items.CAULDRON, new Pair<>(19f, 95));
      IRON_FOODS.put(Items.ANVIL, new Pair<>(50.0f, 200));
      IRON_FOODS.put(Items.CHIPPED_ANVIL, new Pair<>(50.0f, 225));
      IRON_FOODS.put(Items.DAMAGED_ANVIL, new Pair<>(50.0f, 250));
      IRON_FOODS.put(Items.MINECART, new Pair<>(25f, 55));
      IRON_FOODS.put(Items.SHEARS, new Pair<>(10f, 25));
      IRON_FOODS.put(Items.HOPPER, new Pair<>(13f, 65));
      IRON_FOODS.put(Items.COMPASS, new Pair<>(11f, 55));
      IRON_FOODS.put(Items.ACTIVATOR_RAIL, new Pair<>(6f, 24));
      IRON_FOODS.put(Items.DETECTOR_RAIL, new Pair<>(6.5f, 26));
      IRON_FOODS.put(Items.RAIL, new Pair<>(1.6f, 8));
      IRON_FOODS.put(Items.HOPPER_MINECART, new Pair<>(50.0f, 95));
      
      COPPER_FOODS.put(Items.RAW_COPPER, new Pair<>(1f, 20));
      COPPER_FOODS.put(Items.COPPER_NUGGET, new Pair<>(0.1f, 1));
      COPPER_FOODS.put(Items.COPPER_INGOT, new Pair<>(1.0f, 9));
      COPPER_FOODS.put(Items.COPPER_ORE, new Pair<>(1.5f, 20));
      COPPER_FOODS.put(Items.DEEPSLATE_COPPER_ORE, new Pair<>(1.5f, 15));
      COPPER_FOODS.put(Items.RAW_COPPER_BLOCK, new Pair<>(8f, 160));
      COPPER_FOODS.put(Items.COPPER_BLOCK.weathering().unaffected(), new Pair<>(2f, 16));
      COPPER_FOODS.put(Items.CHISELED_COPPER.weathering().unaffected(), new Pair<>(3.25f, 23));
      COPPER_FOODS.put(Items.COPPER_GRATE.weathering().unaffected(), new Pair<>(3.0f, 22));
      COPPER_FOODS.put(Items.CUT_COPPER.weathering().unaffected(), new Pair<>(3.0f, 22));
      COPPER_FOODS.put(Items.CUT_COPPER_STAIRS.weathering().unaffected(), new Pair<>(3.5f, 25));
      COPPER_FOODS.put(Items.CUT_COPPER_SLAB.weathering().unaffected(), new Pair<>(2.0f, 15));
      COPPER_FOODS.put(Items.COPPER_DOOR.weathering().unaffected(), new Pair<>(1.25f, 10));
      COPPER_FOODS.put(Items.COPPER_TRAPDOOR.weathering().unaffected(), new Pair<>(1.75f, 14));
      COPPER_FOODS.put(Items.COPPER_BULB.weathering().unaffected(), new Pair<>(4f, 29));
      COPPER_FOODS.put(Items.COPPER_BARS.weathering().unaffected(), new Pair<>(0.5f, 4));
      COPPER_FOODS.put(Items.COPPER_LANTERN.weathering().unaffected(), new Pair<>(0.9f, 8));
      COPPER_FOODS.put(Items.COPPER_CHAIN.weathering().unaffected(), new Pair<>(1.2f, 9));
      COPPER_FOODS.put(Items.COPPER_CHEST.weathering().unaffected(), new Pair<>(3.25f, 23));
      COPPER_FOODS.put(Items.COPPER_GOLEM_STATUE.weathering().unaffected(), new Pair<>(4f, 27));
      COPPER_FOODS.put(Items.LIGHTNING_ROD.weathering().unaffected(), new Pair<>(1.5f, 12));
      COPPER_FOODS.put(Items.COPPER_BLOCK.weathering().exposed(), new Pair<>(4f, 27));
      COPPER_FOODS.put(Items.CHISELED_COPPER.weathering().exposed(), new Pair<>(5.25f, 32));
      COPPER_FOODS.put(Items.COPPER_GRATE.weathering().exposed(), new Pair<>(5f, 31));
      COPPER_FOODS.put(Items.CUT_COPPER.weathering().exposed(), new Pair<>(5f, 31));
      COPPER_FOODS.put(Items.CUT_COPPER_STAIRS.weathering().exposed(), new Pair<>(5.5f, 33));
      COPPER_FOODS.put(Items.CUT_COPPER_SLAB.weathering().exposed(), new Pair<>(3.5f, 22));
      COPPER_FOODS.put(Items.COPPER_DOOR.weathering().exposed(), new Pair<>(2.75f, 19));
      COPPER_FOODS.put(Items.COPPER_TRAPDOOR.weathering().exposed(), new Pair<>(3.25f, 22));
      COPPER_FOODS.put(Items.COPPER_BULB.weathering().exposed(), new Pair<>(6.0f, 37));
      COPPER_FOODS.put(Items.COPPER_BARS.weathering().exposed(), new Pair<>(1f, 7));
      COPPER_FOODS.put(Items.COPPER_LANTERN.weathering().exposed(), new Pair<>(1.8f, 12));
      COPPER_FOODS.put(Items.COPPER_CHAIN.weathering().exposed(), new Pair<>(2.4f, 16));
      COPPER_FOODS.put(Items.COPPER_CHEST.weathering().exposed(), new Pair<>(5.25f, 32));
      COPPER_FOODS.put(Items.COPPER_GOLEM_STATUE.weathering().exposed(), new Pair<>(6f, 35));
      COPPER_FOODS.put(Items.LIGHTNING_ROD.weathering().exposed(), new Pair<>(3f, 21));
      COPPER_FOODS.put(Items.COPPER_BLOCK.weathering().weathered(), new Pair<>(6f, 34));
      COPPER_FOODS.put(Items.CHISELED_COPPER.weathering().weathered(), new Pair<>(7.25f, 38));
      COPPER_FOODS.put(Items.COPPER_GRATE.weathering().weathered(), new Pair<>(7f, 37));
      COPPER_FOODS.put(Items.CUT_COPPER.weathering().weathered(), new Pair<>(7f, 37));
      COPPER_FOODS.put(Items.CUT_COPPER_STAIRS.weathering().weathered(), new Pair<>(7.5f, 39));
      COPPER_FOODS.put(Items.CUT_COPPER_SLAB.weathering().weathered(), new Pair<>(5f, 26));
      COPPER_FOODS.put(Items.COPPER_DOOR.weathering().weathered(), new Pair<>(4.25f, 24));
      COPPER_FOODS.put(Items.COPPER_TRAPDOOR.weathering().weathered(), new Pair<>(4.75f, 27));
      COPPER_FOODS.put(Items.COPPER_BULB.weathering().weathered(), new Pair<>(9f, 48));
      COPPER_FOODS.put(Items.COPPER_BARS.weathering().weathered(), new Pair<>(1.5f, 9));
      COPPER_FOODS.put(Items.COPPER_LANTERN.weathering().weathered(), new Pair<>(2.7f, 15));
      COPPER_FOODS.put(Items.COPPER_CHAIN.weathering().weathered(), new Pair<>(3.6f, 20));
      COPPER_FOODS.put(Items.COPPER_CHEST.weathering().weathered(), new Pair<>(7.25f, 28));
      COPPER_FOODS.put(Items.COPPER_GOLEM_STATUE.weathering().weathered(), new Pair<>(8f, 41));
      COPPER_FOODS.put(Items.LIGHTNING_ROD.weathering().weathered(), new Pair<>(3.5f, 26));
      COPPER_FOODS.put(Items.COPPER_BLOCK.weathering().oxidized(), new Pair<>(8f, 40));
      COPPER_FOODS.put(Items.CHISELED_COPPER.weathering().oxidized(), new Pair<>(9.25f, 44));
      COPPER_FOODS.put(Items.COPPER_GRATE.weathering().oxidized(), new Pair<>(9f, 43));
      COPPER_FOODS.put(Items.CUT_COPPER.weathering().oxidized(), new Pair<>(9f, 43));
      COPPER_FOODS.put(Items.CUT_COPPER_STAIRS.weathering().oxidized(), new Pair<>(9.5f, 45));
      COPPER_FOODS.put(Items.CUT_COPPER_SLAB.weathering().oxidized(), new Pair<>(6.5f, 31));
      COPPER_FOODS.put(Items.COPPER_DOOR.weathering().oxidized(), new Pair<>(5.75f, 29));
      COPPER_FOODS.put(Items.COPPER_TRAPDOOR.weathering().oxidized(), new Pair<>(6.25f, 31));
      COPPER_FOODS.put(Items.COPPER_BULB.weathering().oxidized(), new Pair<>(10.0f, 48));
      COPPER_FOODS.put(Items.COPPER_BARS.weathering().oxidized(), new Pair<>(2f, 10));
      COPPER_FOODS.put(Items.COPPER_LANTERN.weathering().oxidized(), new Pair<>(3.6f, 18));
      COPPER_FOODS.put(Items.COPPER_CHAIN.weathering().oxidized(), new Pair<>(4.8f, 24));
      COPPER_FOODS.put(Items.COPPER_CHEST.weathering().oxidized(), new Pair<>(9.25f, 44));
      COPPER_FOODS.put(Items.COPPER_GOLEM_STATUE.weathering().oxidized(), new Pair<>(10f, 47));
      COPPER_FOODS.put(Items.LIGHTNING_ROD.weathering().oxidized(), new Pair<>(6f, 30));
      COPPER_FOODS.put(Items.COPPER_SHOVEL, new Pair<>(3f, 15));
      COPPER_FOODS.put(Items.COPPER_SPEAR, new Pair<>(3f, 15));
      COPPER_FOODS.put(Items.COPPER_HOE, new Pair<>(6f, 20));
      COPPER_FOODS.put(Items.COPPER_SWORD, new Pair<>(6f, 20));
      COPPER_FOODS.put(Items.COPPER_PICKAXE, new Pair<>(9f, 25));
      COPPER_FOODS.put(Items.COPPER_AXE, new Pair<>(9f, 25));
      COPPER_FOODS.put(Items.COPPER_BOOTS, new Pair<>(12f, 30));
      COPPER_FOODS.put(Items.COPPER_HELMET, new Pair<>(15f, 35));
      COPPER_FOODS.put(Items.COPPER_NAUTILUS_ARMOR, new Pair<>(15f, 35));
      COPPER_FOODS.put(Items.COPPER_LEGGINGS, new Pair<>(21f, 40));
      COPPER_FOODS.put(Items.COPPER_HORSE_ARMOR, new Pair<>(21f, 40));
      COPPER_FOODS.put(Items.COPPER_CHESTPLATE, new Pair<>(24f, 45));
   }
   
   private static ArchetypeAbility register(ArchetypeAbility ability){
      Registry.register(ABILITIES, archetypesId(ability.id()), ability);
      return ability;
   }
   
   private static Archetype register(Archetype archetype){
      Registry.register(ARCHETYPES, archetypesId(archetype.id()), archetype);
      return archetype;
   }
   
   private static SubArchetype register(SubArchetype subarchetype){
      Registry.register(SUBARCHETYPES, archetypesId(subarchetype.getId()), subarchetype);
      return subarchetype;
   }
   
   private static Item registerItem(String id, Item item){
      Identifier identifier = archetypesId(id);
      if(item instanceof AbilityItem){
         ABILITY_ITEM_KEY_MAP.put(item, ResourceKey.create(Registries.ITEM, identifier));
      }
      Registry.register(ITEMS, identifier, Registry.register(BuiltInRegistries.ITEM, identifier, item));
      return item;
   }
   
   private static IConfigSetting<?> registerConfigSetting(IConfigSetting<?> setting){
      Registry.register(CONFIG_SETTINGS, archetypesId(setting.getId()), setting);
      return setting;
   }
   
   public static <T extends Entity> EntityType<T> registerEntity(String id, EntityType.Builder<T> builder){
      Identifier identifier = archetypesId(id);
      EntityType<T> entityType = builder.build(ResourceKey.create(Registries.ENTITY_TYPE, identifier));
      Registry.register(BuiltInRegistries.ENTITY_TYPE, archetypesId(id), entityType);
      PolymerEntityUtils.registerType(entityType);
      return entityType;
   }
   
   private static LoginCallback registerCallback(LoginCallback callback){
      return Registry.register(BorisLib.LOGIN_CALLBACKS, callback.getId(), callback);
   }
   
   public static void initialize(){
      PolymerResourcePackUtils.addModAssets(MOD_ID);
      
      FabricDefaultAttributeRegistry.register(CREAKING_HEART_ENTITY, CreakingHeartEntity.createHeartAttributes());
      
      PolymerItemUtils.CONTEXT_ITEM_CHECK.register(
            (itemInstance, context) -> {
               if(context == null || context.get(PacketContext.GAME_PROFILE) == null) return false;
               UUID player = context.get(PacketContext.GAME_PROFILE).id();
               if(player == null) return false;
               PlayerArchetypeData profile = profile(player);
               if(profile.hasAbility(TUFF_EATER) && TUFF_FOODS.containsKey(itemInstance.typeHolder().value())){
                  return true;
               }else if(profile.hasAbility(IRON_EATER) && IRON_FOODS.containsKey(itemInstance.typeHolder().value())){
                  return true;
               }else if(profile.hasAbility(COPPER_EATER) && COPPER_FOODS.containsKey(itemInstance.typeHolder().value())){
                  return true;
               }else if(profile.hasAbility(SLIME_TOTEM) && itemInstance.is(SLIME_GROW_ITEMS)){
                  return true;
               }else if(profile.hasAbility(MAGMA_TOTEM) && itemInstance.is(MAGMA_CUBE_GROW_ITEMS)){
                  return true;
               }else if(profile.hasAbility(SULFUR_TOTEM) && itemInstance.is(SULFUR_GROW_ITEMS)){
                  return true;
               }else if(profile.hasAbility(FUNGUS_SPEED_BOOST) && itemInstance.is(Items.WARPED_FUNGUS)){
                  return true;
               }else if(profile.hasAbility(WAX_SHIELD) && itemInstance.is(Items.HONEYCOMB)){
                  return true;
               }else if(profile.hasAbility(BERRY_EATER) && (itemInstance.is(Items.GLOW_BERRIES) || itemInstance.is(Items.SWEET_BERRIES))){
                  return true;
               }else if(profile.hasAbility(METAMORPH) && ArchetypeRegistry.METAMORPH_ITEMS.values().stream().anyMatch(itemInstance::is)){
                  return true;
               }
               return false;
            }
      );
      
      PolymerItemUtils.ITEM_MODIFICATION_EVENT.register(
            (original, client, context) -> {
               if(context == null || context.get(PacketContext.GAME_PROFILE) == null) return client;
               String metamorphHelmet = archetypes$ITEM_DATA.getStringProperty(original, METAMORPH_HELMET_TYPE);
               if(!metamorphHelmet.isEmpty()){
                  ArchetypeUtils.addMetamorphHelmetTags(client, MetamorphTypes.fromString(metamorphHelmet));
               }
               UUID player = context.get(PacketContext.GAME_PROFILE).id();
               if(player == null) return client;
               PlayerArchetypeData profile = profile(player);
               HashMap<Item, Pair<Float, Integer>> map = null;
               float healthMod = 1.0f;
               float durationMod = 1.0f;
               
               if(profile.hasAbility(TUFF_EATER) && TUFF_FOODS.containsKey(original.getItem())){
                  map = TUFF_FOODS;
                  healthMod = (float) CONFIG.getDouble(ArchetypeRegistry.TUFF_FOOD_HEALTH_MODIFIER);
                  durationMod = (float) CONFIG.getDouble(ArchetypeRegistry.TUFF_FOOD_DURATION_MODIFIER);
               }
               if(profile.hasAbility(COPPER_EATER) && COPPER_FOODS.containsKey(original.getItem())){
                  map = COPPER_FOODS;
                  healthMod = (float) CONFIG.getDouble(ArchetypeRegistry.COPPER_FOOD_HEALTH_MODIFIER);
                  durationMod = (float) CONFIG.getDouble(ArchetypeRegistry.COPPER_FOOD_DURATION_MODIFIER);
               }
               if(profile.hasAbility(IRON_EATER) && IRON_FOODS.containsKey(original.getItem())){
                  map = IRON_FOODS;
                  healthMod = (float) CONFIG.getDouble(ArchetypeRegistry.IRON_FOOD_HEALTH_MODIFIER);
                  durationMod = (float) CONFIG.getDouble(ArchetypeRegistry.IRON_FOOD_DURATION_MODIFIER);
               }
               
               if(map != null){
                  ItemLore lore = client.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
                  List<Component> currentLore = new ArrayList<>(lore.styledLines());
                  Pair<Float, Integer> pair = map.get(original.getItem());
                  currentLore.add(getFoodLoreLine(new Pair<>(pair.getFirst() * healthMod, Math.round(pair.getSecond() * durationMod))));
                  client.set(DataComponents.LORE, new ItemLore(currentLore, currentLore));
               }
               
               if(profile.hasAbility(SLIME_TOTEM) && original.is(ArchetypeRegistry.SLIME_GROW_ITEMS)){
                  ItemLore lore = client.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
                  List<Component> currentLore = new ArrayList<>(lore.styledLines());
                  currentLore.add(getGrowItemLoreLine());
                  client.set(DataComponents.LORE, new ItemLore(currentLore, currentLore));
               }
               
               if(profile.hasAbility(MAGMA_TOTEM) && original.is(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS)){
                  ItemLore lore = client.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
                  List<Component> currentLore = new ArrayList<>(lore.styledLines());
                  currentLore.add(getGrowItemLoreLine());
                  client.set(DataComponents.LORE, new ItemLore(currentLore, currentLore));
               }
               
               if(profile.hasAbility(SULFUR_TOTEM) && original.is(ArchetypeRegistry.SULFUR_GROW_ITEMS)){
                  ItemLore lore = client.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
                  List<Component> currentLore = new ArrayList<>(lore.styledLines());
                  currentLore.add(getGrowItemLoreLine());
                  client.set(DataComponents.LORE, new ItemLore(currentLore, currentLore));
               }
               
               if(profile.hasAbility(FUNGUS_SPEED_BOOST) && original.is(Items.WARPED_FUNGUS)){
                  ItemLore lore = client.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
                  List<Component> currentLore = new ArrayList<>(lore.styledLines());
                  currentLore.add(fungusLoreLine());
                  client.set(DataComponents.LORE, new ItemLore(currentLore, currentLore));
               }
               
               if(profile.hasAbility(WAX_SHIELD) && original.is(Items.HONEYCOMB)){
                  ItemLore lore = client.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
                  List<Component> currentLore = new ArrayList<>(lore.styledLines());
                  currentLore.add(waxLoreLine());
                  client.set(DataComponents.LORE, new ItemLore(currentLore, currentLore));
               }
               
               if(profile.hasAbility(BERRY_EATER) && original.is(Items.GLOW_BERRIES)){
                  ItemLore lore = client.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
                  List<Component> currentLore = new ArrayList<>(lore.styledLines());
                  currentLore.add(glowBerryLoreLine());
                  client.set(DataComponents.LORE, new ItemLore(currentLore, currentLore));
               }
               
               if(profile.hasAbility(BERRY_EATER) && original.is(Items.SWEET_BERRIES)){
                  ItemLore lore = client.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
                  List<Component> currentLore = new ArrayList<>(lore.styledLines());
                  currentLore.add(sweetBerryLoreLine());
                  client.set(DataComponents.LORE, new ItemLore(currentLore, currentLore));
               }
               
               if(profile.hasAbility(METAMORPH) && ArchetypeRegistry.METAMORPH_ITEMS.values().stream().anyMatch(original::is)){
                  ItemLore lore = client.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
                  List<Component> currentLore = new ArrayList<>(lore.styledLines());
                  currentLore.add(metamorphLoreLine());
                  client.set(DataComponents.LORE, new ItemLore(currentLore, currentLore));
               }
               
               return client;
            }
      );
      
      final CreativeModeTab ITEM_GROUP = PolymerCreativeModeTabUtils.builder().title(Component.translatable("itemGroup.archetype_items")).icon(() -> new ItemStack(CHANGE_ITEM)).displayItems((displayContext, entries) -> {
         for(Item item : ITEMS){
            entries.accept(new ItemStack(item));
         }
      }).build();
      
      PolymerCreativeModeTabUtils.registerPolymerCreativeModeTab(archetypesId("archetype_items"), ITEM_GROUP);
   }
   
   private static Component getFoodLoreLine(Pair<Float, Integer> pair){
      DecimalFormat df = new DecimalFormat("0.###");
      return TextUtils.removeItalics(Component.literal("").withStyle(ChatFormatting.DARK_PURPLE)
            .append(Component.translatable("text.ancestralarchetypes.consume_1"))
            .append(Component.literal(df.format(pair.getSecond() / 20.0) + " ").withStyle(ChatFormatting.GOLD))
            .append(Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD))
            .append(Component.translatable("text.ancestralarchetypes.consume_2"))
            .append(Component.literal(df.format(pair.getFirst() / 2.0) + " ").withStyle(ChatFormatting.RED))
            .append(Component.translatable("text.ancestralarchetypes.hearts").withStyle(ChatFormatting.RED))
      );
   }
   
   private static Component getGrowItemLoreLine(){
      DecimalFormat df = new DecimalFormat("0.###");
      int eatTime = CONFIG.getInt(ArchetypeRegistry.GELATIAN_GROW_ITEM_EAT_DURATION);
      return TextUtils.removeItalics(Component.literal("").withStyle(ChatFormatting.DARK_PURPLE)
            .append(Component.translatable("text.ancestralarchetypes.consume_1"))
            .append(Component.literal(df.format(eatTime / 20.0) + " ").withStyle(ChatFormatting.GOLD))
            .append(Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD))
            .append(Component.translatable("text.ancestralarchetypes.consume_regrow"))
      );
   }
   
   private static Component fungusLoreLine(){
      DecimalFormat df = new DecimalFormat("0.###");
      int eatTime = CONFIG.getInt(ArchetypeRegistry.FUNGUS_SPEED_BOOST_CONSUME_DURATION);
      double boost = CONFIG.getDouble(ArchetypeRegistry.FUNGUS_SPEED_BOOST_MULTIPLIER);
      int boostDuration = CONFIG.getInt(ArchetypeRegistry.FUNGUS_SPEED_BOOST_DURATION);
      return TextUtils.removeItalics(Component.translatable("text.ancestralarchetypes.fungus_consume",
            Component.literal(df.format(eatTime / 20.0)).withStyle(ChatFormatting.GOLD),
            Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD),
            Component.literal(df.format(boost)).withStyle(ChatFormatting.GOLD),
            Component.literal(df.format(boostDuration / 20.0)).withStyle(ChatFormatting.GOLD),
            Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD)
      ).withStyle(ChatFormatting.DARK_PURPLE));
   }
   
   private static Component waxLoreLine(){
      DecimalFormat df = new DecimalFormat("0.###");
      int eatTime = CONFIG.getInt(ArchetypeRegistry.WAX_SHIELD_CONSUME_DURATION);
      double boost = CONFIG.getDouble(ArchetypeRegistry.WAX_SHIELD_HEALTH);
      double boostMax = CONFIG.getDouble(ArchetypeRegistry.WAX_SHIELD_MAX_HEALTH);
      int boostDuration = CONFIG.getInt(ArchetypeRegistry.WAX_SHIELD_DURATION);
      return TextUtils.removeItalics(Component.translatable("text.ancestralarchetypes.wax_consume",
            Component.literal(df.format(eatTime / 20.0)).withStyle(ChatFormatting.GOLD),
            Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD),
            Component.literal(df.format(boost / 2.0)).withStyle(ChatFormatting.RED),
            Component.translatable("text.ancestralarchetypes.hearts").withStyle(ChatFormatting.RED),
            Component.literal(df.format(boostMax / 2.0)).withStyle(ChatFormatting.RED),
            Component.translatable("text.ancestralarchetypes.hearts").withStyle(ChatFormatting.RED),
            Component.literal(df.format(boostDuration / 20.0)).withStyle(ChatFormatting.GOLD),
            Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD)
      ).withStyle(ChatFormatting.DARK_PURPLE));
   }
   
   private static Component sweetBerryLoreLine(){
      DecimalFormat df = new DecimalFormat("0.###");
      int damageTime = CONFIG.getInt(ArchetypeRegistry.BERRY_EATER_SWEET_STRENGTH_DURATION);
      int regenTime = CONFIG.getInt(ArchetypeRegistry.BERRY_EATER_SWEET_REGEN_DURATION);
      double damageBoost = CONFIG.getDouble(ArchetypeRegistry.BERRY_EATER_SWEET_STRENGTH);
      double regenRate = CONFIG.getDouble(ArchetypeRegistry.BERRY_EATER_SWEET_REGEN_RATE);
      double heartsHealed = regenRate * regenTime / 2.0;
      return TextUtils.removeItalics(Component.translatable("text.ancestralarchetypes.sweet_berry_consume",
            Component.literal(df.format(damageBoost * 100.0)).withStyle(ChatFormatting.RED),
            Component.literal(df.format(damageTime / 20.0)).withStyle(ChatFormatting.GOLD),
            Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD),
            Component.literal(df.format(heartsHealed)).withStyle(ChatFormatting.RED),
            Component.translatable("text.ancestralarchetypes.hearts").withStyle(ChatFormatting.RED),
            Component.literal(df.format(regenTime / 20.0)).withStyle(ChatFormatting.GOLD),
            Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD)
      ).withStyle(ChatFormatting.DARK_PURPLE));
   }
   
   private static Component glowBerryLoreLine(){
      DecimalFormat df = new DecimalFormat("0.###");
      int speedTime = CONFIG.getInt(ArchetypeRegistry.BERRY_EATER_GLOW_SPEED_DURATION);
      int absTime = CONFIG.getInt(ArchetypeRegistry.BERRY_EATER_GLOW_ABSORPTION_DURATION);
      double speedBoost = CONFIG.getDouble(ArchetypeRegistry.BERRY_EATER_GLOW_SPEED);
      double absAmount = CONFIG.getDouble(ArchetypeRegistry.BERRY_EATER_GLOW_ABSORPTION);
      return TextUtils.removeItalics(Component.translatable("text.ancestralarchetypes.glow_berry_consume",
            Component.literal(df.format(speedBoost * 100.0)).withStyle(ChatFormatting.RED),
            Component.literal(df.format(speedTime / 20.0)).withStyle(ChatFormatting.GOLD),
            Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD),
            Component.literal(df.format(absAmount / 2.0)).withStyle(ChatFormatting.RED),
            Component.translatable("text.ancestralarchetypes.hearts").withStyle(ChatFormatting.RED),
            Component.literal(df.format(absTime / 20.0)).withStyle(ChatFormatting.GOLD),
            Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD)
      ).withStyle(ChatFormatting.DARK_PURPLE));
   }
   
   private static Component metamorphLoreLine(){
      DecimalFormat df = new DecimalFormat("0.###");
      int eatTime = CONFIG.getInt(ArchetypeRegistry.METAMORPH_EAT_DURATION);
      int abilityDuration = CONFIG.getInt(ArchetypeRegistry.METAMORPH_ABILITY_DURATION);
      return TextUtils.removeItalics(Component.translatable("text.ancestralarchetypes.metamorph_consume",
            Component.literal(df.format(eatTime / 20.0)).withStyle(ChatFormatting.GOLD),
            Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD),
            Component.literal(df.format(abilityDuration / 20.0)).withStyle(ChatFormatting.GOLD),
            Component.translatable("text.ancestralarchetypes.seconds").withStyle(ChatFormatting.GOLD)
      ).withStyle(ChatFormatting.DARK_PURPLE));
   }
}
