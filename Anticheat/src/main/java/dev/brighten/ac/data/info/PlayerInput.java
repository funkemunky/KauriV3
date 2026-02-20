package dev.brighten.ac.data.info;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;

public record PlayerInput(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sprint, boolean shift) {

    public static PlayerInput getFromPacket(WrapperPlayClientPlayerInput packet) {
        return new PlayerInput(packet.isForward(), packet.isBackward(), packet.isLeft(),
                packet.isRight(), packet.isJump(), packet.isSprint(), packet.isShift());
    }

    public static final PlayerInput NONE = new PlayerInput(false, false, false,
            false, false, false, false);
}