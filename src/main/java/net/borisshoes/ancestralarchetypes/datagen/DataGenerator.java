package net.borisshoes.ancestralarchetypes.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class DataGenerator implements DataGeneratorEntrypoint {
   
   @Override
   public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator){
      FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
      pack.addProvider(ItemTagGenerator::new);
      pack.addProvider(DamageTagGenerator::new);
   }
}
