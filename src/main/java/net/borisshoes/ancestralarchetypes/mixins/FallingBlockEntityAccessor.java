package net.borisshoes.ancestralarchetypes.mixins;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FallingBlockEntity.class)
public interface FallingBlockEntityAccessor {
   
   @Invoker("<init>")
   static FallingBlockEntity newFallingBlock(Level world, double x, double y, double z, BlockState blockState){
      throw new AssertionError();
   }
}
