package dev.endeavours.locator;
import com.mojang.blaze3d.platform.InputConstants;
import dev.endeavours.locator.client.FriendsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
@Mod(value = LocatorFriends.MOD_ID, dist = Dist.CLIENT)
public class LocatorFriends {
 public static final String MOD_ID = "endeavours_locator";
 private static final KeyMapping OPEN = new KeyMapping("key.endeavours_locator.open",
  InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9,
  KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID,"controls")));
 public LocatorFriends(IEventBus modBus,ModContainer container) {
  container.registerExtensionPoint(IConfigScreenFactory.class,(mc,parent)->new FriendsScreen(parent));
  modBus.addListener(this::registerKeys);
  NeoForge.EVENT_BUS.register(this);
 }
 private void registerKeys(RegisterKeyMappingsEvent event) {event.register(OPEN);}
 @SubscribeEvent public void onTick(ClientTickEvent.Post event){
  while(OPEN.consumeClick()){
   Minecraft mc=Minecraft.getInstance();
   if(mc.screen==null)mc.setScreen(new FriendsScreen(null));
  }
 }
}
