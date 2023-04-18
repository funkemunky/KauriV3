package dev.brighten.ac.utils;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.reflections.Reflections;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.SneakyThrows;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

public class IntegrityCheck {

    private static final WrappedClass SYSTEM_CLASS = Reflections.getClass("java.lang.System");
    private static final WrappedMethod EXIT_METHOD = SYSTEM_CLASS.getMethod("exit", int.class);

    public static void checkIntegrity() {
        File file = getPlugin();

        if(file == null) {
            exit();
            return;
        }

        long hash = getHashOfFile(file);

        if(!acceptableHashes.contains(hash)) {
            exit();
        }
    }

    private static void exit() {
        EXIT_METHOD.invoke(null, 0);
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

    private static File getPlugin() {
        final File pluginDir = new File("plugins");
        if (!pluginDir.isDirectory()) {
            return null;
        }
        File pluginFile = new File(pluginDir, "EnterpriseLoader.jar");
        if (!pluginFile.isFile()) {
            File[] files = pluginDir.listFiles();

            if(files == null) return null;

            for (final File f : files) {
                try {
                    if (f.getName().endsWith(".jar")) {
                        final PluginDescriptionFile pdf = Anticheat.INSTANCE.getPluginInstance()
                                .getPluginLoader().getPluginDescription(f);
                        if (pdf.getName().equalsIgnoreCase("EnterpriseLoader")) {
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

    private static final LongList acceptableHashes = new LongArrayList(Arrays.asList(981789340L, 3477115375L, 948780235L));


}
