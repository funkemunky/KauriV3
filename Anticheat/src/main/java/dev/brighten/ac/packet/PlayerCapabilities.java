package dev.brighten.ac.packet;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;
import lombok.Builder;
@Builder
public class PlayerCapabilities {
    public boolean isInvulnerable;
    public boolean isFlying;
    public boolean canFly;
    public boolean canInstantlyBuild;
    public boolean mayBuild = true;
    public float flySpeed = 0.05F;
    public float walkSpeed = 0.1F;

    public PlayerCapabilities() {

    }

    public PlayerCapabilities(WrapperPlayServerPlayerAbilities packet) {
        this.isInvulnerable = packet.isInGodMode();
        this.isFlying = packet.isFlying();
        this.canFly = packet.isFlightAllowed();
        this.canInstantlyBuild = packet.isInCreativeMode();
        this.flySpeed = packet.getFlySpeed();
        this.walkSpeed = packet.getFOVModifier();
    }
}
