package net.borisshoes.ancestralarchetypes.callbacks;

import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.SubArchetype;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.items.AbilityItem;
import net.borisshoes.ancestralarchetypes.misc.SpyglassRevealEvent;
import net.borisshoes.ancestralarchetypes.mixins.EntityAccessor;
import net.borisshoes.borislib.tracker.PlayerMovementEntry;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.borisshoes.borislib.utils.SoundUtils;
import net.borisshoes.borislib.utils.TextUtils;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.*;
import static net.borisshoes.ancestralarchetypes.ArchetypeRegistry.ITEMS;
import static net.borisshoes.borislib.BorisLib.PLAYER_MOVEMENT_TRACKER;


public class TickCallback {
   
   public static void onTick(MinecraftServer server){
      for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()){
         IArchetypeProfile profile = profile(player);
         ServerWorld world = player.getEntityWorld();
         
         deathReductionSize(profile,player);
         
         PlayerInventory inv = player.getInventory();
         for(int i = 0; i < inv.size(); i++){
            ItemStack stack = inv.getStack(i);
            inventoryItem(profile,player,stack);
         }
         
         profile.tick();
         
         stopEat(profile,player);
         slowfaller(profile,player);
         spyglass(profile,player);
         
         if(profile.hasAbility(ArchetypeRegistry.ANTIVENOM) && player.hasStatusEffect(StatusEffects.POISON)){
            player.removeStatusEffect(StatusEffects.POISON);
         }
         if(profile.hasAbility(ArchetypeRegistry.WITHERING) && player.hasStatusEffect(StatusEffects.WITHER)){
            player.removeStatusEffect(StatusEffects.WITHER);
         }
         
         if(profile.hasAbility(ArchetypeRegistry.REGEN_WHEN_LOW) && player.getHealth() < player.getMaxHealth()/2.0 && !player.isDead()){
            player.heal((float) CONFIG.getDouble(ArchetypeRegistry.REGENERATION_RATE));
         }
         
         if(profile.hasAbility(ArchetypeRegistry.HEALTH_BASED_SPRINT)){
            HungerManager hungerManager = player.getHungerManager();
            boolean canSprint = player.getHealth()/player.getMaxHealth() > (float) CONFIG.getDouble(ArchetypeRegistry.HEALTH_SPRINT_CUTOFF);
            if(canSprint){
               hungerManager.setFoodLevel(20);
            }else{
               hungerManager.setFoodLevel(2);
            }
         }
         
         if(profile.hasAbility(ArchetypeRegistry.LAVA_WALKER) && player.getFluidHeight(FluidTags.LAVA) > 0 && !player.isSneaking()){
            double lavaHeight = player.getFluidHeight(FluidTags.LAVA);
            if (lavaHeight > 0.1) {
               Vec3d v = player.getVelocity();
               double targetDepth = player.getHeight() * 0.25;
               double error = lavaHeight - targetDepth;
               double gain = 0.16;
               double bobAmp = 0.025, bobFreq = 0.20, bobBand = 0.18;
               boolean inBand = Math.abs(error) < bobBand;
               double damping = inBand ? 0.80 : 0.95;
               double cmd = gain * error - damping * v.y;
               if (Math.abs(error) < bobBand) cmd += bobAmp * Math.sin(player.age * bobFreq);
               
               double vyDelta = MathHelper.clamp(cmd, -0.08, 0.08);
               double newVy = MathHelper.clamp(v.y + vyDelta, -0.35, 0.35);
               Vec3d newVel = new Vec3d(v.x * 0.99, newVy, v.z * 0.99);
               player.setVelocity(newVel);
               if (newVel.squaredDistanceTo(v) > 1.0e-7) player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player.getId(), newVel));
            }
         }
         
         if(server.getTicks() % 5 == 0){
            if(profile.hasAbility(ArchetypeRegistry.SHY)){
               if(!world.getPlayers(p ->
                     p.distanceTo(player) < 255 && !player.getUuidAsString().equals(p.getUuidAsString()) &&
                           LivingEntity.NOT_WEARING_GAZE_DISGUISE_PREDICATE.test(player) && player.isEntityLookingAtMe(p, 1-Math.cos(CONFIG.getDouble(ArchetypeRegistry.SHY_VIEWING_ANGLE)), true, false, player.getEyeY()) &&
                           LivingEntity.NOT_WEARING_GAZE_DISGUISE_PREDICATE.test(p) && p.isEntityLookingAtMe(player, 1-Math.cos(CONFIG.getDouble(ArchetypeRegistry.SHY_NOTICING_ANGLE)), true, false, p.getEyeY())
               ).isEmpty()){
                  player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, 9, false, false, true));
               }
            }
         }
         
         importantAttributes(profile,player);
         
         if(server.getTicks() % 5 == 0){
            attributes(profile,player);
            effects(profile,player);
         }
         
         if(server.getTicks() % 20 == 0){
            boolean shouldMelt = profile.hasAbility(ArchetypeRegistry.HURT_BY_WATER) && !player.hasStatusEffect(StatusEffects.WATER_BREATHING) && player.isTouchingWaterOrRain() && !player.isCreative() && !player.isSpectator();
            if(shouldMelt){
               if(player.isTouchingWater()){
                  player.damage(world, world.getDamageSources().drown(), (float) CONFIG.getDouble(ArchetypeRegistry.HURT_BY_WATER_SWIM_DAMAGE));
               }else if(((EntityAccessor)player).rainedOn() && server.getTicks() % 40 == 0){
                  player.damage(world, world.getDamageSources().drown(), (float) CONFIG.getDouble(ArchetypeRegistry.HURT_BY_WATER_RAIN_DAMAGE));
               }
               world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_GENERIC_BURN, player.getSoundCategory(), 0.4F, 2.0F + player.getRandom().nextFloat() * 0.4F);
            }
         }
         
         if(server.getTicks() % 40 == 0){
            temperatureCheck(profile,player);
         }
         
         if(player.getEntityWorld().getServer().getTicks() % 18000 == 0 && profile.giveReminders()){ // Ability and Archetype check
            reminders(profile,player);
         }
      }
      
      SPYGLASS_REVEAL_EVENTS.forEach(SpyglassRevealEvent::tickCooldown);
      SPYGLASS_REVEAL_EVENTS.removeIf(event -> event.getCooldown() < 0);
   }
   
   private static void slowfaller(IArchetypeProfile profile, ServerPlayerEntity player){
      if(profile.hasAbility(ArchetypeRegistry.SLOW_FALLER)){
         int predictedFallDist = 0;
         for(int y = player.getBlockY(); y >= player.getBlockY()-player.getEntityWorld().getHeight(); y--){
            BlockPos blockPos = new BlockPos(player.getBlockX(),y,player.getBlockZ());
            BlockState state = player.getEntityWorld().getBlockState(blockPos);
            if(state.isAir() || state.getCollisionShape(player.getEntityWorld(),blockPos).isEmpty()){
               predictedFallDist++;
            }else{
               break;
            }
         }
         
         double yMov = PLAYER_MOVEMENT_TRACKER.getOrDefault(player, PlayerMovementEntry.blankEntry(player)).velocity().getY();
         boolean shouldTriggerSlowFall = (yMov < -CONFIG.getDouble(ArchetypeRegistry.SLOW_FALLER_TRIGGER_SPEED))
               && !player.isGliding() && !player.getAbilities().flying && predictedFallDist > player.getAttributeValue(EntityAttributes.SAFE_FALL_DISTANCE) && !player.isSwimming();
         if(shouldTriggerSlowFall){
            if(!player.isSneaking()){
               if(!player.hasStatusEffect(StatusEffects.SLOW_FALLING)){
                  SoundUtils.playSongToPlayer(player,SoundEvents.ENTITY_ENDER_DRAGON_FLAP,0.3f,1);
               }else{
                  if(yMov < -1.25*CONFIG.getDouble(ArchetypeRegistry.SLOW_FALLER_TRIGGER_SPEED)){
                     player.addVelocity(0,0.2,0);
                     player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
                  }
               }
               StatusEffectInstance slowFall = new StatusEffectInstance(StatusEffects.SLOW_FALLING, 60, 0, false, false, true);
               player.addStatusEffect(slowFall);
            }
            player.networkHandler.floatingTicks = 0;
         }
      }
   }
   
   private static void temperatureCheck(IArchetypeProfile profile, ServerPlayerEntity player){
      ServerWorld world = player.getEntityWorld();
      RegistryEntry<Biome> biome = player.getEntityWorld().getBiome(player.getBlockPos());
      float temp = biome.value().getTemperature();
      
      boolean shouldFreeze = (biome.isIn(ArchetypeRegistry.COLD_DAMAGE_INCLUDE_BIOMES) || (temp < 0.1 && !biome.isIn(ArchetypeRegistry.COLD_DAMAGE_EXCEPTION_BIOMES))) &&
            profile.hasAbility(ArchetypeRegistry.DAMAGED_BY_COLD) && !player.hasStatusEffect(StatusEffects.WATER_BREATHING) && !(player.isCreative() || player.isSpectator());
      if(shouldFreeze){
         player.damage(world, world.getDamageSources().freeze(), (float) CONFIG.getDouble(ArchetypeRegistry.BIOME_DAMAGE));
         player.sendMessage(Text.translatable("text.ancestralarchetypes.freeze_warning").formatted(Formatting.AQUA,Formatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.ENTITY_PLAYER_HURT_FREEZE,1,1f);
      }
      
      boolean shouldDryOut = (biome.isIn(ArchetypeRegistry.DRY_OUT_INCLUDE_BIOMES) || (!biome.value().hasPrecipitation() && !biome.isIn(ArchetypeRegistry.DRY_OUT_EXCEPTION_BIOMES))) &&
            profile.hasAbility(ArchetypeRegistry.DRIES_OUT) && !player.isTouchingWater() && !player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) &&
            !(player.isCreative() || player.isSpectator()) && !player.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.TURTLE_HELMET);
      if(shouldDryOut){
         player.damage(world, world.getDamageSources().dryOut(), (float) CONFIG.getDouble(ArchetypeRegistry.BIOME_DAMAGE));
         player.sendMessage(Text.translatable("text.ancestralarchetypes.dry_out_warning").formatted(Formatting.RED,Formatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.ITEM_FIRECHARGE_USE,1,1f);
      }
   }
   
   private static void reminders(IArchetypeProfile profile, ServerPlayerEntity player){
      if(profile.getSubArchetype() == null && profile.canChangeArchetype()){
         player.sendMessage(Text.translatable("text.ancestralarchetypes.archetype_reminder").styled(s ->
               s.withClickEvent(new ClickEvent.RunCommand("/archetypes changeArchetype"))
                     .withHoverEvent(new HoverEvent.ShowText(Text.translatable("text.ancestralarchetypes.change_hover")))
                     .withColor(Formatting.AQUA)));
      }
      
      List<ArchetypeAbility> abilities = profile.getAbilities();
      boolean giveWarning = false;
      block: {
         for(ArchetypeAbility ability : abilities){
            if(!ability.isActive()) continue;
            for(Item item : ITEMS){
               if(item instanceof AbilityItem abilityItem && ability.equals(abilityItem.ability)){
                  boolean found = false;
                  PlayerInventory inv = player.getInventory();
                  for(int i = 0; i < inv.size(); i++){
                     ItemStack stack = inv.getStack(i);
                     if(stack.isOf(abilityItem)){
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
         player.sendMessage(Text.translatable("text.ancestralarchetypes.ability_reminder").styled(s ->
               s.withClickEvent(new ClickEvent.RunCommand("/archetypes items"))
                     .withHoverEvent(new HoverEvent.ShowText(Text.translatable("text.ancestralarchetypes.items_hover")))
                     .withColor(Formatting.AQUA)));
      }
   }
   
   private static void spyglass(IArchetypeProfile profile, ServerPlayerEntity player){
      boolean spyglass = CONFIG.getBoolean(ArchetypeRegistry.SPYGLASS_REVEALS_ARCHETYPE);
      if(spyglass){
         int warmup = CONFIG.getInt(ArchetypeRegistry.SPYGLASS_INVESTIGATE_DURATION);
         boolean alert = CONFIG.getBoolean(ArchetypeRegistry.SPYGLASS_REVEAL_ALERTS_PLAYER);
         ItemStack activeStack = player.getActiveItem();
         if(activeStack != null && activeStack.isOf(Items.SPYGLASS)){
            MinecraftUtils.LasercastResult lasercast = MinecraftUtils.lasercast(player.getEntityWorld(), player.getEyePos(), player.getRotationVecClient(), player.getEntityWorld().getServer().getPlayerManager().getViewDistance() * 16, true, player);
            for(Entity entity : lasercast.sortedHits()){
               if(entity instanceof ServerPlayerEntity target){
                  if(SPYGLASS_REVEAL_EVENTS.stream().anyMatch(event -> event.isReset() && event.getTarget().equals(target) && event.getInspector().equals(player))){
                     continue;
                  }
                  SPYGLASS_REVEAL_EVENTS.add(new SpyglassRevealEvent(player,target, (int) (warmup*1.125),false));
                  break;
               }
            }
         }
         
         HashMap<ServerPlayerEntity, Integer> revealers = new HashMap<>();
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
            if(inspector.getActiveItem() != null && inspector.getActiveItem().isOf(Items.SPYGLASS))
               inspector.sendMessage(Text.literal(message.toString()).formatted(Formatting.GOLD),true);
            if(percentage > 1.0){
               SubArchetype subArchetype = profile.getSubArchetype();
               if(subArchetype == null){
                  inspector.sendMessage(Text.translatable("text.ancestralarchetypes.inspect_no_archetype",player.getStyledDisplayName()).formatted(Formatting.AQUA),false);
               }else{
                  inspector.sendMessage(Text.translatable("text.ancestralarchetypes.inspect_results",player.getStyledDisplayName(),subArchetype.getName().formatted(TextUtils.getClosestFormatting(subArchetype.getColor()))).formatted(Formatting.AQUA),false);
               }
               if(alert) player.sendMessage(Text.translatable("text.ancestralarchetypes.inspected").formatted(Formatting.RED));
               
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
            player.sendMessage(Text.translatable("text.ancestralarchetypes.inspection_in_progress",message.toString()).formatted(Formatting.GOLD),true);
         }
      }
   }
   
   private static void effects(IArchetypeProfile profile, ServerPlayerEntity player){
      if(profile.hasAbility(ArchetypeRegistry.FIRE_IMMUNE)){
         StatusEffectInstance fireRes = new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 110, 0, false, false, true);
         player.addStatusEffect(fireRes);
         if(player.isOnFire()) player.extinguish();
      }
      
      if(profile.hasAbility(ArchetypeRegistry.INSATIABLE)){
         HungerManager hungerManager = player.getHungerManager();
         hungerManager.addExhaustion((float) CONFIG.getDouble(ArchetypeRegistry.INSATIATBLE_HUNGER_RATE));
      }
      
      if(profile.hasAbility(ArchetypeRegistry.WEAVING)){
         player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAVING, 110, 0, false, false, true));
      }
      
      if(profile.hasAbility(ArchetypeRegistry.MOONLIT_CAVE_SPIDER) && player.getEntityWorld().getRegistryKey().equals(ServerWorld.OVERWORLD)){
         long timeOfDay = player.getEntityWorld().getTimeOfDay();
         int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
         int curPhase = day % 8;
         int moonLevel = Math.abs(-curPhase+4); // 0 - new moon, 4 - full moon
         
         if(moonLevel == 2){
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 110, 0, false, false, true));
         }else if(moonLevel == 3){
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 110, 0, false, false, true));
         }else if(moonLevel == 4){
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 110, 0, false, false, true));
         }
      }
   }
   
   public static void importantAttributes(IArchetypeProfile profile, ServerPlayerEntity player){
      if(profile.hasAbility(ArchetypeRegistry.GOOD_SWIMMER)){
         if(player.isSubmergedInWater()){
            StatusEffectInstance conduitPower = new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 110, 0, false, false, true);
            player.addStatusEffect(conduitPower);
         }
         MinecraftUtils.attributeEffect(player, EntityAttributes.WATER_MOVEMENT_EFFICIENCY, 1f, EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"swim_buff"),false);
      }else{
         MinecraftUtils.attributeEffect(player, EntityAttributes.WATER_MOVEMENT_EFFICIENCY, 1f, EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"swim_buff"),true);
      }
      
      if(profile.hasAbility(ArchetypeRegistry.GREAT_SWIMMER) && player.isTouchingWaterOrRain()){
         MinecraftUtils.attributeEffect(player, EntityAttributes.MOVEMENT_SPEED, CONFIG.getDouble(ArchetypeRegistry.GREAT_SWIMMER_MOVE_SPEED_MODIFIER), EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"great_swimmer"),false);
      }else{
         MinecraftUtils.attributeEffect(player, EntityAttributes.MOVEMENT_SPEED, CONFIG.getDouble(ArchetypeRegistry.GREAT_SWIMMER_MOVE_SPEED_MODIFIER), EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"great_swimmer"),true);
      }
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.MOVEMENT_SPEED, -1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"fortify"),!profile.isFortifyActive());
      MinecraftUtils.attributeEffect(player, EntityAttributes.JUMP_STRENGTH, -1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"fortify"),!profile.isFortifyActive());
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.MOVEMENT_SPEED, CONFIG.getDouble(ArchetypeRegistry.FUNGUS_SPEED_BOOST_MULTIPLIER), EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"fungus_speed_boost"),!profile.isFungusBoosted());
      
      Entity vehicle = player.getVehicle();
      boolean gaveReach = false;
      if(vehicle != null){
         if(!vehicle.getCommandTags().stream().filter(s -> s.contains("$"+MOD_ID+".spirit_mount")).toList().isEmpty()){
            MinecraftUtils.attributeEffect(player, EntityAttributes.BLOCK_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.MOUNTED_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"mounted_reach"),false);
            MinecraftUtils.attributeEffect(player, EntityAttributes.ENTITY_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.MOUNTED_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"mounted_reach"),false);
            MinecraftUtils.attributeEffect(player, EntityAttributes.SAFE_FALL_DISTANCE, 10, EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"mounted_fall"),false);
            gaveReach = true;
         }
      }
      if(!gaveReach){
         MinecraftUtils.attributeEffect(player, EntityAttributes.BLOCK_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.MOUNTED_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"mounted_reach"),true);
         MinecraftUtils.attributeEffect(player, EntityAttributes.ENTITY_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.MOUNTED_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"mounted_reach"),true);
         MinecraftUtils.attributeEffect(player, EntityAttributes.SAFE_FALL_DISTANCE, 10, EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"mounted_fall"),true);
      }
   }
   
   public static void attributes(IArchetypeProfile profile, ServerPlayerEntity player){
      MinecraftUtils.attributeEffect(player, EntityAttributes.SCALE, -0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"half_sized"),!profile.hasAbility(ArchetypeRegistry.HALF_SIZED));
      MinecraftUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH, -0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"half_sized"),!profile.hasAbility(ArchetypeRegistry.HALF_SIZED));
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.SCALE, 0.25, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"tall_sized"),!profile.hasAbility(ArchetypeRegistry.TALL_SIZED));
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.SCALE, 0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"giant_sized"),!profile.hasAbility(ArchetypeRegistry.GIANT_SIZED));
      MinecraftUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH, 1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"giant_sized"),!profile.hasAbility(ArchetypeRegistry.GIANT_SIZED));
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.SCALE, 0.75, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"massive_sized"),!profile.hasAbility(ArchetypeRegistry.MASSIVE_SIZED));
      MinecraftUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH, 1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"massive_sized"),!profile.hasAbility(ArchetypeRegistry.MASSIVE_SIZED));
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.ENTITY_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.LONG_ARMS_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"long_arms"),!profile.hasAbility(ArchetypeRegistry.LONG_ARMS));
      MinecraftUtils.attributeEffect(player, EntityAttributes.BLOCK_INTERACTION_RANGE, CONFIG.getDouble(ArchetypeRegistry.LONG_ARMS_RANGE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"long_arms"),!profile.hasAbility(ArchetypeRegistry.LONG_ARMS));
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.MOVEMENT_SPEED, CONFIG.getDouble(ArchetypeRegistry.SPEEDY_SPEED_BOOST), EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"speedy"),!profile.hasAbility(ArchetypeRegistry.SPEEDY));
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.SNEAKING_SPEED, CONFIG.getDouble(ArchetypeRegistry.SNEAKY_SPEED_BOOST), EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, Identifier.of(MOD_ID,"sneaky"),!profile.hasAbility(ArchetypeRegistry.SNEAKY));
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.JUMP_STRENGTH, CONFIG.getDouble(ArchetypeRegistry.JUMPY_JUMP_BOOST), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"jumpy"),!profile.hasAbility(ArchetypeRegistry.JUMPY));
      MinecraftUtils.attributeEffect(player, EntityAttributes.SAFE_FALL_DISTANCE, CONFIG.getDouble(ArchetypeRegistry.JUMPY_JUMP_BOOST)*10, EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"jumpy"),!profile.hasAbility(ArchetypeRegistry.JUMPY));
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.ATTACK_KNOCKBACK, CONFIG.getDouble(ArchetypeRegistry.HARD_HITTER_KNOCKBACK_INCREASE), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"hard_hitter"),!profile.hasAbility(ArchetypeRegistry.HARD_HITTER));
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.ATTACK_SPEED, CONFIG.getDouble(ArchetypeRegistry.HASTY_ATTACK_SPEED_INCREASE), EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"hasty"),!profile.hasAbility(ArchetypeRegistry.HASTY));
      
      MinecraftUtils.attributeEffect(player, EntityAttributes.SAFE_FALL_DISTANCE, CONFIG.getDouble(ArchetypeRegistry.RESILIENT_JOINTS_EXTRA_FALL_BLOCKS), EntityAttributeModifier.Operation.ADD_VALUE, Identifier.of(MOD_ID,"resilient_joints"),!profile.hasAbility(ArchetypeRegistry.RESILIENT_JOINTS));
      
      long timeOfDay = player.getEntityWorld().getTimeOfDay();
      int day = (int) (timeOfDay/24000L % Integer.MAX_VALUE);
      int curPhase = day % 8;
      int moonLevel = Math.abs(-curPhase+4); // 0 - new moon, 4 - full moon
      for(int i = 0; i <= 4; i++){
         Identifier id = Identifier.of(MOD_ID,"slime_moonlit_"+i);
         if(moonLevel == i && profile.hasAbility(ArchetypeRegistry.MOONLIT_SLIME) && profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM)){
            MinecraftUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH, CONFIG.getDouble(ArchetypeRegistry.MOONLIT_SLIME_HEALTH_PER_PHASE) * i, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, id,false);
            MinecraftUtils.attributeEffect(player, EntityAttributes.SCALE, CONFIG.getDouble(ArchetypeRegistry.MOONLIT_SLIME_SIZE_PER_PHASE) * i, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, id,false);
         }else{
            MinecraftUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH, CONFIG.getDouble(ArchetypeRegistry.MOONLIT_SLIME_HEALTH_PER_PHASE) * i, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE, id,true);
            MinecraftUtils.attributeEffect(player, EntityAttributes.SCALE, CONFIG.getDouble(ArchetypeRegistry.MOONLIT_SLIME_SIZE_PER_PHASE) * i, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, id,true);
         }
      }
   }
   
   public static void deathReductionSize(IArchetypeProfile profile, ServerPlayerEntity player){
      if(profile.getHealthUpdate() != 0){
         double scale = -(1 - Math.pow(0.5,profile.getDeathReductionSizeLevel()));
         MinecraftUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
         MinecraftUtils.attributeEffect(player, EntityAttributes.SCALE,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),true);
         MinecraftUtils.attributeEffect(player, EntityAttributes.MAX_HEALTH,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),false);
         MinecraftUtils.attributeEffect(player, EntityAttributes.SCALE,scale, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, Identifier.of(MOD_ID,"death_reduction_size_level"),false);
         if(player.getMaxHealth() >= profile.getHealthUpdate()){
            player.setHealth(profile.getHealthUpdate());
            profile.setHealthUpdate(0);
         }
      }
   }
   
   public static void inventoryItem(IArchetypeProfile profile, ServerPlayerEntity player, ItemStack stack){
      HashMap<Item, Pair<Float,Integer>> map = null;
      boolean unusualFood = ArchetypeRegistry.TUFF_FOODS.containsKey(stack.getItem())
            || ArchetypeRegistry.COPPER_FOODS.containsKey(stack.getItem())
            || ArchetypeRegistry.IRON_FOODS.containsKey(stack.getItem())
            || stack.isIn(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS)
            || stack.isIn(ArchetypeRegistry.SLIME_GROW_ITEMS)
            || stack.isOf(Items.WARPED_FUNGUS)
            || stack.isOf(Items.HONEYCOMB);
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
      
      ItemStack mainhand = player.getMainHandStack();
      ItemStack offhand = player.getOffHandStack();
      boolean inHand = stack.equals(mainhand) || stack.equals(offhand);
      boolean shouldHaveGolemEatComponent = (inHand && map != null && player.getHealth() < player.getMaxHealth());
      boolean shouldHaveGelatianEatComponent = inHand && profile.getDeathReductionSizeLevel() != 0
            && ((profile.hasAbility(ArchetypeRegistry.SLIME_TOTEM) && stack.isIn(ArchetypeRegistry.SLIME_GROW_ITEMS))
            || (profile.hasAbility(ArchetypeRegistry.MAGMA_TOTEM) && stack.isIn(ArchetypeRegistry.MAGMA_CUBE_GROW_ITEMS)));
      boolean shouldHaveFungusEatComponent = inHand && profile.hasAbility(ArchetypeRegistry.FUNGUS_SPEED_BOOST) && stack.isOf(Items.WARPED_FUNGUS);
      boolean shouldHaveWaxEatComponent = inHand && profile.hasAbility(ArchetypeRegistry.WAX_SHIELD) && stack.isOf(Items.HONEYCOMB) && player.getAbsorptionAmount() < CONFIG.getDouble(ArchetypeRegistry.WAX_SHIELD_MAX_HEALTH);
      if(shouldHaveGolemEatComponent){ // Add component
         if(!stack.contains(DataComponentTypes.CONSUMABLE)){
            Pair<Float,Integer> pair = map.get(stack.getItem());
            ConsumableComponent comp = ConsumableComponent.builder().sound(SoundEvents.ENTITY_GENERIC_EAT).useAction(UseAction.EAT).consumeSeconds(pair.getRight()/20.0f * durationMod).consumeParticles(true).build();
            stack.set(DataComponentTypes.CONSUMABLE,comp);
         }
      }else if(shouldHaveGelatianEatComponent){
         if(!stack.contains(DataComponentTypes.CONSUMABLE)){
            ConsumableComponent comp = ConsumableComponent.builder().sound(SoundEvents.ENTITY_GENERIC_EAT).useAction(UseAction.EAT).consumeSeconds(CONFIG.getInt(ArchetypeRegistry.GELATIAN_GROW_ITEM_EAT_DURATION)/20.0f).consumeParticles(true).build();
            stack.set(DataComponentTypes.CONSUMABLE,comp);
         }
      }else if(shouldHaveFungusEatComponent){
         if(!stack.contains(DataComponentTypes.CONSUMABLE)){
            ConsumableComponent comp = ConsumableComponent.builder().sound(Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_STRIDER_EAT)).useAction(UseAction.EAT).consumeSeconds(CONFIG.getInt(ArchetypeRegistry.FUNGUS_SPEED_BOOST_CONSUME_DURATION)/20.0f).consumeParticles(true).build();
            stack.set(DataComponentTypes.CONSUMABLE,comp);
         }
      }else if(shouldHaveWaxEatComponent){
         if(!stack.contains(DataComponentTypes.CONSUMABLE)){
            ConsumableComponent comp = ConsumableComponent.builder().sound(Registries.SOUND_EVENT.getEntry(SoundEvents.ITEM_HONEYCOMB_WAX_ON)).useAction(UseAction.EAT).consumeSeconds(CONFIG.getInt(ArchetypeRegistry.WAX_SHIELD_CONSUME_DURATION)/20.0f).consumeParticles(true).build();
            stack.set(DataComponentTypes.CONSUMABLE,comp);
         }
      }else if(unusualFood && stack.contains(DataComponentTypes.CONSUMABLE)){ // Remove component
         stack.remove(DataComponentTypes.CONSUMABLE);
      }
   }
   
   private static void stopEat(IArchetypeProfile profile, ServerPlayerEntity player){
      if(player.isUsingItem()){
         if(player.getActiveItem().contains(DataComponentTypes.FOOD)){
            boolean stopUsing = false;
            if(profile.hasAbility(ArchetypeRegistry.CARNIVORE) && !player.getActiveItem().isIn(ArchetypeRegistry.CARNIVORE_FOODS)){
               stopUsing = true;
            }else if(profile.hasAbility(ArchetypeRegistry.COPPER_EATER) || profile.hasAbility(ArchetypeRegistry.IRON_EATER) || profile.hasAbility(ArchetypeRegistry.TUFF_EATER)){
               stopUsing = true;
            }
            
            if(stopUsing){
               player.getItemCooldownManager().set(player.getActiveItem(),20);
               player.stopUsingItem();
               player.sendMessage(Text.translatable("text.ancestralarchetypes.cannot_eat").formatted(Formatting.RED,Formatting.ITALIC),true);
               SoundUtils.playSongToPlayer(player, SoundEvents.ENTITY_PLAYER_BURP,1,0.5f);
            }
         }
      }
   }
}
