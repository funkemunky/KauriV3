package dev.brighten.ac.packet.listener.functions;

import dev.brighten.ac.packet.listener.PacketInfo;

@FunctionalInterface
public interface PacketListener {
    void onEvent(PacketInfo info);
}
