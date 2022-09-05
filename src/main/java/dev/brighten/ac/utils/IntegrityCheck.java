package dev.brighten.ac.utils;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.SneakyThrows;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

public class IntegrityCheck {

    private static WrappedClass classSystem = Reflections.getClass("java.lang.System");
    private static WrappedMethod exitMethod = classSystem.getMethod("exit", int.class);

    public static void checkIntegrity() {
        File file = getPlugin("EnterpriseLoader");

        if(file == null) {
            exit(0);
            return;
        }

        long hash = getHashOfFile(file);

        if(!acceptableHashes.contains(hash)) {
            System.out.println("Bad loader file!");
            exit(0);
        }
    }

    private static void exit(int number) {
        exitMethod.invoke(null, number);
    }

    private static byte[] getBytes(InputStream inputStream) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private static File getPlugin(String pl) {
        Plugin targetPlugin = null;
        String msg = "";
        final File pluginDir = new File("plugins");
        if (!pluginDir.isDirectory()) {
            return null;
        }
        File pluginFile = new File(pluginDir, pl + ".jar");
        if (!pluginFile.isFile()) {
            for (final File f : pluginDir.listFiles()) {
                try {
                    if (f.getName().endsWith(".jar")) {
                        final PluginDescriptionFile pdf = Anticheat.INSTANCE.getPluginInstance()
                                .getPluginLoader().getPluginDescription(f);
                        if (pdf.getName().equalsIgnoreCase(pl)) {
                            return f;
                        }
                    }
                }
                catch (InvalidDescriptionException e2) {
                    return null;
                }
            }
        }
        return null;
    }

    @SneakyThrows
    private static long getHashOfFile(File file) {
        byte[] bits = getBytes(new FileInputStream(file));

        CRC32 crc = new CRC32();
        crc.update(ByteBuffer.wrap(bits));

        return crc.getValue();
    }

    private static final LongList acceptableHashes = new LongArrayList(Arrays.asList(3912178420L));


}
