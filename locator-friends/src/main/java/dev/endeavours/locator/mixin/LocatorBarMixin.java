package dev.endeavours.locator.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.endeavours.locator.client.FriendsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.UUID;
@Mixin(LocatorBarRenderer.class)
public class LocatorBarMixin {
 @WrapOperation(method="*",at=@At(value="INVOKE",
  target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
 private void endeavours$marker(GuiGraphicsExtractor g,RenderPipeline pipeline,Identifier sprite,
  int x,int y,int w,int h,int tint,Operation<Void> original,@Local TrackedWaypoint waypoint) {
  if(w!=9||h!=9||waypoint==null){original.call(g,pipeline,sprite,x,y,w,h,tint);return;}
  Minecraft mc=Minecraft.getInstance();
  if(mc.getConnection()==null){original.call(g,pipeline,sprite,x,y,w,h,tint);return;}
  UUID id=waypoint.id().left().orElse(null);
  if(id==null){original.call(g,pipeline,sprite,x,y,w,h,tint);return;}
  PlayerInfo info=mc.getConnection().getPlayerInfo(id);
  if(info==null||info.getProfile()==null){original.call(g,pipeline,sprite,x,y,w,h,tint);return;}
  String name=info.getProfile().name();
  if(!FriendsConfig.valid(name)){original.call(g,pipeline,sprite,x,y,w,h,tint);return;}
  if(!FriendsConfig.visible(name))return;
  int rgb=FriendsConfig.rgb(name),t=rgb<0?tint:0xff000000|rgb;
  if(!FriendsConfig.head(name)){original.call(g,pipeline,sprite,x,y,w,h,t);return;}
  g.fill(x+1,y,x+8,y+9,t|0xff000000);
  g.fill(x,y+1,x+9,y+8,t|0xff000000);
  PlayerFaceExtractor.extractRenderState(g,info.getSkin().body().texturePath(),
   x+1,y+1,7,info.showHat(),false,-1);
 }
}
