package net.borisshoes.ancestralarchetypes;

import com.mojang.serialization.Lifecycle;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.items.*;
import net.borisshoes.ancestralarchetypes.utils.ConfigUtils;
import net.borisshoes.ancestralarchetypes.utils.MiscUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.*;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class ArchetypeRegistry {
   public static final Registry<ArchetypeAbility> ABILITIES = new SimpleRegistry<>(RegistryKey.ofRegistry(Identifier.of(MOD_ID,"ability")), Lifecycle.stable());
   public static final Registry<Archetype> ARCHETYPES = new SimpleRegistry<>(RegistryKey.ofRegistry(Identifier.of(MOD_ID,"archetype")), Lifecycle.stable());
   public static final Registry<SubArchetype> SUBARCHETYPES = new SimpleRegistry<>(RegistryKey.ofRegistry(Identifier.of(MOD_ID,"subarchetype")), Lifecycle.stable());
   public static final Registry<Item> ITEMS = new SimpleRegistry<>(RegistryKey.ofRegistry(Identifier.of(MOD_ID,"item")), Lifecycle.stable());
   public static final Registry<ArchetypeConfig.ConfigSetting<?>> CONFIG_SETTINGS = new SimpleRegistry<>(RegistryKey.ofRegistry(Identifier.of(MOD_ID,"config_settings")), Lifecycle.stable());
   public static final HashMap<Item, Pair<Float,Integer>> TUFF_FOODS = new HashMap<>();
   public static final HashMap<Item, Pair<Float,Integer>> COPPER_FOODS = new HashMap<>();
   public static final HashMap<Item, Pair<Float,Integer>> IRON_FOODS = new HashMap<>();
   
   public static final TagKey<Item> CARNIVORE_FOODS = TagKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,"carnivore_foods"));
   public static final TagKey<Item> SLIME_GROW_ITEMS = TagKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,"slime_grow_items"));
   public static final TagKey<Item> MAGMA_CUBE_GROW_ITEMS = TagKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID,"magma_cube_grow_items"));
   public static final TagKey<DamageType> NO_STARTLE = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(MOD_ID,"no_startle"));
   
   public static final ArchetypeAbility GOOD_SWIMMER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("good_swimmer").setDisplayStack(new ItemStack(Items.COD)).build());
   public static final ArchetypeAbility GREAT_SWIMMER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("great_swimmer").setDisplayStack(new ItemStack(Items.TROPICAL_FISH)).build());
   public static final ArchetypeAbility DRIES_OUT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("dries_out").setDisplayStack(new ItemStack(Items.SPONGE)).build());
   public static final ArchetypeAbility IMPALE_VULNERABLE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("impale_vulnerable").setDisplayStack(new ItemStack(Items.TRIDENT)).build());
   public static final ArchetypeAbility REGEN_WHEN_LOW = register(new ArchetypeAbility.ArchetypeAbilityBuilder("regen_when_low").setDisplayStack(new ItemStack(Items.GOLDEN_APPLE)).build());
   public static final ArchetypeAbility FIRE_IMMUNE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("fire_immune").setDisplayStack(new ItemStack(Items.MAGMA_CREAM)).build());
   public static final ArchetypeAbility DAMAGED_BY_COLD = register(new ArchetypeAbility.ArchetypeAbilityBuilder("damaged_by_cold").setDisplayStack(new ItemStack(Items.SNOWBALL)).build());
   public static final ArchetypeAbility FIREBALL_VOLLEY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("fireball_volley").setDisplayStack(new ItemStack(Items.FIRE_CHARGE)).setActive().build());
   public static final ArchetypeAbility POTION_BREWER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("potion_brewer").setDisplayStack(new ItemStack(Items.CAULDRON)).setActive().build());
   public static final ArchetypeAbility BOUNCY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("bouncy").setDisplayStack(new ItemStack(Items.SLIME_BLOCK)).build());
   public static final ArchetypeAbility JUMPY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("jumpy").setDisplayStack(new ItemStack(Items.RABBIT_FOOT)).build());
   public static final ArchetypeAbility SLIME_TOTEM = register(new ArchetypeAbility.ArchetypeAbilityBuilder("slime_totem").setDisplayStack(new ItemStack(Items.TOTEM_OF_UNDYING)).build());
   public static final ArchetypeAbility MAGMA_TOTEM = register(new ArchetypeAbility.ArchetypeAbilityBuilder("magma_totem").setDisplayStack(new ItemStack(Items.TOTEM_OF_UNDYING)).build());
   public static final ArchetypeAbility INSATIABLE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("insatiable").setDisplayStack(new ItemStack(Items.ROTTEN_FLESH)).build());
   public static final ArchetypeAbility SLOW_FALLER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("slow_faller").setDisplayStack(new ItemStack(Items.FEATHER)).build());
   public static final ArchetypeAbility PROJECTILE_RESISTANT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("projectile_resistant").setDisplayStack(new ItemStack(Items.SHIELD)).build());
   public static final ArchetypeAbility SOFT_HITTER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("soft_hitter").setDisplayStack(new ItemStack(Items.WOODEN_SWORD)).build());
   public static final ArchetypeAbility WIND_CHARGE_VOLLEY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("wind_charge_volley").setDisplayStack(new ItemStack(Items.WIND_CHARGE)).setActive().build());
   public static final ArchetypeAbility GLIDER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("glider").setDisplayStack(new ItemStack(Items.ELYTRA)).setActive().build());
   public static final ArchetypeAbility NO_REGEN = register(new ArchetypeAbility.ArchetypeAbilityBuilder("no_regen").setDisplayStack(new ItemStack(Items.POISONOUS_POTATO)).build());
   public static final ArchetypeAbility COPPER_EATER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("copper_eater").setDisplayStack(new ItemStack(Items.COPPER_INGOT)).build());
   public static final ArchetypeAbility HEALTH_BASED_SPRINT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("health_based_sprint").setDisplayStack(new ItemStack(Items.DIAMOND_BOOTS)).build());
   public static final ArchetypeAbility IRON_EATER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("iron_eater").setDisplayStack(new ItemStack(Items.IRON_INGOT)).build());
   public static final ArchetypeAbility TUFF_EATER = register(new ArchetypeAbility.ArchetypeAbilityBuilder("tuff_eater").setDisplayStack(new ItemStack(Items.TUFF)).build());
   public static final ArchetypeAbility HALF_SIZED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("half_sized").setDisplayStack(new ItemStack(Items.LEATHER_HELMET)).build());
   public static final ArchetypeAbility TALL_SIZED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("tall_sized").setDisplayStack(new ItemStack(Items.CHAINMAIL_HELMET)).build());
   public static final ArchetypeAbility GIANT_SIZED = register(new ArchetypeAbility.ArchetypeAbilityBuilder("giant_sized").setDisplayStack(new ItemStack(Items.IRON_HELMET)).setOverrides(TALL_SIZED).build());
   public static final ArchetypeAbility LONG_ARMS = register(new ArchetypeAbility.ArchetypeAbilityBuilder("long_arms").setDisplayStack(new ItemStack(Items.LEVER)).build());
   public static final ArchetypeAbility REDUCED_KNOCKBACK = register(new ArchetypeAbility.ArchetypeAbilityBuilder("reduced_knockback").setDisplayStack(new ItemStack(Items.IRON_CHESTPLATE)).build());
   public static final ArchetypeAbility INCREASED_KNOCKBACK = register(new ArchetypeAbility.ArchetypeAbilityBuilder("increased_knockback").setDisplayStack(new ItemStack(Items.LEATHER_CHESTPLATE)).build());
   public static final ArchetypeAbility HASTY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("hasty").setDisplayStack(new ItemStack(Items.GOLDEN_PICKAXE)).build());
   public static final ArchetypeAbility HALVED_FALL_DAMAGE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("halved_fall_damage").setDisplayStack(new ItemStack(Items.LIGHT_GRAY_WOOL)).build());
   public static final ArchetypeAbility NO_FALL_DAMAGE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("no_fall_damage").setDisplayStack(new ItemStack(Items.WHITE_WOOL)).setOverrides(HALVED_FALL_DAMAGE).build());
   public static final ArchetypeAbility CARNIVORE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("carnivore").setDisplayStack(new ItemStack(Items.BEEF)).build());
   public static final ArchetypeAbility CAT_SCARE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("cat_scare").setDisplayStack(new ItemStack(Items.PHANTOM_MEMBRANE)).build());
   public static final ArchetypeAbility SNEAK_ATTACK = register(new ArchetypeAbility.ArchetypeAbilityBuilder("sneak_attack").setDisplayStack(new ItemStack(Items.DIAMOND_SWORD)).build());
   public static final ArchetypeAbility SPEEDY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("speedy").setDisplayStack(new ItemStack(Items.GOLDEN_BOOTS)).build());
   public static final ArchetypeAbility STUNNED_BY_DAMAGE = register(new ArchetypeAbility.ArchetypeAbilityBuilder("stunned_by_damage").setDisplayStack(new ItemStack(Items.CHAINMAIL_CHESTPLATE)).build());
   public static final ArchetypeAbility HORSE_SPIRIT_MOUNT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("horse_spirit_mount").setDisplayStack(new ItemStack(Items.GOLDEN_HORSE_ARMOR)).setActive().build());
   public static final ArchetypeAbility DONKEY_SPIRIT_MOUNT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("donkey_spirit_mount").setDisplayStack(new ItemStack(Items.CHEST)).setActive().build());
   public static final ArchetypeAbility MOONLIT = register(new ArchetypeAbility.ArchetypeAbilityBuilder("moonlit").setDisplayStack(new ItemStack(Items.SEA_LANTERN)).build());
   public static final ArchetypeAbility ANTIVENOM = register(new ArchetypeAbility.ArchetypeAbilityBuilder("antivenom").setDisplayStack(new ItemStack(Items.SPIDER_EYE)).build());
   public static final ArchetypeAbility SLIPPERY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("slippery").setDisplayStack(new ItemStack(Items.PHANTOM_MEMBRANE)).build());
   public static final ArchetypeAbility SNEAKY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("sneaky").setDisplayStack(new ItemStack(Items.LEATHER_BOOTS)).build());
   public static final ArchetypeAbility WITHERING = register(new ArchetypeAbility.ArchetypeAbilityBuilder("withering").setDisplayStack(new ItemStack(Items.WITHER_ROSE)).build());
   public static final ArchetypeAbility THORNY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("thorny").setDisplayStack(new ItemStack(Items.PRISMARINE_SHARD)).build());
   public static final ArchetypeAbility GUARDIAN_RAY = register(new ArchetypeAbility.ArchetypeAbilityBuilder("guardian_ray").setDisplayStack(new ItemStack(Items.PRISMARINE_CRYSTALS)).setActive().build());
   
   public static final Archetype AQUARIAN = register(new Archetype("aquarian", new ItemStack(Items.TROPICAL_FISH), 0x0f89f0, GOOD_SWIMMER, DRIES_OUT, IMPALE_VULNERABLE, SLIPPERY));
   public static final Archetype INFERNAL = register(new Archetype("infernal", new ItemStack(Items.CRIMSON_NYLIUM), 0xe03f24, FIRE_IMMUNE, DAMAGED_BY_COLD));
   public static final Archetype SWAMPER = register(new Archetype("swamper", new ItemStack(Items.SLIME_BLOCK), 0x4dca70, MOONLIT, ANTIVENOM));
   public static final Archetype WINDSWEPT = register(new Archetype("windswept", new ItemStack(Items.FEATHER), 0x98c9c6, SLOW_FALLER));
   public static final Archetype GOLEM = register(new Archetype("golem", new ItemStack(Items.CHISELED_STONE_BRICKS), 0xa0a0ab, NO_REGEN, HEALTH_BASED_SPRINT, PROJECTILE_RESISTANT));
   public static final Archetype FELID = register(new Archetype("felid", new ItemStack(Items.STRING), 0xc6c55c, HALVED_FALL_DAMAGE, CARNIVORE, SPEEDY));
   public static final Archetype CENTAUR = register(new Archetype("centaur", new ItemStack(Items.SADDLE), 0xbd8918, STUNNED_BY_DAMAGE));
   
   public static final SubArchetype AXOLOTL = register(new SubArchetype("axolotl", new ItemStack(Items.AXOLOTL_BUCKET), 0xe070ed, AQUARIAN, REGEN_WHEN_LOW));
   public static final SubArchetype SALMON = register(new SubArchetype("salmon", new ItemStack(Items.SALMON), 0x8f1f63, AQUARIAN, GREAT_SWIMMER));
   public static final SubArchetype BLAZE = register(new SubArchetype("blaze", new ItemStack(Items.BLAZE_POWDER), 0xe88a0f, INFERNAL, FIREBALL_VOLLEY, SLOW_FALLER));
   public static final SubArchetype WITCH = register(new SubArchetype("witch", new ItemStack(Items.CAULDRON), 0x7a0fe8, SWAMPER, POTION_BREWER));
   public static final SubArchetype SLIME = register(new SubArchetype("slime", new ItemStack(Items.SLIME_BLOCK), 0x05f905, SWAMPER, BOUNCY, JUMPY, SLIME_TOTEM, INSATIABLE));
   public static final SubArchetype MAGMA_CUBE = register(new SubArchetype("magma_cube", new ItemStack(Items.MAGMA_BLOCK), 0x943019, INFERNAL, BOUNCY, JUMPY, MAGMA_TOTEM, INSATIABLE));
   public static final SubArchetype BREEZE = register(new SubArchetype("breeze", new ItemStack(Items.WIND_CHARGE), 0x6ac1e6, WINDSWEPT, PROJECTILE_RESISTANT, SOFT_HITTER, JUMPY, WIND_CHARGE_VOLLEY));
   public static final SubArchetype PARROT = register(new SubArchetype("parrot", new ItemStack(Items.ELYTRA), 0xb7d3df, WINDSWEPT, GLIDER));
   public static final SubArchetype COPPER_GOLEM = register(new SubArchetype("copper_golem", new ItemStack(Items.COPPER_BLOCK), 0xbc814d, GOLEM, COPPER_EATER, HALF_SIZED, INCREASED_KNOCKBACK, SOFT_HITTER));
   public static final SubArchetype TUFF_GOLEM = register(new SubArchetype("tuff_golem", new ItemStack(Items.CHISELED_TUFF_BRICKS), 0x648076, GOLEM, TUFF_EATER, HASTY));
   public static final SubArchetype IRON_GOLEM = register(new SubArchetype("iron_golem", new ItemStack(Items.IRON_BLOCK), 0xbebebe, GOLEM, IRON_EATER, GIANT_SIZED, REDUCED_KNOCKBACK, LONG_ARMS));
   public static final SubArchetype CAT = register(new SubArchetype("cat", new ItemStack(Items.PHANTOM_MEMBRANE), 0xf1ce8a, FELID, CAT_SCARE, NO_FALL_DAMAGE, SNEAKY));
   public static final SubArchetype OCELOT = register(new SubArchetype("ocelot", new ItemStack(Items.CHICKEN), 0xc5b900, FELID, SNEAK_ATTACK));
   public static final SubArchetype HORSE = register(new SubArchetype("horse", new ItemStack(Items.GOLDEN_HORSE_ARMOR), 0xbda329, CENTAUR, HORSE_SPIRIT_MOUNT));
   public static final SubArchetype DONKEY = register(new SubArchetype("donkey", new ItemStack(Items.CHEST), 0x9c6d11, CENTAUR, DONKEY_SPIRIT_MOUNT));
   public static final SubArchetype WITHER_SKELETON = register(new SubArchetype("wither_skeleton", new ItemStack(Items.WITHER_SKELETON_SKULL), 0x423c3c, INFERNAL, WITHERING, TALL_SIZED));
   public static final SubArchetype GUARDIAN = register(new SubArchetype("guardian", new ItemStack(Items.PRISMARINE_BRICKS), 0x449e92, AQUARIAN, GUARDIAN_RAY, THORNY));
   
   public static final RegistryKey<? extends Registry<EquipmentAsset>> EQUIPMENT_ASSET_REGISTRY_KEY = RegistryKey.ofRegistry(Identifier.ofVanilla("equipment_asset"));
   
   public static final Item GRAPHICAL_ITEM = registerItem("graphical_item", new GraphicalItem(new Item.Settings().maxCount(64)));
   public static final Item CHANGE_ITEM = registerItem("change_item", new ChangeItem(
         new Item.Settings().maxCount(16).rarity(Rarity.EPIC)
               .component(DataComponentTypes.LORE, new LoreComponent(List.of(Text.translatable("text.ancestralarchetypes.change_item_description"))))
   ));
   public static final Item FIREBALL_VOLLEY_ITEM = registerItem(FIREBALL_VOLLEY.getId(), new FireballVolleyItem(
         new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
               .component(DataComponentTypes.CONSUMABLE, ConsumableComponent.builder()
                     .consumeSeconds(5).useAction(UseAction.BOW).consumeParticles(false)
                     .sound(Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME)).build())
               .component(DataComponentTypes.LORE, new LoreComponent(List.of(Text.translatable("text.ancestralarchetypes.fireball_volley_description"))))
   ));
   public static final Item WIND_CHARGE_VOLLEY_ITEM = registerItem(WIND_CHARGE_VOLLEY.getId(), new WindChargeVolleyItem(
         new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
               .component(DataComponentTypes.LORE, new LoreComponent(List.of(Text.translatable("text.ancestralarchetypes.wind_charge_volley_description"))))
   ));
   public static final Item POTION_BREWER_ITEM = registerItem("potion_brewer", new PortableCauldronItem(
         new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
               .component(DataComponentTypes.LORE, new LoreComponent(List.of(Text.translatable("text.ancestralarchetypes.potion_brewer_description"))))
   ));
   public static final Item GLIDER_ITEM = registerItem("glider", new WingGliderItem(
         new Item.Settings().maxCount(1).rarity(Rarity.EPIC).maxDamage(2048)
               .component(DataComponentTypes.LORE, new LoreComponent(List.of(Text.translatable("text.ancestralarchetypes.glider_description"))))
               .component(DataComponentTypes.GLIDER, Unit.INSTANCE)
               .component(DataComponentTypes.DYED_COLOR, new DyedColorComponent(16777215,false))
               .component(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.CHEST).equipSound(SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA).model(RegistryKey.of(EQUIPMENT_ASSET_REGISTRY_KEY, Identifier.of(MOD_ID,"glider"))).damageOnHurt(false).build())
   ));
   public static final Item HORSE_SPIRIT_MOUNT_ITEM = registerItem(HORSE_SPIRIT_MOUNT.getId(), new HorseSpiritMountItem(
         new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
               .component(DataComponentTypes.LORE, new LoreComponent(List.of(Text.translatable("text.ancestralarchetypes.horse_spirit_mount_description"))))
   ));
   public static final Item DONKEY_SPIRIT_MOUNT_ITEM = registerItem(DONKEY_SPIRIT_MOUNT.getId(), new DonkeySpiritMountItem(
         new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
               .component(DataComponentTypes.LORE, new LoreComponent(List.of(Text.translatable("text.ancestralarchetypes.donkey_spirit_mount_description"))))
   ));
   public static final Item GUARDIAN_RAY_ITEM = registerItem(GUARDIAN_RAY.getId(), new GuardianRayItem(
         new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
               .component(DataComponentTypes.CONSUMABLE, ConsumableComponent.builder()
                     .consumeSeconds(72000).useAction(UseAction.BOW).consumeParticles(false)
                     .sound(Registries.SOUND_EVENT.getEntry(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME)).build())
               .component(DataComponentTypes.LORE, new LoreComponent(List.of(Text.translatable("text.ancestralarchetypes.guardian_ray_description"))))
   ));
   
   public static final ArchetypeConfig.ConfigSetting<?> SPYGLASS_REVEALS_ARCHETYPE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.BooleanConfigValue("spyglassRevealsArchetype", true)));
   
   public static final ArchetypeConfig.ConfigSetting<?> SPYGLASS_REVEAL_ALERTS_PLAYER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.BooleanConfigValue("spyglassRevealAlertsPlayer", false)));
   
   public static final ArchetypeConfig.ConfigSetting<?> CAN_ALWAYS_CHANGE_ARCHETYPE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.BooleanConfigValue("canAlwaysChangeArchetype", false)));
   
   public static final ArchetypeConfig.ConfigSetting<?> REMINDERS_ON_BY_DEFAULT = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.BooleanConfigValue("remindersOnByDefault", true)));
   
   public static final ArchetypeConfig.ConfigSetting<?> SPYGLASS_INVESTIGATE_DURATION = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("spyglassInvestigateDuration", 150, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> CHANGES_PER_CHANGE_ITEM = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("changesPerChangeItem", 1, new ConfigUtils.IntegerConfigValue.IntLimits(0,1000))));
   
   public static final ArchetypeConfig.ConfigSetting<?> STARTING_ARCHETYPE_CHANGES = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("startingArchetypeChanges", 1, new ConfigUtils.IntegerConfigValue.IntLimits(0,1000))));
   
   public static final ArchetypeConfig.ConfigSetting<?> FIREBALL_COOLDOWN = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("fireballCooldown", 600, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> WIND_CHARGE_COOLDOWN = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("windChargeCooldown", 200, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> GLIDER_DURATION = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("gliderDuration", 600, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> GLIDER_COOLDOWN = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("gliderCooldown", 100, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> SPIRIT_MOUNT_KILL_COOLDOWN = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("spiritMountKillCooldown", 8000, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> DAMAGE_STUN_DURATION = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("damageStunDuration", 15, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> CAULDRON_INSTANT_EFFECT_COOLDOWN = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("cauldronInstantEffectCooldown", 900, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> GELATIAN_GROW_ITEM_EAT_DURATION = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("gelatianGrowItemEatDuration", 500, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> WITHERING_EFFECT_DURATION = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("witheringEffectDuration", 150, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> GUARDIAN_RAY_WINDUP = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("guardianRayWindup", 100, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> GUARDIAN_RAY_COOLDOWN = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("guardianRayCooldown", 600, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> GUARDIAN_RAY_DURATION = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.IntegerConfigValue("guardianRayDuration", 85, new ConfigUtils.IntegerConfigValue.IntLimits(1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> CAULDRON_DRINKABLE_COOLDOWN_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("cauldronDrinkableCooldownModifier", 0.9, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> CAULDRON_THROWABLE_COOLDOWN_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("cauldronThrowableCooldownModifier", 1.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> SPIRIT_MOUNT_REGENERATION_RATE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("spiritMountRegenerationRate", 1.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> SNOWBALL_DAMAGE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("snowballDamage", 3.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> REGENERATION_RATE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("regenerationRate", 0.05, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> INSATIATBLE_HUNGER_RATE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("insatiableHungerRate", 0.25, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> PROJECTILE_RESISTANT_REDUCTION = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("projectileResistantReduction", 0.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(0,1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> SOFT_HITTER_DAMAGE_REDUCTION = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("softhitterDamageReduction", 0.85, new ConfigUtils.DoubleConfigValue.DoubleLimits(0,1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> HEALTH_SPRINT_CUTOFF = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("healthSprintCutoff", 0.33, new ConfigUtils.DoubleConfigValue.DoubleLimits(0,1))));
   
   public static final ArchetypeConfig.ConfigSetting<?> KNOCKBACK_DECREASE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("knockbackReduction", 0.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> KNOCKBACK_INCREASE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("knockbackIncrease", 2.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> SNEAK_ATTACK_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("sneakAttackModifier", 1.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> BIOME_DAMAGE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("biomeDamage", 2.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> FALL_DAMAGE_REDUCTION = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("fallDamageReduction", 0.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> COLD_DAMAGE_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("coldDamageModifier", 2.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> ADDED_STARVE_DAMAGE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("addedStarveDamage", 3.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> IMPALE_VULNERABLE_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("impaleVulnerableModifier", 2.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> SLOW_FALLER_TRIGGER_SPEED = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("slowFallerTriggerSpeed", 0.3, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> STARTLE_MIN_DAMAGE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("startleMinDamage", 1.1, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> TUFF_FOOD_HEALTH_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("tuffFoodHealthModifier", 1.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> TUFF_FOOD_DURATION_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("tuffFoodDurationModifier", 1.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> IRON_FOOD_HEALTH_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("ironFoodHealthModifier", 1.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> IRON_FOOD_DURATION_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("ironFoodDurationModifier", 1.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> COPPER_FOOD_HEALTH_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("copperFoodHealthModifier", 1.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> COPPER_FOOD_DURATION_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("copperFoodDurationModifier", 1.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> LONG_ARMS_RANGE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("longArmsRange", 0.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> MOUNTED_RANGE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("mountedRange", 1.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> MOONLIT_SLIME_HEALTH_PER_PHASE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("moonlitSlimeHealthPerPhase", 0.25, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> SPEEDY_SPEED_BOOST = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("speedySpeedBoost", 0.25, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> SNEAKY_SPEED_BOOST = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("sneakySpeedBoost", 0.5, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> THORNY_REFLECTION_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("thornyReflectionModifier", 0.33, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> THORNY_REFLECTION_CAP = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("thornyReflectionCap", 20.0, new ConfigUtils.DoubleConfigValue.DoubleLimits())));
   
   public static final ArchetypeConfig.ConfigSetting<?> GUARDIAN_RAY_DAMAGE = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("guardianRayDamage", 4.0, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   public static final ArchetypeConfig.ConfigSetting<?> GREAT_SWIMMER_MOVE_SPEED_MODIFIER = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("greatSwimmerMoveSpeedModifier", 0.25, new ConfigUtils.DoubleConfigValue.DoubleLimits(-100,100))));
   
   public static final ArchetypeConfig.ConfigSetting<?> GLIDER_RECOVERY_TIME = registerConfigSetting(new ArchetypeConfig.NormalConfigSetting<>(
         new ConfigUtils.DoubleConfigValue("gliderRecoveryTime", 0.1, new ConfigUtils.DoubleConfigValue.DoubleLimits(0))));
   
   static{
      TUFF_FOODS.put(Items.TUFF,new Pair<>(2.0f,14));
      TUFF_FOODS.put(Items.TUFF_STAIRS,new Pair<>(2.5f,20));
      TUFF_FOODS.put(Items.TUFF_WALL,new Pair<>(2.5f,20));
      TUFF_FOODS.put(Items.TUFF_SLAB,new Pair<>(1.75f,10));
      TUFF_FOODS.put(Items.POLISHED_TUFF,new Pair<>(2.5f,25));
      TUFF_FOODS.put(Items.POLISHED_TUFF_STAIRS,new Pair<>(3.0f,25));
      TUFF_FOODS.put(Items.POLISHED_TUFF_WALL,new Pair<>(3.0f,25));
      TUFF_FOODS.put(Items.POLISHED_TUFF_SLAB,new Pair<>(2.25f,10));
      TUFF_FOODS.put(Items.TUFF_BRICKS,new Pair<>(2.75f,25));
      TUFF_FOODS.put(Items.TUFF_BRICK_STAIRS,new Pair<>(3.25f,25));
      TUFF_FOODS.put(Items.TUFF_BRICK_WALL,new Pair<>(3.25f,25));
      TUFF_FOODS.put(Items.TUFF_BRICK_SLAB,new Pair<>(2.5f,10));
      TUFF_FOODS.put(Items.CHISELED_TUFF,new Pair<>(3.5f,30));
      TUFF_FOODS.put(Items.CHISELED_TUFF_BRICKS,new Pair<>(3.75f,30));
      
      IRON_FOODS.put(Items.IRON_NUGGET,new Pair<>(0.25f,2));
      IRON_FOODS.put(Items.RAW_IRON,new Pair<>(0.5f,10));
      IRON_FOODS.put(Items.IRON_INGOT,new Pair<>(2.0f,32));
      IRON_FOODS.put(Items.IRON_ORE,new Pair<>(1.25f,32));
      IRON_FOODS.put(Items.DEEPSLATE_IRON_ORE,new Pair<>(1.5f,36));
      IRON_FOODS.put(Items.IRON_SHOVEL,new Pair<>(2.25f,40));
      IRON_FOODS.put(Items.IRON_PICKAXE,new Pair<>(6.75f,40));
      IRON_FOODS.put(Items.IRON_AXE,new Pair<>(6.75f,40));
      IRON_FOODS.put(Items.IRON_HOE,new Pair<>(4.5f,40));
      IRON_FOODS.put(Items.IRON_SWORD,new Pair<>(4.5f,40));
      IRON_FOODS.put(Items.IRON_HELMET,new Pair<>(11.25f,80));
      IRON_FOODS.put(Items.IRON_CHESTPLATE,new Pair<>(18.0f,80));
      IRON_FOODS.put(Items.IRON_LEGGINGS,new Pair<>(15.75f,80));
      IRON_FOODS.put(Items.IRON_BOOTS,new Pair<>(9.0f,80));
      IRON_FOODS.put(Items.IRON_HORSE_ARMOR,new Pair<>(13.5f,60));
      IRON_FOODS.put(Items.RAW_IRON_BLOCK,new Pair<>(5.0f,60));
      IRON_FOODS.put(Items.IRON_BLOCK,new Pair<>(20.0f,160));
      IRON_FOODS.put(Items.IRON_BARS,new Pair<>(1.0f,20));
      IRON_FOODS.put(Items.IRON_DOOR,new Pair<>(5.0f,40));
      IRON_FOODS.put(Items.IRON_TRAPDOOR,new Pair<>(8.0f,60));
      IRON_FOODS.put(Items.CHAIN,new Pair<>(2.5f,32));
      IRON_FOODS.put(Items.LANTERN,new Pair<>(2.0f,32));
      IRON_FOODS.put(Items.SOUL_LANTERN,new Pair<>(2.5f,32));
      IRON_FOODS.put(Items.HEAVY_WEIGHTED_PRESSURE_PLATE,new Pair<>(4.5f,50));
      IRON_FOODS.put(Items.CAULDRON,new Pair<>(15.75f,90));
      IRON_FOODS.put(Items.ANVIL,new Pair<>(50.0f,320));
      IRON_FOODS.put(Items.CHIPPED_ANVIL,new Pair<>(50.0f,360));
      IRON_FOODS.put(Items.DAMAGED_ANVIL,new Pair<>(50.0f,400));
      IRON_FOODS.put(Items.MINECART,new Pair<>(11.25f,80));
      IRON_FOODS.put(Items.SHEARS,new Pair<>(4.5f,40));
      IRON_FOODS.put(Items.HOPPER,new Pair<>(11.25f,90));
      IRON_FOODS.put(Items.COMPASS,new Pair<>(9.0f,90));
      IRON_FOODS.put(Items.ACTIVATOR_RAIL,new Pair<>(2.5f,36));
      IRON_FOODS.put(Items.DETECTOR_RAIL,new Pair<>(2.5f,36));
      IRON_FOODS.put(Items.RAIL,new Pair<>(1.25f,28));
      IRON_FOODS.put(Items.HOPPER_MINECART,new Pair<>(30.0f,120));
      
      COPPER_FOODS.put(Items.RAW_COPPER,new Pair<>(0.25f,8));
      COPPER_FOODS.put(Items.COPPER_INGOT,new Pair<>(1.0f,26));
      COPPER_FOODS.put(Items.COPPER_ORE,new Pair<>(0.65f,26));
      COPPER_FOODS.put(Items.DEEPSLATE_COPPER_ORE,new Pair<>(0.75f,30));
      COPPER_FOODS.put(Items.RAW_COPPER_BLOCK,new Pair<>(2.5f,40));
      COPPER_FOODS.put(Items.COPPER_BLOCK,new Pair<>(8.5f,80));
      COPPER_FOODS.put(Items.CHISELED_COPPER,new Pair<>(2.0f,26));
      COPPER_FOODS.put(Items.COPPER_GRATE,new Pair<>(2.0f,26));
      COPPER_FOODS.put(Items.CUT_COPPER,new Pair<>(2.0f,26));
      COPPER_FOODS.put(Items.CUT_COPPER_STAIRS,new Pair<>(2.0f,26));
      COPPER_FOODS.put(Items.CUT_COPPER_SLAB,new Pair<>(1.0f,12));
      COPPER_FOODS.put(Items.COPPER_DOOR,new Pair<>(2.25f,32));
      COPPER_FOODS.put(Items.COPPER_TRAPDOOR,new Pair<>(3.5f,32));
      COPPER_FOODS.put(Items.COPPER_BULB,new Pair<>(7.5f,40));
      COPPER_FOODS.put(Items.EXPOSED_COPPER,new Pair<>(9.0f,70));
      COPPER_FOODS.put(Items.EXPOSED_CHISELED_COPPER,new Pair<>(2.25f,24));
      COPPER_FOODS.put(Items.EXPOSED_COPPER_GRATE,new Pair<>(2.25f,24));
      COPPER_FOODS.put(Items.EXPOSED_CUT_COPPER,new Pair<>(2.25f,24));
      COPPER_FOODS.put(Items.EXPOSED_CUT_COPPER_STAIRS,new Pair<>(2.25f,24));
      COPPER_FOODS.put(Items.EXPOSED_CUT_COPPER_SLAB,new Pair<>(1.15f,11));
      COPPER_FOODS.put(Items.EXPOSED_COPPER_DOOR,new Pair<>(2.5f,30));
      COPPER_FOODS.put(Items.EXPOSED_COPPER_TRAPDOOR,new Pair<>(3.75f,30));
      COPPER_FOODS.put(Items.EXPOSED_COPPER_BULB,new Pair<>(8.0f,35));
      COPPER_FOODS.put(Items.WEATHERED_COPPER,new Pair<>(9.5f,60));
      COPPER_FOODS.put(Items.WEATHERED_CHISELED_COPPER,new Pair<>(2.5f,22));
      COPPER_FOODS.put(Items.WEATHERED_COPPER_GRATE,new Pair<>(2.5f,22));
      COPPER_FOODS.put(Items.WEATHERED_CUT_COPPER,new Pair<>(2.5f,22));
      COPPER_FOODS.put(Items.WEATHERED_CUT_COPPER_STAIRS,new Pair<>(2.5f,22));
      COPPER_FOODS.put(Items.WEATHERED_CUT_COPPER_SLAB,new Pair<>(1.25f,10));
      COPPER_FOODS.put(Items.WEATHERED_COPPER_DOOR,new Pair<>(2.75f,28));
      COPPER_FOODS.put(Items.WEATHERED_COPPER_TRAPDOOR,new Pair<>(4.0f,28));
      COPPER_FOODS.put(Items.WEATHERED_COPPER_BULB,new Pair<>(8.5f,30));
      COPPER_FOODS.put(Items.OXIDIZED_COPPER,new Pair<>(10.0f,50));
      COPPER_FOODS.put(Items.OXIDIZED_CHISELED_COPPER,new Pair<>(2.75f,20));
      COPPER_FOODS.put(Items.OXIDIZED_COPPER_GRATE,new Pair<>(2.75f,20));
      COPPER_FOODS.put(Items.OXIDIZED_CUT_COPPER,new Pair<>(2.75f,20));
      COPPER_FOODS.put(Items.OXIDIZED_CUT_COPPER_STAIRS,new Pair<>(2.75f,20));
      COPPER_FOODS.put(Items.OXIDIZED_CUT_COPPER_SLAB,new Pair<>(1.35f,8));
      COPPER_FOODS.put(Items.OXIDIZED_COPPER_DOOR,new Pair<>(3.0f,26));
      COPPER_FOODS.put(Items.OXIDIZED_COPPER_TRAPDOOR,new Pair<>(4.25f,26));
      COPPER_FOODS.put(Items.OXIDIZED_COPPER_BULB,new Pair<>(9.0f,25));
      COPPER_FOODS.put(Items.LIGHTNING_ROD,new Pair<>(3.5f,32));
   }
   
   private static ArchetypeAbility register(ArchetypeAbility ability){
      Registry.register(ABILITIES,Identifier.of(MOD_ID, ability.getId()), ability);
      return ability;
   }
   
   private static Archetype register(Archetype archetype){
      Registry.register(ARCHETYPES,Identifier.of(MOD_ID, archetype.getId()), archetype);
      return archetype;
   }
   
   private static SubArchetype register(SubArchetype subarchetype){
      Registry.register(SUBARCHETYPES,Identifier.of(MOD_ID, subarchetype.getId()), subarchetype);
      return subarchetype;
   }
   
   private static Item registerItem(String id, Item item){
      Identifier identifier = Identifier.of(MOD_ID,id);
      Registry.register(ITEMS, identifier, Registry.register(Registries.ITEM, identifier, item));
      return item;
   }
   
   private static ArchetypeConfig.ConfigSetting<?> registerConfigSetting(ArchetypeConfig.ConfigSetting<?> setting){
      Registry.register(CONFIG_SETTINGS,Identifier.of(MOD_ID,setting.getId()),setting);
      return setting;
   }
   
   public static void initialize(){
      PolymerResourcePackUtils.addModAssets(MOD_ID);
      
      PolymerItemUtils.ITEM_CHECK.register(
            (itemStack) -> {
               boolean isGolemFood = TUFF_FOODS.containsKey(itemStack.getItem()) || IRON_FOODS.containsKey(itemStack.getItem()) || COPPER_FOODS.containsKey(itemStack.getItem());
               boolean isGrowItem = itemStack.isIn(SLIME_GROW_ITEMS) || itemStack.isIn(MAGMA_CUBE_GROW_ITEMS);
               return isGolemFood || isGrowItem;
            }
      );
      
      PolymerItemUtils.ITEM_MODIFICATION_EVENT.register(
            (original, client, context) -> {
               ServerPlayerEntity player = context.getPlayer();
               if(player == null) return client;
               IArchetypeProfile profile = profile(player);
               HashMap<Item, Pair<Float,Integer>> map = null;
               float healthMod = 1.0f;
               float durationMod = 1.0f;
               
               if(profile.hasAbility(TUFF_EATER) && TUFF_FOODS.containsKey(original.getItem())){
                  map = TUFF_FOODS;
                  healthMod = (float) ArchetypeConfig.getDouble(ArchetypeRegistry.TUFF_FOOD_HEALTH_MODIFIER);
                  durationMod = (float) ArchetypeConfig.getDouble(ArchetypeRegistry.TUFF_FOOD_DURATION_MODIFIER);
               }
               if(profile.hasAbility(COPPER_EATER) && COPPER_FOODS.containsKey(original.getItem())){
                  map = COPPER_FOODS;
                  healthMod = (float) ArchetypeConfig.getDouble(ArchetypeRegistry.COPPER_FOOD_HEALTH_MODIFIER);
                  durationMod = (float) ArchetypeConfig.getDouble(ArchetypeRegistry.COPPER_FOOD_DURATION_MODIFIER);
               }
               if(profile.hasAbility(IRON_EATER) && IRON_FOODS.containsKey(original.getItem())){
                  map = IRON_FOODS;
                  healthMod = (float) ArchetypeConfig.getDouble(ArchetypeRegistry.IRON_FOOD_HEALTH_MODIFIER);
                  durationMod = (float) ArchetypeConfig.getDouble(ArchetypeRegistry.IRON_FOOD_DURATION_MODIFIER);
               }
               
               if(map != null){
                  LoreComponent lore = client.getOrDefault(DataComponentTypes.LORE,LoreComponent.DEFAULT);
                  List<Text> currentLore = new ArrayList<>(lore.styledLines());
                  Pair<Float,Integer> pair = map.get(original.getItem());
                  currentLore.add(getFoodLoreLine(new Pair<>(pair.getLeft() * healthMod, Math.round(pair.getRight() * durationMod))));
                  client.set(DataComponentTypes.LORE,new LoreComponent(currentLore,currentLore));
               }
               
               if(profile.hasAbility(SLIME_TOTEM) && original.isIn(ArchetypeRegistry.SLIME_GROW_ITEMS)){
                  LoreComponent lore = client.getOrDefault(DataComponentTypes.LORE,LoreComponent.DEFAULT);
                  List<Text> currentLore = new ArrayList<>(lore.styledLines());
                  currentLore.add(getGrowItemLoreLine());
                  client.set(DataComponentTypes.LORE,new LoreComponent(currentLore,currentLore));
               }
               
               if(profile.hasAbility(MAGMA_TOTEM) && original.isIn(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS)){
                  LoreComponent lore = client.getOrDefault(DataComponentTypes.LORE,LoreComponent.DEFAULT);
                  List<Text> currentLore = new ArrayList<>(lore.styledLines());
                  currentLore.add(getGrowItemLoreLine());
                  client.set(DataComponentTypes.LORE,new LoreComponent(currentLore,currentLore));
               }
               
               return client;
            }
      );
      
      final ItemGroup ITEM_GROUP = PolymerItemGroupUtils.builder().displayName(Text.translatable("itemGroup.archetype_items")).icon(() -> new ItemStack(CHANGE_ITEM)).entries((displayContext, entries) -> {
         entries.add(new ItemStack(CHANGE_ITEM));
         entries.add(new ItemStack(GLIDER_ITEM));
         entries.add(new ItemStack(WIND_CHARGE_VOLLEY_ITEM));
         entries.add(new ItemStack(FIREBALL_VOLLEY_ITEM));
         entries.add(new ItemStack(POTION_BREWER_ITEM));
         entries.add(new ItemStack(HORSE_SPIRIT_MOUNT_ITEM));
         entries.add(new ItemStack(DONKEY_SPIRIT_MOUNT_ITEM));
      }).build();
      
      PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(MOD_ID,"archetype_items"), ITEM_GROUP);
   }
   
   private static Text getFoodLoreLine(Pair<Float,Integer> pair){
      DecimalFormat df = new DecimalFormat("0.###");
      return MiscUtils.removeItalics(Text.literal("").formatted(Formatting.DARK_PURPLE)
            .append(Text.translatable("text.ancestralarchetypes.consume_1"))
            .append(Text.literal(df.format(pair.getRight()/20.0)+" ").formatted(Formatting.GOLD))
            .append(Text.translatable("text.ancestralarchetypes.seconds").formatted(Formatting.GOLD))
            .append(Text.translatable("text.ancestralarchetypes.consume_2"))
            .append(Text.literal(df.format(pair.getLeft()/2.0)+" ").formatted(Formatting.RED))
            .append(Text.translatable("text.ancestralarchetypes.hearts").formatted(Formatting.RED))
      );
   }
   
   private static Text getGrowItemLoreLine(){
      DecimalFormat df = new DecimalFormat("0.###");
      int eatTime = ArchetypeConfig.getInt(ArchetypeRegistry.GELATIAN_GROW_ITEM_EAT_DURATION);
      return MiscUtils.removeItalics(Text.literal("").formatted(Formatting.DARK_PURPLE)
            .append(Text.translatable("text.ancestralarchetypes.consume_1"))
            .append(Text.literal(df.format(eatTime/20.0)+" ").formatted(Formatting.GOLD))
            .append(Text.translatable("text.ancestralarchetypes.seconds").formatted(Formatting.GOLD))
            .append(Text.translatable("text.ancestralarchetypes.consume_regrow"))
      );
   }
}
