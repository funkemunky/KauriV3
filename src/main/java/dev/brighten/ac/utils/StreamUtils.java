package dev.brighten.ac.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class StreamUtils {
    public static void streamCopy(InputStream in, OutputStream out) throws IOException {
        assert (in != null);
        assert (out != null);
        ReadableByteChannel inChannel = Channels.newChannel(in);
        WritableByteChannel outChannel = Channels.newChannel(out);
        channelCopy(inChannel, outChannel);
    }

    /**
     * A fast method to copy bytes from one channel to another; uses direct 16k
     * buffers to minimize copies and OS overhead.
     * @author http://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/
     * @param src - a non-null readable bytechannel to read the data from
     * @param dest - a non-null writeable byte channel to write the data to
     */
    public static void channelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
        assert (src != null);
        assert (dest != null);
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        while (src.read(buffer) != -1) {
            // prepare the buffer to be drained
            buffer.flip();
            // write to the channel, may block
            dest.write(buffer);
            // If partial transfer, shift remainder down
            // If buffer is empty, same as doing clear()
            buffer.compact();
        }

        // EOF will leave buffer in fill state
        buffer.flip();

        // make sure the buffer is fully drained.
        while (buffer.hasRemaining()) {
            dest.write(buffer);
        }
    }

}
