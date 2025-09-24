package net.borisshoes.ancestralarchetypes.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.borisshoes.ancestralarchetypes.AncestralArchetypes;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.borislib.utils.MinecraftUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
   
   @Final
   @Shadow
   protected ServerPlayerEntity player;
   
   @ModifyExpressionValue(method = "tryBreakBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;", ordinal = 1))
   private ItemStack archetypes$silkTouch(ItemStack original){
      if(original.isEmpty() && AncestralArchetypes.profile(player).hasAbility(ArchetypeRegistry.SILK_TOUCH)){
         ItemStack silkStack = new ItemStack(Items.WOODEN_PICKAXE);
         silkStack.addEnchantment(MinecraftUtils.getEnchantment(Enchantments.SILK_TOUCH),1);
         return silkStack;
      }
      return original;
   }
}
