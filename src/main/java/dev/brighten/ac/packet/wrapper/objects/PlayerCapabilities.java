package dev.brighten.ac.packet.wrapper.objects;

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
}
