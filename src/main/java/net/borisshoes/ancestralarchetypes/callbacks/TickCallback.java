package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.PlayerArchetypeData;
import net.borisshoes.ancestralarchetypes.SubArchetype;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.ancestralarchetypes.misc.SpyglassRevealEvent;
import net.borisshoes.ancestralarchetypes.mixins.EntityAccessor;
import net.borisshoes.borislib.tracker.PlayerMovementEntry;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.borisshoes.borislib.utils.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.ITEMS;
import static net.borisshoes.borislib.BorisLib.PLAYER_MOVEMENT_TRACKER;


public class TickCallback {
   
   public static void onTick(MinecraftServer server){
      for(ServerPlayer player : server.getPlayerList().getPlayers()){
         PlayerArchetypeData profile = profile(player);
         ServerLevel world = player.level();
         
         deathReductionSize(profile,player);
         
         Inventory inv = player.getInventory();
         for(int i = 0; i < inv.getContainerSize(); i++){
            ItemStack stack = inv.getItem(i);
            inventoryItem(profile,player,stack);
         }
         
         profile.tick(player);
         
         stopEat(profile,player);
         slowfaller(profile,player);
         spyglass(profile,player);
         
         if(profile.hasAbility(ArchetypeRegistry.ANTIVENOM) && player.hasEffect(MobEffects.POISON)){
            player.removeEffect(MobEffects.POISON);
         }
         if(profile.hasAbility(ArchetypeRegistry.WITHERING) && player.hasEffect(MobEffects.WITHER)){
            player.removeEffect(MobEffects.WITHER);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.REGEN_WHEN_LOW) && player.getHealth() < player.getMaxHealth()/2.0 && !player.isDeadOrDying()){
            player.heal((float) CONFIG.getDouble(ArchetypeRegistry.REGENERATION_RATE));
         }
         
         if(profile.hasAbility(ArchetypeRegistry.HEALTH_BASED_SPRINT)){
            FoodData hungerManager = player.getFoodData();
            boolean canSprint = player.getHealth()/player.getMaxHealth() > (float) CONFIG.getDouble(ArchetypeRegistry.HEALTH_SPRINT_CUTOFF);
            if(canSprint){
               hungerManager.setFoodLevel(20);
            }else{
               hungerManager.setFoodLevel(2);
            }
         }
         
         if(profile.hasAbility(ArchetypeRegistry.LAVA_WALKER) && player.getFluidHeight(FluidTags.LAVA) > 0 && !player.isShiftKeyDown()){
            double lavaHeight = player.getFluidHeight(FluidTags.LAVA);
            if (lavaHeight > 0.1) {
               Vec3 v = player.getDeltaMovement();
               double targetDepth = player.getBbHeight() * 0.25;
               double error = lavaHeight - targetDepth;
               double gain = 0.16;
               double bobAmp = 0.025, bobFreq = 0.20, bobBand = 0.18;
               boolean inBand = Math.abs(error) < bobBand;
               double damping = inBand ? 0.80 : 0.95;
               double cmd = gain * error - damping * v.y;
               if (Math.abs(error) < bobBand) cmd += bobAmp * Math.sin(player.tickCount * bobFreq);
               
               double vyDelta = Mth.clamp(cmd, -0.08, 0.08);
               double newVy = Mth.clamp(v.y + vyDelta, -0.35, 0.35);
               Vec3 newVel = new Vec3(v.x * 0.99, newVy, v.z * 0.99);
               player.setDeltaMovement(newVel);
               if (newVel.distanceToSqr(v) > 1.0e-7) player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), newVel));
            }
         }
         
         if(server.getTickCount() % 5 == 0){
            if(profile.hasAbility(ArchetypeRegistry.SHY)){
               if(!world.getPlayers(p ->
                     p.distanceTo(player) < 255 && !player.getStringUUID().equals(p.getStringUUID()) &&
                           LivingEntity.PLAYER_NOT_WEARING_DISGUISE_ITEM.test(player) && player.isLookingAtMe(p, 1-Math.cos(CONFIG.getDouble(ArchetypeRegistry.SHY_VIEWING_ANGLE)), true, false, player.getEyeY()) &&
                           LivingEntity.PLAYER_NOT_WEARING_DISGUISE_ITEM.test(p) && p.isLookingAtMe(player, 1-Math.cos(CONFIG.getDouble(ArchetypeRegistry.SHY_NOTICING_ANGLE)), true, false, p.getEyeY())
               ).isEmpty()){
                  player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 6, 9, false, false, true));
               }
            }
         }
         
         importantAttributes(profile,player);
         
         if(server.getTickCount() % 5 == 0){
            attributes(profile,player);
            effects(profile,player);
         }
         
         if(server.getTickCount() % 20 == 0){
            boolean shouldMelt = profile.hasAbility(ArchetypeRegistry.HURT_BY_WATER) && !player.hasEffect(MobEffects.WATER_BREATHING) && player.isInWaterOrRain() && !player.isCreative() && !player.isSpectator();
            if(shouldMelt){
               if(player.isInWater()){
                  player.hurtServer(world, world.damageSources().drown(), (float) CONFIG.getDouble(ArchetypeRegistry.HURT_BY_WATER_SWIM_DAMAGE));
               }else if(((EntityAccessor)player).rainedOn() && server.getTickCount() % 40 == 0){
                  player.hurtServer(world, world.damageSources().drown(), (float) CONFIG.getDouble(ArchetypeRegistry.HURT_BY_WATER_RAIN_DAMAGE));
               }
               world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_BURN, player.getSoundSource(), 0.4F, 2.0F + player.getRandom().nextFloat() * 0.4F);
            }
         }
         
         if(server.getTickCount() % 40 == 0){
            temperatureCheck(profile,player);
         }
         
         if(player.level().getServer().getTickCount() % 18000 == 0 && profile.giveReminders()){ // Ability and Archetype check
            reminders(profile,player);
         }
      }
      
      SPYGLASS_REVEAL_EVENTS.forEach(SpyglassRevealEvent::tickCooldown);
      SPYGLASS_REVEAL_EVENTS.removeIf(event -> event.getCooldown() < 0);
   }
   
   private static void slowfaller(PlayerArchetypeData profile, ServerPlayer player){
      if(profile.hasAbility(ArchetypeRegistry.SLOW_FALLER)){
         int predictedFallDist = 0;
         for(int y = player.getBlockY(); y >= player.getBlockY()-player.level().getHeight(); y--){
            BlockPos blockPos = new BlockPos(player.getBlockX(),y,player.getBlockZ());
            BlockState state = player.level().getBlockState(blockPos);
            if(state.isAir() || state.getCollisionShape(player.level(),blockPos).isEmpty()){
               predictedFallDist++;
            }else{
               break;
            }
         }
         
         double yMov = PLAYER_MOVEMENT_TRACKER.getOrDefault(player, PlayerMovementEntry.blankEntry(player)).velocity().y();
         boolean shouldTriggerSlowFall = (yMov < -CONFIG.getDouble(ArchetypeRegistry.SLOW_FALLER_TRIGGER_SPEED))
               && !player.isFallFlying() && !player.getAbilities().flying && predictedFallDist > player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE) && !player.isSwimming();
         if(shouldTriggerSlowFall){
            if(!player.isShiftKeyDown()){
               if(!player.hasEffect(MobEffects.SLOW_FALLING)){
                  SoundUtils.playSongToPlayer(player, SoundEvents.ENDER_DRAGON_FLAP,0.3f,1);
               }else{
                  if(yMov < -1.25*CONFIG.getDouble(ArchetypeRegistry.SLOW_FALLER_TRIGGER_SPEED)){
                     player.push(0,0.2,0);
                     player.connection.send(new ClientboundSetEntityMotionPacket(player));
                  }
               }
               MobEffectInstance slowFall = new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true);
               player.addEffect(slowFall);
            }
            player.connection.aboveGroundTickCount = 0;
         }
      }
   }
   
   private static void temperatureCheck(PlayerArchetypeData profile, ServerPlayer player){
      ServerLevel world = player.level();
      Holder<Biome> biome = player.level().getBiome(player.blockPosition());
      float temp = biome.value().getBaseTemperature();
      
      boolean shouldFreeze = (biome.is(ArchetypeRegistry.COLD_DAMAGE_INCLUDE_BIOMES) || (temp < 0.1 && !biome.is(ArchetypeRegistry.COLD_DAMAGE_EXCEPTION_BIOMES))) &&
            profile.hasAbility(ArchetypeRegistry.DAMAGED_BY_COLD) && !player.hasEffect(MobEffects.WATER_BREATHING) && !(player.isCreative() || player.isSpectator());
      if(shouldFreeze){
         player.hurtServer(world, world.damageSources().freeze(), (float) CONFIG.getDouble(ArchetypeRegistry.BIOME_DAMAGE));
         player.displayClientMessage(Component.translatable("text.ancestralarchetypes.freeze_warning").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.PLAYER_HURT_FREEZE,1,1f);
      }
      
      boolean shouldDryOut = (biome.is(ArchetypeRegistry.DRY_OUT_INCLUDE_BIOMES) || (!biome.value().hasPrecipitation() && !biome.is(ArchetypeRegistry.DRY_OUT_EXCEPTION_BIOMES))) &&
            profile.hasAbility(ArchetypeRegistry.DRIES_OUT) && !player.isInWater() && !player.hasEffect(MobEffects.FIRE_RESISTANCE) &&
            !(player.isCreative() || player.isSpectator()) && !player.getItemBySlot(EquipmentSlot.HEAD).is(Items.TURTLE_HELMET);
      if(shouldDryOut){
         player.hurtServer(world, world.damageSources().dryOut(), (float) CONFIG.getDouble(ArchetypeRegistry.BIOME_DAMAGE));
         player.displayClientMessage(Component.translatable("text.ancestralarchetypes.dry_out_warning").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.FIRECHARGE_USE,1,1f);
      }
   }
   
   private static void reminders(PlayerArchetypeData profile, ServerPlayer player){
      if(profile.getSubArchetype() == null && profile.canChangeArchetype()){
         player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.archetype_reminder").withStyle(s ->
               s.withClickEvent(new ClickEvent.RunCommand("/archetypes changeArchetype"))
                     .withHoverEvent(new HoverEvent.ShowText(Component.translatable("text.ancestralarchetypes.change_hover")))
                     .withColor(ChatFormatting.AQUA)));
      }
      
      Set<ArchetypeAbility> abilities = profile.getAbilities();
      boolean giveWarning = false;
      block: {
         for(ArchetypeAbility ability : abilities){
            if(!ability.active()) continue;
            for(Item item : ITEMS){
               if(item instanceof AbilityItem abilityItem && ability.equals(abilityItem.ability)){
                  boolean found = false;
                  Inventory inv = player.getInventory();
                  for(int i = 0; i < inv.getContainerSize(); i++){
                     ItemStack stack = inv.getItem(i);
                     if(stack.is(abilityItem)){
                        found = true;
                        break;
                     }
                  }
                  if(!found){
                     giveWarning = true;
                     break block;
                  }
               }
            }
         }
      }
      if(giveWarning){
         player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.ability_reminder").withStyle(s ->
               s.withClickEvent(new ClickEvent.RunCommand("/archetypes items"))
                     .withHoverEvent(new HoverEvent.ShowText(Component.translatable("text.ancestralarchetypes.items_hover")))
                     .withColor(ChatFormatting.AQUA)));
      }
   }
   
   private static void spyglass(PlayerArchetypeData profile, ServerPlayer player){
      boolean spyglass = CONFIG.getBoolean(ArchetypeRegistry.SPYGLASS_REVEALS_ARCHETYPE);
      if(spyglass){
         int warmup = CONFIG.getInt(ArchetypeRegistry.SPYGLASS_INVESTIGATE_DURATION);
         boolean alert = CONFIG.getBoolean(ArchetypeRegistry.SPYGLASS_REVEAL_ALERTS_PLAYER);
         ItemStack activeStack = player.getUseItem();
         if(activeStack != null && activeStack.is(Items.SPYGLASS)){
            MinecraftUtils.LasercastResult lasercast = MinecraftUtils.lasercast(player.level(), player.getEyePosition(), player.getForward(), player.level().getServer().getPlayerList().getViewDistance() * 16, true, player);
            for(Entity entity : lasercast.sortedHits()){
               if(entity instanceof ServerPlayer target){
                  if(SPYGLASS_REVEAL_EVENTS.stream().anyMatch(event -> event.isReset() && event.getTarget().equals(target) && event.getInspector().equals(player))){
                     continue;
                  }
                  SPYGLASS_REVEAL_EVENTS.add(new SpyglassRevealEvent(player,target, (int) (warmup*1.125),false));
                  break;
               }
            }
         }
         
         HashMap<ServerPlayer, Integer> revealers = new HashMap<>();
         for(SpyglassRevealEvent event : SPYGLASS_REVEAL_EVENTS){
            if(!event.getTarget().equals(player) || event.isReset()) continue;
            revealers.compute(event.getInspector(), (k, count) -> count == null ? 1 : count + 1);
         }
         AtomicInteger highestRevealCount = new AtomicInteger();
         revealers.forEach((inspector, count) -> {
            if(count > highestRevealCount.get()){
               highestRevealCount.set(count);
            }
            double percentage = (double) count / warmup;
            StringBuilder message = new StringBuilder("\uD83D\uDD0D ");
            for (int i = 0; i < 20; i++) {
               if(percentage*20 > i){
                  message.append("|");
               }else{
                  message.append("¦");
               }
            }
            message.append(" \uD83D\uDD0D");
            if(inspector.getUseItem() != null && inspector.getUseItem().is(Items.SPYGLASS))
               inspector.displayClientMessage(Component.literal(message.toString()).withStyle(ChatFormatting.GOLD),true);
            if(percentage > 1.0){
               SubArchetype subArchetype = profile.getSubArchetype();
               if(subArchetype == null){
                  inspector.displayClientMessage(Component.translatable("text.ancestralarchetypes.inspect_no_archetype",player.getFeedbackDisplayName()).withStyle(ChatFormatting.AQUA),false);
               }else{
                  inspector.displayClientMessage(Component.translatable("text.ancestralarchetypes.inspect_results",player.getFeedbackDisplayName(),subArchetype.getName().withStyle(TextUtils.getClosestFormatting(subArchetype.getColor()))).withStyle(ChatFormatting.AQUA),false);
               }
               if(alert) player.sendSystemMessage(Component.translatable("text.ancestralarchetypes.inspected").withStyle(ChatFormatting.RED));
               
               SPYGLASS_REVEAL_EVENTS.removeIf(event -> event.getInspector().equals(inspector) && event.getTarget().equals(player));
               SPYGLASS_REVEAL_EVENTS.add(new SpyglassRevealEvent(inspector, player,1200,true));
            }
         });
         
         if(alert && highestRevealCount.get() > 0){
            double percentage = (double) highestRevealCount.get() / warmup;
            StringBuilder message = new StringBuilder();
            for (int i = 0; i < 20; i++) {
               if(percentage*20 > i){
                  message.append("|");
               }else{
                  message.append("¦");
               }
            }
            player.displayClientMessage(Component.translatable("text.ancestralarchetypes.inspection_in_progress",message.toString()).withStyle(ChatFormatting.GOLD),true);
         }
      }
   }
   
   private static void effects(PlayerArchetypeData profile, ServerPlayer player){
      if(profile.hasAbility(ArchetypeRegistry.FIRE_IMMUNE)){
         MobEffectInstance fireRes = new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 210, 0, false, false, true);
         player.addEffect(fireRes);
         if(player.isOnFire()) player.clearFire();
      }
      
      if(profile.hasAbility(ArchetypeRegistry.INSATIABLE)){
         FoodData hungerManager = player.getFoodData();
         hungerManager.addExhaustion((float) CONFIG.getDouble(ArchetypeRegistry.INSATIATBLE_HUNGER_RATE));
      }
      
      if(profile.hasAbility(ArchetypeRegistry.WEAVING)){
         player.addEffect(new MobEffectInstance(MobEffects.WEAVING, 210, 0, false, false, true));
      }
      
      if(profile.hasAbility(ArchetypeRegistry.MOONLIT_CAVE_SPIDER) && player.level().dimension().equals(ServerLevel.OVERWORLD)){
         long timeOfDay = player.level().getDayTime();
         int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
         int curPhase = day % 8;
         int moonLevel = Math.abs(-curPhase+4); // 0 - new moon, 4 - full moon
         
         if(moonLevel == 2){
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 210, 0, false, false, true));
         }else if(moonLevel == 3){
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 210, 0, false, false, true));
         }else if(moonLevel == 4){
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 210, 0, false, false, true));
         }
      }
   }
   
   public static void importantAttributes(PlayerArchetypeData profile, ServerPlayer player){
      if(profile.hasAbility(ArchetypeRegistry.GOOD_SWIMMER)){
         if(player.isUnderWater()){
            MobEffectInstance conduitPower = new MobEffectInstance(MobEffects.CONDUIT_POWER, 210, 0, false, false, true);
            player.addEffect(conduitPower);
         }
         MinecraftUtils.attributeEffect(player, Attributes.WATER_MOVEMENT_EFFICIENCY, 1f, AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"swim_buff"),false);
      }else{
         MinecraftUtils.attributeEffect(player, Attributes.WATER_MOVEMENT_EFFICIENCY, 1f, AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"swim_buff"),true);
      }
      
      if(profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER) && player.isInWaterOrRain()){
         MinecraftUtils.attributeEffect(player, Attributes.MOVEMENT_SPEED, CONFIG.getDouble(ArchetypeRegistry.GREAT_SWIMMER_MOVE_SPEED_MODIFIER), AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"great_swimmer"),false);
      }else{
         MinecraftUtils.attributeEffect(player, Attributes.MOVEMENT_SPEED, CONFIG.getDouble(ArchetypeRegistry.GREAT_SWIMMER_MOVE_SPEED_MODIFIER), AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"great_swimmer"),true);
      }
      
      MinecraftUtils.attributeEffect(player, Attributes.MOVEMENT_SPEED, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"fortify"),!profile.isFortifyActive());
      MinecraftUtils.attributeEffect(player, Attributes.JUMP_STRENGTH, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"fortify"),!profile.isFortifyActive());
      
      MinecraftUtils.attributeEffect(player, Attributes.MOVEMENT_SPEED, CONFIG.getDouble(ArchetypeRegistry.FUNGUS_SPEED_BOOST_MULTIPLIER), AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"fungus_speed_boost"),!profile.isFungusBoosted());
      
      Entity vehicle = player.getVehicle();
      boolean gaveReach = false;
      if(vehicle != null){
         if(!vehicle.getTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList().isEmpty()){
            MinecraftUtils.attributeEffect(player, Attributes.BLOCK_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.MOUNTED_RANGE), AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"mounted_reach"),false);
            MinecraftUtils.attributeEffect(player, Attributes.ENTITY_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.MOUNTED_RANGE), AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"mounted_reach"),false);
            MinecraftUtils.attributeEffect(player, Attributes.SAFE_FALL_DISTANCE, 10, AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"mounted_fall"),false);
            gaveReach = true;
         }
      }
      if(!gaveReach){
         MinecraftUtils.attributeEffect(player, Attributes.BLOCK_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.MOUNTED_RANGE), AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"mounted_reach"),true);
         MinecraftUtils.attributeEffect(player, Attributes.ENTITY_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.MOUNTED_RANGE), AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"mounted_reach"),true);
         MinecraftUtils.attributeEffect(player, Attributes.SAFE_FALL_DISTANCE, 10, AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"mounted_fall"),true);
      }
   }
   
   public static void attributes(PlayerArchetypeData profile, ServerPlayer player){
      MinecraftUtils.attributeEffect(player, Attributes.SCALE, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"half_sized"),!profile.hasAbility(ArchetypeRegistry.HALF_SIZED));
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"half_sized"),!profile.hasAbility(ArchetypeRegistry.HALF_SIZED));
      
      MinecraftUtils.attributeEffect(player, Attributes.SCALE, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"tall_sized"),!profile.hasAbility(ArchetypeRegistry.TALL_SIZED));
      
      MinecraftUtils.attributeEffect(player, Attributes.SCALE, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"giant_sized"),!profile.hasAbility(ArchetypeRegistry.GIANT_SIZED));
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH, 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"giant_sized"),!profile.hasAbility(ArchetypeRegistry.GIANT_SIZED));
      
      MinecraftUtils.attributeEffect(player, Attributes.SCALE, 0.75, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"massive_sized"),!profile.hasAbility(ArchetypeRegistry.MASSIVE_SIZED));
      MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH, 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"massive_sized"),!profile.hasAbility(ArchetypeRegistry.MASSIVE_SIZED));
      
      MinecraftUtils.attributeEffect(player, Attributes.ENTITY_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.LONG_ARMS_RANGE), AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"long_arms"),!profile.hasAbility(ArchetypeRegistry.LONG_ARMS));
      MinecraftUtils.attributeEffect(player, Attributes.BLOCK_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.LONG_ARMS_RANGE), AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"long_arms"),!profile.hasAbility(ArchetypeRegistry.LONG_ARMS));
      
      MinecraftUtils.attributeEffect(player, Attributes.MOVEMENT_SPEED, CONFIG.getDouble(ArchetypeRegistry.SPEEDY_SPEED_BOOST), AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"speedy"),!profile.hasAbility(ArchetypeRegistry.SPEEDY));
      
      MinecraftUtils.attributeEffect(player, Attributes.SNEAKING_SPEED, CONFIG.getDouble(ArchetypeRegistry.SNEAKY_SPEED_BOOST), AttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.fromNamespaceAndPath(MOD_ID,"sneaky"),!profile.hasAbility(ArchetypeRegistry.SNEAKY));
      
      MinecraftUtils.attributeEffect(player, Attributes.JUMP_STRENGTH, CONFIG.getDouble(ArchetypeRegistry.JUMPY_JUMP_BOOST), AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"jumpy"),!profile.hasAbility(ArchetypeRegistry.JUMPY));
      MinecraftUtils.attributeEffect(player, Attributes.SAFE_FALL_DISTANCE, CONFIG.getDouble(ArchetypeRegistry.JUMPY_JUMP_BOOST)*10, AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"jumpy"),!profile.hasAbility(ArchetypeRegistry.JUMPY));
      
      MinecraftUtils.attributeEffect(player, Attributes.ATTACK_KNOCKBACK, CONFIG.getDouble(ArchetypeRegistry.HARD_HITTER_KNOCKBACK_INCREASE), AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"hard_hitter"),!profile.hasAbility(ArchetypeRegistry.HARD_HITTER));
      
      MinecraftUtils.attributeEffect(player, Attributes.ATTACK_SPEED, CONFIG.getDouble(ArchetypeRegistry.HASTY_ATTACK_SPEED_INCREASE), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"hasty"),!profile.hasAbility(ArchetypeRegistry.HASTY));
      
      MinecraftUtils.attributeEffect(player, Attributes.SAFE_FALL_DISTANCE, CONFIG.getDouble(ArchetypeRegistry.RESILIENT_JOINTS_EXTRA_FALL_BLOCKS), AttributeModifier.Operation.ADD_VALUE, Identifier.fromNamespaceAndPath(MOD_ID,"resilient_joints"),!profile.hasAbility(ArchetypeRegistry.RESILIENT_JOINTS));
      
      long timeOfDay = player.level().getDayTime();
      int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
      int curPhase = day % 8;
      int moonLevel = Math.abs(-curPhase+4); // 0 - new moon, 4 - full moon
      for(int i = 0; i <= 4; i++){
         Identifier id = Identifier.fromNamespaceAndPath(MOD_ID,"slime_moonlit_"+i);
         if(moonLevel == i && profile.hasAbility(ArchetypeRegistry.MOONLIT_SLIME) && profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM)){
            MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH, CONFIG.getDouble(ArchetypeRegistry.MOONLIT_SLIME_HEALTH_PER_PHASE) * i, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, id,false);
            MinecraftUtils.attributeEffect(player, Attributes.SCALE, CONFIG.getDouble(ArchetypeRegistry.MOONLIT_SLIME_SIZE_PER_PHASE) * i, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, id,false);
         }else{
            MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH, CONFIG.getDouble(ArchetypeRegistry.MOONLIT_SLIME_HEALTH_PER_PHASE) * i, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, id,true);
            MinecraftUtils.attributeEffect(player, Attributes.SCALE, CONFIG.getDouble(ArchetypeRegistry.MOONLIT_SLIME_SIZE_PER_PHASE) * i, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, id,true);
         }
      }
   }
   
   public static void deathReductionSize(PlayerArchetypeData profile, ServerPlayer player){
      if(profile.getHealthUpdate() != 0){
         double scale = -(1 - Math.pow(0.5,profile.getDeathReductionSizeLevel()));
         MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH,scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"death_reduction_size_level"),true);
         MinecraftUtils.attributeEffect(player, Attributes.SCALE,scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"death_reduction_size_level"),true);
         MinecraftUtils.attributeEffect(player, Attributes.MAX_HEALTH,scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"death_reduction_size_level"),false);
         MinecraftUtils.attributeEffect(player, Attributes.SCALE,scale, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.fromNamespaceAndPath(MOD_ID,"death_reduction_size_level"),false);
         if(player.getMaxHealth() >= profile.getHealthUpdate()){
            player.setHealth(profile.getHealthUpdate());
            profile.setHealthUpdate(0);
         }
      }
   }
   
   public static void inventoryItem(PlayerArchetypeData profile, ServerPlayer player, ItemStack stack){
      HashMap<Item, Tuple<Float,Integer>> map = null;
      boolean unusualFood = ArchetypeRegistry.TUFF_FOODS.containsKey(stack.getItem())
            || ArchetypeRegistry.COPPER_FOODS.containsKey(stack.getItem())
            || ArchetypeRegistry.IRON_FOODS.containsKey(stack.getItem())
            || stack.is(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS)
            || stack.is(ArchetypeRegistry.SLIME_GROW_ITEMS)
            || stack.is(Items.WARPED_FUNGUS)
            || stack.is(Items.HONEYCOMB);
      float durationMod = 1.0f;
      
      if(profile.hasAbility(ArchetypeRegistry.TUFF_EATER) && ArchetypeRegistry.TUFF_FOODS.containsKey(stack.getItem())){
         map = ArchetypeRegistry.TUFF_FOODS;
         durationMod = (float) CONFIG.getDouble(ArchetypeRegistry.TUFF_FOOD_DURATION_MODIFIER);
      }
      if(profile.hasAbility(ArchetypeRegistry.COPPER_EATER) && ArchetypeRegistry.COPPER_FOODS.containsKey(stack.getItem())){
         map = ArchetypeRegistry.COPPER_FOODS;
         durationMod = (float) CONFIG.getDouble(ArchetypeRegistry.COPPER_FOOD_DURATION_MODIFIER);
      }
      if(profile.hasAbility(ArchetypeRegistry.IRON_EATER) && ArchetypeRegistry.IRON_FOODS.containsKey(stack.getItem())){
         map = ArchetypeRegistry.IRON_FOODS;
         durationMod = (float) CONFIG.getDouble(ArchetypeRegistry.IRON_FOOD_DURATION_MODIFIER);
      }
      
      ItemStack mainhand = player.getMainHandItem();
      ItemStack offhand = player.getOffhandItem();
      boolean inHand = stack.equals(mainhand) || stack.equals(offhand);
      boolean shouldHaveGolemEatComponent = (inHand && map != null && player.getHealth() < player.getMaxHealth());
      boolean shouldHaveGelatianEatComponent = inHand && profile.getDeathReductionSizeLevel() != 0
            && ((profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM) && stack.is(ArchetypeRegistry.SLIME_GROW_ITEMS))
            || (profile.hasAbility(ArchetypeRegistry.MAGMA_TOTEM) && stack.is(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS)));
      boolean shouldHaveFungusEatComponent = inHand && profile.hasAbility(ArchetypeRegistry.FUNGUS_SPEED_BOOST) && stack.is(Items.WARPED_FUNGUS);
      boolean shouldHaveWaxEatComponent = inHand && profile.hasAbility(ArchetypeRegistry.WAX_SHIELD) && stack.is(Items.HONEYCOMB) && player.getAbsorptionAmount() < CONFIG.getDouble(ArchetypeRegistry.WAX_SHIELD_MAX_HEALTH);
      if(shouldHaveGolemEatComponent){ // Add component
         if(!stack.has(DataComponents.CONSUMABLE)){
            Tuple<Float,Integer> pair = map.get(stack.getItem());
            Consumable comp = Consumable.builder().sound(SoundEvents.GENERIC_EAT).animation(ItemUseAnimation.EAT).consumeSeconds(pair.getB()/20.0f * durationMod).hasConsumeParticles(true).build();
            stack.set(DataComponents.CONSUMABLE,comp);
         }
      }else if(shouldHaveGelatianEatComponent){
         if(!stack.has(DataComponents.CONSUMABLE)){
            Consumable comp = Consumable.builder().sound(SoundEvents.GENERIC_EAT).animation(ItemUseAnimation.EAT).consumeSeconds(CONFIG.getInt(ArchetypeRegistry.GELATIAN_GROW_ITEM_EAT_DURATION)/20.0f).hasConsumeParticles(true).build();
            stack.set(DataComponents.CONSUMABLE,comp);
         }
      }else if(shouldHaveFungusEatComponent){
         if(!stack.has(DataComponents.CONSUMABLE)){
            Consumable comp = Consumable.builder().sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.STRIDER_EAT)).animation(ItemUseAnimation.EAT).consumeSeconds(CONFIG.getInt(ArchetypeRegistry.FUNGUS_SPEED_BOOST_CONSUME_DURATION)/20.0f).hasConsumeParticles(true).build();
            stack.set(DataComponents.CONSUMABLE,comp);
         }
      }else if(shouldHaveWaxEatComponent){
         if(!stack.has(DataComponents.CONSUMABLE)){
            Consumable comp = Consumable.builder().sound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.HONEYCOMB_WAX_ON)).animation(ItemUseAnimation.EAT).consumeSeconds(CONFIG.getInt(ArchetypeRegistry.WAX_SHIELD_CONSUME_DURATION)/20.0f).hasConsumeParticles(true).build();
            stack.set(DataComponents.CONSUMABLE,comp);
         }
      }else if(unusualFood && stack.has(DataComponents.CONSUMABLE)){ // Remove component
         stack.remove(DataComponents.CONSUMABLE);
      }
   }
   
   private static void stopEat(PlayerArchetypeData profile, ServerPlayer player){
      if(player.isUsingItem()){
         if(player.getUseItem().has(DataComponents.FOOD)){
            boolean stopUsing = false;
            if(profile.hasAbility(ArchetypeRegistry.CARNIVORE) && !player.getUseItem().is(ArchetypeRegistry.CARNIVORE_FOODS)){
               stopUsing = true;
            }else if(profile.hasAbility(ArchetypeRegistry.COPPER_EATER) || profile.hasAbility(ArchetypeRegistry.IRON_EATER) || profile.hasAbility(ArchetypeRegistry.TUFF_EATER)){
               stopUsing = true;
            }
            
            if(stopUsing){
               player.getCooldowns().addCooldown(player.getUseItem(),20);
               player.releaseUsingItem();
               player.displayClientMessage(Component.translatable("text.ancestralarchetypes.cannot_eat").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC),true);
               SoundUtils.playSongToPlayer(player, SoundEvents.PLAYER_BURP,1,0.5f);
            }
         }
      }
   }
}
