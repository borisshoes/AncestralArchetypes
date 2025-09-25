package net.borisshoes.ancestralarchetypes.mixins;

import net.minecraft.block.BlockState;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FallingBlockEntity.class)
public interface FallingBlockEntityAccessor {
   
   @Invoker("<init>")
   static FallingBlockEntity newFallingBlock(World world, double x, double y, double z, BlockState blockState){
      throw new AssertionError();
   }
}
