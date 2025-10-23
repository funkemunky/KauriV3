package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.util.Vector3d;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import me.hydro.emulator.util.mcp.MathHelper;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class MiscUtils {

    public static Map<EntityType, Vector3d> entityDimensions = new HashMap<>();

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

    public static boolean isInMaterialBB(APlayer player, SimpleCollisionBox entityBox, int bitmask) {
        int startX = MathHelper.floor_double(entityBox.minX);
        int startY = MathHelper.floor_double(entityBox.minY);
        int startZ = MathHelper.floor_double(entityBox.minZ);
        int endX = MathHelper.floor_double(entityBox.maxX + 1D);
        int endY = MathHelper.floor_double(entityBox.maxY + 1D);
        int endZ = MathHelper.floor_double(entityBox.maxZ + 1D);

        for(int x = startX ; x < endX ; x++) {
            for(int y = startY ; y < endY ; y++) {
                for(int z = startZ ; z < endZ ; z++) {
                    StateType type = player.getBlockUpdateHandler().getBlock(x, y, z).getType();

                    if(Materials.checkFlag(type, bitmask))
                        return true;
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

    public static String injectColor(String string, String color) {
        String[] split = string.split("");

        return Arrays.stream(split).map(s -> color + s).collect(Collectors.joining());
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

    public static <T> List<T> combine(List<T> one, List<T> two) {
        List<T> newList = new ArrayList<>();
        if(one != null)
            newList.addAll(one);
        if(two != null)
            newList.addAll(two);

        return newList;
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

