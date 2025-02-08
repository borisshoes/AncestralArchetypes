package net.borisshoes.ancestralarchetypes.items;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.borisshoes.ancestralarchetypes.ArchetypeRegistry;
import net.borisshoes.ancestralarchetypes.utils.ItemUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.Arrays;

import static net.borisshoes.ancestralarchetypes.AncestralArchetypes.MOD_ID;

public class GraphicalItem extends Item implements PolymerItem {
   
   public static final String GRAPHICS_TAG = "graphic_id";
   
   private static final ArrayList<Pair<Item,String>> MODEL_LIST = new ArrayList<>(Arrays.asList(
         new Pair<>(Items.STRUCTURE_VOID,"gui/confirm"),
         new Pair<>(Items.BARRIER,"gui/cancel"),
         new Pair<>(Items.SPECTRAL_ARROW,"gui/right_arrow"),
         new Pair<>(Items.SPECTRAL_ARROW,"gui/left_arrow"),
         new Pair<>(Items.NETHER_STAR,"gui/sort"),
         new Pair<>(Items.HOPPER,"gui/filter"),
         new Pair<>(Items.BLACK_STAINED_GLASS_PANE,"gui/black"),
         new Pair<>(Items.POTION,"gui/locked_potion"),
         new Pair<>(Items.SPLASH_POTION,"gui/locked_splash_potion"),
         new Pair<>(Items.LINGERING_POTION,"gui/locked_lingering_potion")
   ));
   
   private static final ArrayList<Pair<Item,Integer>> DYED_REPLACEMENTS = new ArrayList<>(Arrays.asList(
         new Pair<>(Items.BLACK_STAINED_GLASS_PANE,0x000000),
         new Pair<>(Items.BLUE_STAINED_GLASS_PANE,0x0000ff),
         new Pair<>(Items.BROWN_STAINED_GLASS_PANE,0x6b5341),
         new Pair<>(Items.GRAY_STAINED_GLASS_PANE,0x5c5c5c),
         new Pair<>(Items.CYAN_STAINED_GLASS_PANE,0x168e94),
         new Pair<>(Items.GREEN_STAINED_GLASS_PANE,0x04753a),
         new Pair<>(Items.LIGHT_BLUE_STAINED_GLASS_PANE,0x5ad2fa),
         new Pair<>(Items.LIGHT_GRAY_STAINED_GLASS_PANE,0xc7c7c7),
         new Pair<>(Items.LIME_STAINED_GLASS_PANE,0x4ded0e),
         new Pair<>(Items.MAGENTA_STAINED_GLASS_PANE,0xb306c9),
         new Pair<>(Items.ORANGE_STAINED_GLASS_PANE,0xff8800),
         new Pair<>(Items.PINK_STAINED_GLASS_PANE,0xff7dde),
         new Pair<>(Items.PURPLE_STAINED_GLASS_PANE,0x8502cc),
         new Pair<>(Items.RED_STAINED_GLASS_PANE,0xff000),
         new Pair<>(Items.WHITE_STAINED_GLASS_PANE,0xffffff),
         new Pair<>(Items.YELLOW_STAINED_GLASS_PANE,0xffff00)
   ));
   
   private static final ArrayList<String> DYEABLE_MODEL_LIST = new ArrayList<>(Arrays.asList(
         "gui/menu_horizontal",
         "gui/menu_vertical",
         "gui/menu_top",
         "gui/menu_bottom",
         "gui/menu_left",
         "gui/menu_right",
         "gui/menu_top_right",
         "gui/menu_top_left",
         "gui/menu_bottom_left",
         "gui/menu_bottom_right",
         "gui/menu_right_connector",
         "gui/menu_left_connector",
         "gui/menu_top_connector",
         "gui/menu_bottom_connector",
         "gui/page_bg"
   ));
   
   public GraphicalItem(Item.Settings settings){
      super(settings.registryKey(RegistryKey.of(RegistryKeys.ITEM,Identifier.of(MOD_ID,"graphical_item"))));
   }
   
   private ArrayList<Pair<Item, String>> getModels(){
      ArrayList<Pair<Item, String>> arrayList = new ArrayList<>(MODEL_LIST);
      for(String str : DYEABLE_MODEL_LIST){
         for(Pair<Item, Integer> pair : DYED_REPLACEMENTS){
            arrayList.add(new Pair<>(pair.getLeft(),str));
         }
         arrayList.add(new Pair<>(Items.GLASS_PANE,str));
         arrayList.add(new Pair<>(Items.LEATHER_CHESTPLATE,str));
      }
      return arrayList;
   }
   
   @Override
   public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context){
      String id = ItemUtils.getStringProperty(stack,GRAPHICS_TAG);
      for(Pair<Item, String> pair : getModels()){
         String[] split = pair.getRight().split("[/\\\\]");
         if(split[split.length-1].equals(id)){
            if(PolymerResourcePackUtils.hasMainPack(context)){
               return Identifier.of(MOD_ID,pair.getRight());
            }else{
               return Registries.ITEM.getKey(pair.getLeft()).get().getValue();
            }
         }
      }
      return Identifier.ofVanilla("barrier");
   }
   
   @Override
   public Item getPolymerItem(ItemStack itemStack, PacketContext context){
      String id = ItemUtils.getStringProperty(itemStack,GRAPHICS_TAG);
      
      for(Pair<Item, String> pair : MODEL_LIST){
         String[] split = pair.getRight().split("[/\\\\]");
         if(split[split.length-1].equals(id)){
            return pair.getLeft();
         }
      }
      
      for(String str : DYEABLE_MODEL_LIST){
         String[] split = str.split("[/\\\\]");
         if(split[split.length-1].equals(id)){
            if(PolymerResourcePackUtils.hasMainPack(context.getPlayer())){
               return Items.LEATHER_CHESTPLATE;
            }else{
               if(itemStack != null && itemStack.contains(DataComponentTypes.DYED_COLOR)){
                  return getItemFromColor(itemStack.get(DataComponentTypes.DYED_COLOR).rgb());
               }
               return Items.WHITE_STAINED_GLASS_PANE;
            }
         }
      }
      
      return Items.BARRIER;
   }
   
   private Item getItemFromColor(int colorRGB){
      Item closest = Items.GLASS_PANE;
      double cDist = Integer.MAX_VALUE;
      for(Pair<Item, Integer> pair : DYED_REPLACEMENTS){
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
   
   public static ItemStack with(String id){
      ItemStack stack = new ItemStack(ArchetypeRegistry.GRAPHICAL_ITEM);
      ItemUtils.putProperty(stack,GRAPHICS_TAG,id);
      return stack;
   }
   
   public static ItemStack with(GraphicItems id){
      ItemStack stack = new ItemStack(ArchetypeRegistry.GRAPHICAL_ITEM);
      ItemUtils.putProperty(stack,GRAPHICS_TAG,id.id);
      return stack;
   }
   
   public static ItemStack withColor(GraphicItems id, int color){
      ItemStack stack = new ItemStack(ArchetypeRegistry.GRAPHICAL_ITEM);
      ItemUtils.putProperty(stack,GRAPHICS_TAG,id.id);
      stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color, false));
      return stack;
   }
   
   public enum GraphicItems{
      CONFIRM("confirm"),
      CANCEL("cancel"),
      LEFT_ARROW("left_arrow"),
      RIGHT_ARROW("right_arrow"),
      SORT("sort"),
      FILTER("filter"),
      BLACK("black"),
      MENU_HORIZONTAL("menu_horizontal"),
      MENU_VERTICAL("menu_vertical"),
      MENU_TOP("menu_top"),
      MENU_BOTTOM("menu_bottom"),
      MENU_LEFT("menu_left"),
      MENU_RIGHT("menu_right"),
      MENU_TOP_RIGHT("menu_top_right"),
      MENU_TOP_LEFT("menu_top_left"),
      MENU_BOTTOM_LEFT("menu_bottom_left"),
      MENU_BOTTOM_RIGHT("menu_bottom_right"),
      MENU_RIGHT_CONNECTOR("menu_right_connector"),
      MENU_LEFT_CONNECTOR("menu_left_connector"),
      MENU_TOP_CONNECTOR("menu_top_connector"),
      MENU_BOTTOM_CONNECTOR("menu_bottom_connector"),
      TRANSMUTATION_BOOK("transmutation_book"),
      PAGE_BG("page_bg"),
      LOCKED_POTION("locked_potion"),
      LOCKED_SPLASH_POTION("locked_splash_potion"),
      LOCKED_LINGERING_POTION("locked_lingering_potion")
      ;
      
      public final String id;
      
      GraphicItems(String id){
         this.id = id;
      }
   }
}