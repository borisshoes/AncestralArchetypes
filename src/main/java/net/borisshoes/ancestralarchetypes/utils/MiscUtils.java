package net.borisshoes.ancestralarchetypes.utils;

import com.google.common.collect.HashMultimap;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.items.GraphicalItem;
import net.borisshoes.ancestralarchetypes.misc.ArcanaCompat;
import net.borisshoes.ancestralarchetypes.mixins.LivingEntityAccessor;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.*;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;
import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.hasArcana;

public class MiscUtils {
   
   public static void blockWithShield(LivingEntity entity, float damage){
      if(entity.isBlocking()){
         ((LivingEntityAccessor) entity).invokeDamageShield(damage);
         SoundUtils.playSound(entity.getWorld(),entity.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS,1f,1f);
         
         if(hasArcana){
            ArcanaCompat.triggerShieldOfFortitude(entity,damage);
         }
      }
   }
   
   public static LasercastResult lasercast(World world, Vec3d startPos, Vec3d direction, double distance, boolean blockedByShields, Entity entity){
      Vec3d rayEnd = startPos.add(direction.multiply(distance));
      BlockHitResult raycast = world.raycast(new RaycastContext(startPos,rayEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));
      EntityHitResult entityHit;
      List<Entity> hits = new ArrayList<>();
      Box box = new Box(startPos,raycast.getPos());
      box = box.expand(2);
      // Primary hitscan check
      do{
         entityHit = ProjectileUtil.raycast(entity,startPos,raycast.getPos(),box, e -> e instanceof LivingEntity && !e.isSpectator() && !hits.contains(e),100000);
         if(entityHit != null && entityHit.getType() == HitResult.Type.ENTITY){
            hits.add(entityHit.getEntity());
         }
      }while(entityHit != null && entityHit.getType() == HitResult.Type.ENTITY);
      
      // Secondary hitscan check to add lenience
      List<Entity> hits2 = world.getOtherEntities(entity, box, (e)-> e instanceof LivingEntity && !e.isSpectator() && !hits.contains(e) && inRange(e,startPos,raycast.getPos()));
      hits.addAll(hits2);
      hits.sort(Comparator.comparingDouble(e->e.distanceTo(entity)));
      
      if(!blockedByShields){
         return new LasercastResult(startPos, raycast.getPos(), direction, hits);
      }
      
      List<Entity> hits3 = new ArrayList<>();
      Vec3d endPoint = raycast.getPos();
      for(Entity hit : hits){
         boolean blocked = false;
         if(hit instanceof ServerPlayerEntity hitPlayer && hitPlayer.isBlocking()){
            double dp = hitPlayer.getRotationVecClient().normalize().dotProduct(direction.normalize());
            blocked = dp < -0.6;
            if(blocked){
               SoundUtils.playSound(world,hitPlayer.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS,1f,1f);
               endPoint = startPos.add(direction.normalize().multiply(direction.normalize().dotProduct(hitPlayer.getPos().subtract(startPos)))).subtract(direction.normalize());
            }
         }
         hits3.add(hit);
         if(blocked){
            break;
         }
      }
      
      return new LasercastResult(startPos,endPoint,direction,hits3);
   }
   
   public record LasercastResult(Vec3d startPos, Vec3d endPos, Vec3d direction, List<Entity> sortedHits){}
   
   public static boolean inRange(Entity e, Vec3d start, Vec3d end){
      double range = .25;
      Box entityBox = e.getBoundingBox().expand(e.getTargetingMargin());
      double len = end.subtract(start).length();
      Vec3d trace = end.subtract(start).normalize().multiply(range);
      int i = 0;
      Vec3d t2 = trace.multiply(i);
      while(t2.length() < len){
         Vec3d t3 = start.add(t2);
         Box hitBox = new Box(t3.x-range,t3.y-range,t3.z-range,t3.x+range,t3.y+range,t3.z+range);
         if(entityBox.intersects(hitBox)){
            return true;
         }
         t2 = trace.multiply(i);
         i++;
      }
      return false;
   }
   
   public static void giveStacks(PlayerEntity player, ItemStack... stacks){
      returnItems(new SimpleInventory(stacks),player);
   }
   
   public static void returnItems(Inventory inv, PlayerEntity player){
      if(inv == null) return;
      for(int i=0; i<inv.size();i++){
         ItemStack stack = inv.getStack(i).copy();
         if(!stack.isEmpty()){
            inv.setStack(0,ItemStack.EMPTY);
            
            ItemEntity itemEntity;
            boolean bl = player.getInventory().insertStack(stack);
            if(!bl || !stack.isEmpty()){
               itemEntity = player.dropItem(stack, false);
               if(itemEntity == null) continue;
               itemEntity.resetPickupDelay();
               itemEntity.setOwner(player.getUuid());
               continue;
            }
            stack.setCount(1);
            itemEntity = player.dropItem(stack, false);
            if(itemEntity != null){
               itemEntity.setDespawnImmediately();
            }
            player.currentScreenHandler.sendContentUpdates();
         }
      }
   }
   
   public static UUID getUUIDOrNull(String str){
      try{
         return UUID.fromString(str);
      }catch(Exception e){
         return null;
      }
   }
   
   public static ArchetypeAbility abilityFromTag(String tag){
      int lastDotIndex = tag.lastIndexOf(".");
      if (lastDotIndex == -1) {
         return null;
      }
      return ArchetypeRegistry.ABILITIES.get(Identifier.of(MOD_ID,tag.substring(lastDotIndex + 1)));
   }
   
   private static final ArrayList<Pair<Formatting,Integer>> COLOR_MAP = new ArrayList<>(Arrays.asList(
         new Pair<>(Formatting.BLACK,0x000000),
         new Pair<>(Formatting.DARK_BLUE,0x0000AA),
         new Pair<>(Formatting.DARK_GREEN,0x00AA00),
         new Pair<>(Formatting.DARK_AQUA,0x00AAAA),
         new Pair<>(Formatting.DARK_RED,0xAA0000),
         new Pair<>(Formatting.DARK_PURPLE,0xAA00AA),
         new Pair<>(Formatting.GOLD,0xFFAA00),
         new Pair<>(Formatting.GRAY,0xAAAAAA),
         new Pair<>(Formatting.DARK_GRAY,0x555555),
         new Pair<>(Formatting.BLUE,0x5555FF),
         new Pair<>(Formatting.GREEN,0x55FF55),
         new Pair<>(Formatting.AQUA,0x55FFFF),
         new Pair<>(Formatting.RED,0xFF5555),
         new Pair<>(Formatting.LIGHT_PURPLE,0xFF55FF),
         new Pair<>(Formatting.YELLOW,0xFFFF55),
         new Pair<>(Formatting.WHITE,0xFFFFFF)
   ));
   
   public static Formatting getClosestFormatting(int colorRGB){
      Formatting closest = Formatting.WHITE;
      double cDist = Integer.MAX_VALUE;
      for(Pair<Formatting, Integer> pair : COLOR_MAP){
         int repColor = pair.getRight();
         double rDist = (((repColor>>16)&0xFF)-((colorRGB>>16)&0xFF))*0.30;
         double gDist = (((repColor>>8)&0xFF)-((colorRGB>>8)&0xFF))*0.59;
         double bDist = ((repColor&0xFF)-(colorRGB&0xFF))*0.11;
         double dist = rDist*rDist + gDist*gDist + bDist*bDist;
         if(dist < cDist){
            cDist = dist;
            closest = pair.getLeft();
         }
      }
      return closest;
   }
   
   public static MutableText withColor(MutableText text, int color){
      return text.setStyle(text.getStyle().withColor(color));
   }
   
   public static void outlineGUI(SimpleGui gui, int color, Text borderText){
      outlineGUI(gui,color,borderText,null);
   }
   
   public static void outlineGUI(SimpleGui gui, int color, Text borderText, List<Text> lore){
      for(int i = 0; i < gui.getSize(); i++){
         gui.clearSlot(i);
         GuiElementBuilder menuItem;
         boolean top = i/9 == 0;
         boolean bottom = i/9 == (gui.getSize()/9 - 1);
         boolean left = i%9 == 0;
         boolean right = i%9 == 8;
         
         if(top){
            if(left){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP_LEFT,color));
            }else if(right){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP_RIGHT,color));
            }else{
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP,color));
            }
         }else if(bottom){
            if(left){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM_LEFT,color));
            }else if(right){
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM_RIGHT,color));
            }else{
               menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_BOTTOM,color));
            }
         }else if(left){
            menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_LEFT,color));
         }else if(right){
            menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_RIGHT,color));
         }else{
            menuItem = GuiElementBuilder.from(GraphicalItem.withColor(GraphicalItem.GraphicItems.MENU_TOP,color));
         }
         
         if(borderText.getString().isEmpty()){
            menuItem.hideTooltip();
         }else{
            menuItem.setName(borderText).hideDefaultTooltip();
            if(lore != null && !lore.isEmpty()){
               for(Text text : lore){
                  menuItem.addLoreLine(text);
               }
            }
         }
         
         gui.setSlot(i,menuItem);
      }
   }
   
   public static MutableText removeItalics(MutableText text){
      Style parentStyle = Style.EMPTY.withColor(Formatting.DARK_PURPLE).withItalic(false).withBold(false).withUnderline(false).withObfuscated(false).withStrikethrough(false);
      return text.setStyle(text.getStyle().withParent(parentStyle));
   }
   
   public static RegistryEntry<Enchantment> getEnchantment(RegistryKey<Enchantment> key){
      if(AncestralArchetypes.SERVER == null){
         AncestralArchetypes.log(2,"Attempted to access Enchantment "+key.toString()+" before DRM is available");
         return null;
      }
      Optional<RegistryEntry.Reference<Enchantment>> opt = AncestralArchetypes.SERVER.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(key);
      return opt.orElse(null);
   }
   
   
   public static void attributeEffect(LivingEntity livingEntity, RegistryEntry<EntityAttribute> attribute, double value, EntityAttributeModifier.Operation operation, Identifier identifier, boolean remove){
      boolean hasMod = livingEntity.getAttributes().hasModifierForAttribute(attribute,identifier);
      if(hasMod && remove){ // Remove the modifier
         HashMultimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = HashMultimap.create();
         map.put(attribute, new EntityAttributeModifier(identifier, value, operation));
         livingEntity.getAttributes().removeModifiers(map);
      }else if(!hasMod && !remove){ // Add the modifier
         HashMultimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = HashMultimap.create();
         map.put(attribute, new EntityAttributeModifier(identifier, value, operation));
         livingEntity.getAttributes().addTemporaryModifiers(map);
      }
   }
}
