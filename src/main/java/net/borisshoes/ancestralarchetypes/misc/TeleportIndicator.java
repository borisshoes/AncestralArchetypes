package net.borisshoes.ancestralarchetypes.misc;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.borislib.conditions.Conditions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.archetypesId;

public final class TeleportIndicator {
   private static final int TTL = 3;
   private static final Map<UUID, TeleportIndicator> INDICATORS = new HashMap<>();
   
   private final ServerPlayer player;
   private final ServerLevel world;
   private final ElementHolder holder;
   private final ItemDisplayElement display;
   private final int glowColor;
   private Vec3 pos;
   private int ttl;
   
   private TeleportIndicator(ServerPlayer player, Vec3 spot, ArchetypeAbility ability, int glowColor){
      this.player = player;
      this.world = player.level();
      this.pos = spot;
      this.ttl = TTL;
      this.glowColor = glowColor;
      this.holder = new ElementHolder();
      
      ItemStack displayStack = new ItemStack(Items.ENDER_EYE);
      if(PolymerResourcePackUtils.hasMainPack(player)){
         displayStack.set(DataComponents.ITEM_MODEL, archetypesId(ability.id()));
      }
      this.display = new ItemDisplayElement(displayStack);
      this.display.setItemDisplayContext(ItemDisplayContext.FIXED);
      this.display.setBillboardMode(Display.BillboardConstraints.CENTER);
      this.display.setGlowing(false);
      this.display.setGlowColorOverride(this.glowColor);
      this.display.setViewRange(256.0f);
      this.display.setBrightness(Brightness.FULL_BRIGHT);
      this.display.setScale(new Vector3f(0.5f, 0.5f, 0.5f));
      this.holder.addElement(this.display);
      
      new ManualAttachment(this.holder, this.world, () -> this.pos);
      this.holder.startWatching(player);
   }
   
   public static void show(ServerPlayer player, Vec3 spot, Vec3 eyePos, ArchetypeAbility ability, int glowColor){
      TeleportIndicator indicator = INDICATORS.get(player.getUUID());
      if(indicator == null || indicator.player != player || indicator.player.level() != indicator.world){
         if(indicator != null) indicator.destroy();
         indicator = new TeleportIndicator(player, spot, ability, glowColor);
         INDICATORS.put(player.getUUID(), indicator);
      }else{
         indicator.refresh(spot);
      }
      indicator.updateGlow(eyePos);
   }
   
   public static void hide(ServerPlayer player){
      TeleportIndicator indicator = INDICATORS.remove(player.getUUID());
      if(indicator != null) indicator.destroy();
   }
   
   public static void tickAll(){
      if(INDICATORS.isEmpty()) return;
      Iterator<TeleportIndicator> it = INDICATORS.values().iterator();
      while(it.hasNext()){
         TeleportIndicator indicator = it.next();
         if(--indicator.ttl <= 0 || indicator.player.hasDisconnected() || indicator.player.isRemoved() || indicator.player.level() != indicator.world){
            indicator.destroy();
            it.remove();
         }else{
            double distance = indicator.display.getCurrentPos().distanceTo(indicator.player.position());
            float scale = (float) Mth.clamp(4.5 / 128.0 * distance + 0.5, 0.5, 5.0) + 0.25f;
            indicator.display.setScale(new Vector3f(scale, scale, scale));
         }
      }
   }
   
   private void refresh(Vec3 spot){
      this.ttl = TTL;
      if(this.pos.distanceToSqr(spot) > 1.0e-6){
         this.pos = spot;
         this.holder.tick();
      }
   }
   
   private void updateGlow(Vec3 eyePos){
      boolean hasLineOfSight = checkLineOfSight(eyePos, this.pos);
      boolean isBlinded = this.player.getEffect(MobEffects.BLINDNESS) != null || Conditions.getConditionValue(this.player.getUUID(),Conditions.NEARSIGHT) != Conditions.NEARSIGHT.value().getBase();
      this.display.setGlowing(!hasLineOfSight || isBlinded);
      this.display.tick();
   }
   
   private boolean checkLineOfSight(Vec3 eyePos, Vec3 targetPos){
      ClipContext context = new ClipContext(
            eyePos,
            targetPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            this.player
      );
      HitResult result = this.world.clip(context);
      return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(targetPos) < 0.1;
   }
   
   private void destroy(){
      this.holder.destroy();
   }
}

