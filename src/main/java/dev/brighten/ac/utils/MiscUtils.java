package dev.brighten.ac.utils;

import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.reflections.impl.CraftReflection;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class MiscUtils {

    public static Material[] array = Arrays.stream(Material.values())
            .filter(mat -> mat.name().contains("LEGACY"))
            .toArray(Material[]::new);

    public static Map<EntityType, Vector> entityDimensions = new HashMap<>();

    public static boolean containsIgnoreCase(String toCheck, String contains) {
        return toCheck.toLowerCase().contains(contains.toLowerCase());
    }

    public static void close(AutoCloseable... closeables) {
        try {
            for (AutoCloseable closeable : closeables) if (closeable != null) closeable.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static boolean isInMaterialBB(World world, SimpleCollisionBox entityBox, XMaterial xmaterial) {
        int startX = MathHelper.floor_double(entityBox.xMin);
        int startY = MathHelper.floor_double(entityBox.yMin);
        int startZ = MathHelper.floor_double(entityBox.zMin);
        int endX = MathHelper.floor_double(entityBox.xMax + 1D);
        int endY = MathHelper.floor_double(entityBox.yMax + 1D);
        int endZ = MathHelper.floor_double(entityBox.zMax + 1D);

        for(int x = startX ; x < endX ; x++) {
            for(int y = startY ; y < endY ; y++) {
                for(int z = startZ ; z < endZ ; z++) {
                    Location loc = new Location(world, x, y, z);
                    Optional<Block> op = BlockUtils.getBlockAsync(loc);

                    if(op.isPresent()) {
                        if(XMaterial.matchXMaterial(op.get().getType()).equals(xmaterial))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isInMaterialBB(World world, SimpleCollisionBox entityBox, int bitmask) {
        int startX = MathHelper.floor_double(entityBox.xMin);
        int startY = MathHelper.floor_double(entityBox.yMin);
        int startZ = MathHelper.floor_double(entityBox.zMin);
        int endX = MathHelper.floor_double(entityBox.xMax + 1D);
        int endY = MathHelper.floor_double(entityBox.yMax + 1D);
        int endZ = MathHelper.floor_double(entityBox.zMax + 1D);

        for(int x = startX ; x < endX ; x++) {
            for(int y = startY ; y < endY ; y++) {
                for(int z = startZ ; z < endZ ; z++) {
                    Location loc = new Location(world, x, y, z);
                    Optional<Block> op = BlockUtils.getBlockAsync(loc);

                    if(op.isPresent()) {
                        if(Materials.checkFlag(op.get().getType(), bitmask))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    public static <T> List<T> combineLists(List<T> one, List<T> two) {
        List<T> newList = new ArrayList<>();

        newList.addAll(one);
        newList.addAll(two);

        return newList;
    }
    public static boolean endsWith(double value, String string) {
        return String.valueOf(value).endsWith(string);
    }

    public static void sendMessage(CommandSender player, String message, Object... objects) {
        String toSend = String.format(Color.translate(message), objects);
        if(player instanceof Player) {
            ((Player)player).spigot().sendMessage(TextComponent.fromLegacyText(toSend));
        } else player.sendMessage(toSend);
    }

    private static final WrappedField ticksField;

    static {
        switch (ProtocolVersion.getGameVersion()) {
            case V1_19: {
                ticksField = MinecraftReflection.minecraftServer
                        .getFieldByName("S");
                break;
            }
            case V1_18_2:
            case V1_18:
            case V1_17_1:
            case V1_17: {
                ticksField = MinecraftReflection.minecraftServer.getFieldByName("V");
                break;
            }
            default: {
                ticksField = MinecraftReflection.minecraftServer.getFieldByName("ticks");
                break;
            }
        }
    }
    private static Object minecraftServer = null;
    //TODO Make this use the new abstraction system.
    public static int currentTick() {
        if(minecraftServer == null) minecraftServer = CraftReflection.getMinecraftServer();
        return ticksField.get(minecraftServer);
    }


    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    public static LongStream listToStream(Collection<Long> collection) {
        LongStream.Builder longBuilder = LongStream.builder();
        collection.forEach(longBuilder::add);
        return longBuilder.build();
    }

    /** Nik's method **/
    public static <E> E randomElement(final Collection<? extends E> collection) {
        if (collection.size() == 0) return null;
        int index = new Random().nextInt(collection.size());

        if (collection instanceof List) {
            return ((List<? extends E>) collection).get(index);
        } else {
            Iterator<? extends E> iter = collection.iterator();
            for (int i = 0; i < index; i++) iter.next();
            return iter.next();
        }
    }

    public static String timeStampToDate(long timeStamp) {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/YYYY (hh:mm)");

        format.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        Date date = new Date(timeStamp);

        return format.format(date);
    }

    public static void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            int lenght;
            byte[] buf = new byte[1024];

            while ((lenght = in.read(buf)) > 0)
            {
                out.write(buf, 0, lenght);
            }

            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getOridinal(Material material) {
        int i = 0;
        for (Material mat : array) {
            if(mat.getId() == material.getId()) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static BaseComponent[] toComponentArray(TextComponent message) {
        return TextComponent.fromLegacyText(message.toLegacyText());
    }

    public static String injectColor(String string, String color) {
        String[] split = string.split("");

        return Arrays.stream(split).map(s -> color + s).collect(Collectors.joining());
    }

    public static Material getById(int id) {
        return Arrays.stream(Material.values()).filter(mat -> mat.getId() == id).findFirst()
                .orElse(Material.getMaterial("AIR"));
    }

    public static String line(String color) {
        return color + Color.Strikethrough + "-----------------------------------------------------";
    }

    public static String line() {
        return Color.Strikethrough + "-----------------------------------------------------";
    }

    public static String lineNoStrike(String color) {
        return color + "-----------------------------------------------------";
    }

    public static long copy(InputStream from, OutputStream to) throws IOException {
        if(from == null || to == null) return 0;

        byte[] buf = new byte[4096];
        long total = 0L;

        int r ;
        while((r = from.read(buf)) != -1) {
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    private static final WrappedClass materialClass = new WrappedClass(Material.class);
    public static Material match(String material) {
        if(ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_13)) {
            return materialClass
                    .getMethod("matchMaterial", String.class, boolean.class)
                    .invoke(null, material, material.contains("LEGACY_"));
        } return Material.getMaterial(material.replace("LEGACY_", ""));
    }

    public static <T> List<T> combine(List<T> one, List<T> two) {
        List<T> newList = new ArrayList<>();
        if(one != null)
            newList.addAll(one);
        if(two != null)
            newList.addAll(two);

        return newList;
    }

    public static void printToConsole(String string, Object... objects) {
        Bukkit.getConsoleSender().sendMessage(Color.translate(String.format(string, objects)));
    }

    public static void printToConsole(String string) {
        Bukkit.getConsoleSender().sendMessage(Color.translate(string));
    }

    public static String trimEnd(String string) {
        if(string.length() <= 1) {
            return string;
        }
        return string.substring(0, string.length() - 1);
    }

    public static String[] splitIntoLine(String input, int maxCharInLine) {

        StringTokenizer tok = new StringTokenizer(input, " ");
        StringBuilder output = new StringBuilder(input.length());
        int lineLen = 0;
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken();

            while (word.length() > maxCharInLine) {
                output.append(word.substring(0, maxCharInLine - lineLen) + "\n");
                word = word.substring(maxCharInLine - lineLen);
                lineLen = 0;
            }

            if (lineLen + word.length() > maxCharInLine) {
                output.append("\n");
                lineLen = 0;
            }
            output.append("&f" + word + " ");

            lineLen += word.length() + 1;
        }
        // output.split();
        // return output.toString();
        return output.toString().split("\n");
    }

    public static <T> T getResult(Supplier<T> consumer) {
        return consumer.get();
    }

    public static String lineNoStrike() {
        return "-----------------------------------------------------";
    }

    //Stolen from Luke
    public static boolean contains(Object[] array, Object obj) {
        for (Object object : array) if (object != null && object.equals(obj)) return true;
        return false;
    }

    public static <T> T parseObjectFromString(String s, Class<T> clazz) throws Exception {
        return clazz.getConstructor(new Class[] {String.class}).newInstance(s);
    }

    /* MAKE SURE TO ONLY RUN THIS METHOD IN onLoad() AND NO WHERE ELSE */
    public static void registerCommand(String name, JavaPlugin plugin) {
        plugin.getDescription().getCommands().put(name, new HashMap<>());
    }

    public static ItemStack createItem(Material material, int amount, String name, String... lore) {
        ItemStack thing = new ItemStack(material, amount);
        ItemMeta thingm = thing.getItemMeta();
        thingm.setDisplayName(Color.translate(name));
        ArrayList<String> loreList = new ArrayList<>();
        for (String string : lore) {
            loreList.add(Color.translate(string));
        }
        thingm.setLore(loreList);
        thing.setItemMeta(thingm);
        return thing;
    }

    public static boolean arraysSimilar(String[] one, String[] two) {
        if(one.length != two.length) return false;

        for (int i = 0; i < one.length; i++) {
            String a1 = one[i], a2 = two[i];

            if(!a1.equalsIgnoreCase(a2)) {
                return false;
            }
        }
        return true;
    }

    public static <T> T getArgOrNull(T[] array, int index) {
        if(array.length > index) {
            return array[index];
        }
        return null;
    }
}

