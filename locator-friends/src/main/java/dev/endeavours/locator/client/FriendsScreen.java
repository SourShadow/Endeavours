package dev.endeavours.locator.client;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import java.util.*;
public class FriendsScreen extends Screen {
 private final Screen parent;
 private final int requestedPage;
 private int page,rows,pages;
 private List<String> names=List.of();
 private EditBox nameBox;
 private String notice="";
 public FriendsScreen(Screen parent){this(parent,0);}
 private FriendsScreen(Screen parent,int page){super(Component.literal("Endeavours Locator Friends"));this.parent=parent;this.requestedPage=page;}
 @Override protected void init(){
  super.init();
  int left=this.width/2-206;
  rows=Math.max(2,Math.min(7,(this.height-170)/23));
  names=collect();
  pages=Math.max(1,(names.size()+rows-1)/rows);
  page=Math.min(requestedPage,pages-1);
  addRenderableWidget(Button.builder(Component.literal(FriendsConfig.trackedOnly()?"Showing: TRACKED only":"Showing: ALL players"),
   b->{FriendsConfig.toggleMode();refresh();}).bounds(left,38,198,20).build());
  addRenderableWidget(Button.builder(Component.literal("Refresh players"),b->refresh()).bounds(left+208,38,198,20).build());
  for(int row=0;row<rows&&page*rows+row<names.size();row++){
   final String n=names.get(page*rows+row);int y=80+row*23;
   addRenderableWidget(Button.builder(Component.literal(FriendsConfig.tracked(n)?"Tracked":"Not tracked"),
    b->{FriendsConfig.toggleTracked(n);refresh();}).bounds(left+135,y,87,20).build());
   addRenderableWidget(Button.builder(Component.literal(FriendsConfig.colorName(n)),
    b->{FriendsConfig.cycleColor(n);refresh();}).bounds(left+226,y,87,20).build());
   addRenderableWidget(Button.builder(Component.literal(FriendsConfig.head(n)?"Head":"Dot"),
    b->{FriendsConfig.toggleHead(n);refresh();}).bounds(left+317,y,89,20).build());
  }
  int y=this.height-63;
  nameBox=new EditBox(font,left,y,152,20,Component.literal("Player name"));
  nameBox.setMaxLength(32);
  nameBox.setHint(Component.literal("Enter username"));
  addRenderableWidget(nameBox);
  addRenderableWidget(Button.builder(Component.literal("Add / track"),b->{
   String n=nameBox.getValue().trim();
   if(FriendsConfig.valid(n)){FriendsConfig.add(n);refresh();}
   else notice="Invalid Minecraft name.";
  }).bounds(left+156,y,90,20).build());
  addRenderableWidget(Button.builder(Component.literal("<"),b->{
   if(page>0)minecraft.setScreen(new FriendsScreen(parent,page-1));
  }).bounds(left+252,y,35,20).build()).active=page>0;
  addRenderableWidget(Button.builder(Component.literal(">"),b->{
   if(page<pages-1)minecraft.setScreen(new FriendsScreen(parent,page+1));
  }).bounds(left+290,y,35,20).build()).active=page<pages-1;
  addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(left+329,y,77,20).build());
 }
 private List<String> collect(){
  TreeSet<String> all=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
  all.addAll(FriendsConfig.names());
  if(minecraft!=null&&minecraft.getConnection()!=null)
   for(PlayerInfo p:minecraft.getConnection().getOnlinePlayers()){
    String n=p.getProfile().name();if(FriendsConfig.valid(n))all.add(n);
   }
  if(minecraft!=null&&minecraft.player!=null)all.remove(minecraft.player.getGameProfile().name());
  return new ArrayList<>(all);
 }
 private void refresh(){minecraft.setScreen(new FriendsScreen(parent,page));}
 @Override public void onClose(){minecraft.setScreen(parent);}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
  super.extractRenderState(g,mouseX,mouseY,delta);
  int left=width/2-206;
  g.text(font,"Endeavours Locator Friends",left+110,15,0xffffffff,true);
  g.text(font,"Track players and customize their markers.",left,65,0xffaaaaaa,false);
  for(int row=0;row<rows&&page*rows+row<names.size();row++){
   String n=names.get(page*rows+row);
   if(n.length()>19)n=n.substring(0,17)+"...";
   g.text(font,n,left+3,85+row*23,0xffffffff,false);
  }
  g.text(font,notice.isBlank()?"Page "+(page+1)+"/"+pages+" | F9 opens this menu":notice,
   left,height-85,0xffaaaaaa,false);
 }
}
