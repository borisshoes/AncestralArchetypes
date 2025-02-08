package net.borisshoes.ancestralarchetypes.items;

import net.borisshoes.ancestralarchetypes.ArchetypeAbility;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.cca.IArchetypeProfile;
import net.borisshoes.ancestralarchetypes.utils.SoundUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.profile;

public class WingGliderItem extends AbilityItem{
   
   public WingGliderItem(Settings settings){
      super(ArchetypeRegistry.GLIDER, settings);
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext){
      return Items.LEATHER_CHESTPLATE;
   }
   
   @Override
   public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected){
      super.inventoryTick(stack, world, entity, slot, selected);
      
      if(!(entity instanceof ServerPlayerEntity player)) return;
      IArchetypeProfile profile = profile(player);
      double glidePercentage = ((double)profile.getGlideTime() / profile.getMaxGlideTime());
      boolean onCooldown = profile.getAbilityCooldown(this.ability) > 0;
      if(onCooldown){
         stack.setDamage(stack.getMaxDamage() - 1);
      }else{
         stack.setDamage((int) ((stack.getMaxDamage()-1) * (1-glidePercentage)));
      }
      
      DyedColorComponent dyedColorComponent = stack.get(DataComponentTypes.DYED_COLOR);
      if(dyedColorComponent == null || dyedColorComponent.rgb() != profile.getGliderColor()){
         stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(profile.getGliderColor(),false));
      }
      
      if(stack.equals(player.getEquippedStack(EquipmentSlot.CHEST)) && player.isGliding()){
         int glideValue = (int) (glidePercentage * 100);
         char[] unicodeChars = {'▁', '▂', '▃', '▅', '▆', '▇', '▌'};
         StringBuilder message = new StringBuilder("≈ ");
         for (int i = 0; i < 10; i++) {
            int segmentValue = glideValue - (i * 10);
            if (segmentValue <= 0) {
               message.append(unicodeChars[0]);
            } else if (segmentValue >= 10) {
               message.append(unicodeChars[unicodeChars.length - 1]);
            } else {
               int charIndex = (int) ((double) segmentValue / 10 * (unicodeChars.length - 1));
               message.append(unicodeChars[charIndex]);
            }
         }
         message.append(" ≈");
         player.sendMessage(Text.literal(message.toString()).formatted(Formatting.GRAY), true);
      }
   }
   
   @Override
   public ActionResult use(World world, PlayerEntity user, Hand hand){
      if(!(user instanceof ServerPlayerEntity player)) return ActionResult.PASS;
      IArchetypeProfile profile = profile(player);
      if(profile.getAbilityCooldown(this.ability) > 0){
         player.sendMessage(Text.translatable("text.ancestralarchetypes.ability_on_cooldown").formatted(Formatting.RED,Formatting.ITALIC),true);
         SoundUtils.playSongToPlayer(player, SoundEvents.BLOCK_FIRE_EXTINGUISH,0.25f,0.8f);
         return ActionResult.PASS;
      }
      
      return super.use(world, user, hand);
   }
}
