package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
   
   @Final
   @Shadow
   protected ServerPlayer player;
   
   @ModifyExpressionValue(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;", ordinal = 1))
   private ItemStack archetypes$silkTouch(ItemStack original){
      if(original.isEmpty() && AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.SILK_TOUCH)){
         ItemStack silkStack = new ItemStack(Items.WOODEN_PICKAXE);
         silkStack.enchant(MinecraftUtils.getEnchantment(Enchantments.SILK_TOUCH),1);
         return silkStack;
      }
      return original;
   }
}
