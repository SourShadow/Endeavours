package dev.endeavours.locator.client;
import net.neoforged.fml.loading.FMLPaths;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public final class FriendsConfig {
 private static final Path FILE=FMLPaths.CONFIGDIR.get().resolve("endeavours-locator-friends.properties");
 private static final Properties DATA=new Properties();
 public record Color(String label,int rgb){}
 public static final Color[] COLORS={
  new Color("Vanilla",-1),new Color("Pink",0xff69b4),new Color("Red",0xf34b4b),
  new Color("Orange",0xffa33b),new Color("Yellow",0xffe867),new Color("Green",0x64cd76),
  new Color("Cyan",0x44d6d9),new Color("Blue",0x649fff),new Color("Purple",0xb783e9),new Color("White",0xffffff)};
 static {if(Files.isRegularFile(FILE))try(InputStream in=Files.newInputStream(FILE)){DATA.load(in);}
 catch(IOException e){System.err.println("[Endeavours Locator] Failed reading config "+e);}}
 private FriendsConfig(){}
 public static boolean valid(String n){return n!=null&&n.matches("[A-Za-z0-9_]{1,32}");}
 private static String key(String n,String kind){return "p."+n.toLowerCase(Locale.ROOT)+"."+kind;}
 public static boolean trackedOnly(){return Boolean.parseBoolean(DATA.getProperty("trackedOnly","false"));}
 public static void toggleMode(){DATA.setProperty("trackedOnly",Boolean.toString(!trackedOnly()));save();}
 public static boolean tracked(String n){return Boolean.parseBoolean(DATA.getProperty(key(n,"tracked"),"false"));}
 public static boolean visible(String n){return !trackedOnly()||tracked(n);}
 public static void toggleTracked(String n){if(valid(n)){DATA.setProperty(key(n,"tracked"),Boolean.toString(!tracked(n)));save();}}
 public static void add(String n){if(valid(n)){DATA.setProperty(key(n,"tracked"),"true");save();}}
 public static boolean head(String n){return Boolean.parseBoolean(DATA.getProperty(key(n,"head"),"false"));}
 public static void toggleHead(String n){if(valid(n)){DATA.setProperty(key(n,"head"),Boolean.toString(!head(n)));save();}}
 public static int rgb(String n){try{return Integer.parseInt(DATA.getProperty(key(n,"rgb"),"-1"));}catch(NumberFormatException e){return -1;}}
 public static String colorName(String n){int rgb=rgb(n);for(Color c:COLORS)if(c.rgb()==rgb)return c.label();return "Vanilla";}
 public static void cycleColor(String n){if(!valid(n))return;int now=rgb(n),index=0;
  for(int i=0;i<COLORS.length;i++)if(COLORS[i].rgb()==now){index=i;break;}
  DATA.setProperty(key(n,"rgb"),Integer.toString(COLORS[(index+1)%COLORS.length].rgb()));save();
 }
 public static List<String> names(){List<String> result=new ArrayList<>();
  for(String k:DATA.stringPropertyNames())if(k.startsWith("p.")&&k.endsWith(".tracked")){
   String n=k.substring(2,k.length()-8);if(valid(n))result.add(n);
  }return result;
 }
 private static void save(){try{
  Files.createDirectories(FILE.getParent());
  try(OutputStream out=Files.newOutputStream(FILE)){DATA.store(out,"Endeavours Locator Friends");}
 }catch(IOException e){System.err.println("[Endeavours Locator] Failed saving config "+e);}}
}
