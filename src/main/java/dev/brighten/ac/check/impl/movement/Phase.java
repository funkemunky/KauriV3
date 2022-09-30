package dev.brighten.ac.check.impl.movement;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WTimedAction;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.Helper;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.Materials;
import dev.brighten.ac.utils.XMaterial;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.TickTimer;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@CheckData(name = "Phase", checkId = "phase", type = CheckType.MOVEMENT)
public class Phase extends Check {

    private static final Set<Material> allowedMaterials = EnumSet.noneOf(Material.class);

    static {
        Arrays.stream(Material.values())
                .filter(mat -> mat.name().contains("BANNER") || mat.name().contains("BREWING")
                        || mat.name().contains("CAULDRON") || mat.name().contains("PISTON"))
                .forEach(allowedMaterials::add);

        allowedMaterials.add(XMaterial.VINE.parseMaterial());
        allowedMaterials.add(XMaterial.CAKE.parseMaterial());
        if (ProtocolVersion.getGameVersion().isOrAbove(ProtocolVersion.V1_14)) {
            allowedMaterials.add(XMaterial.SCAFFOLDING.parseMaterial());
        }
    }

    public Phase(APlayer player) {
        super(player);
    }

    private final Timer lastFlag = new TickTimer(5);
    private KLocation fromWhereShitAintBad = null;


    WTimedAction<WPacketPlayInFlying> packet = (packet, now) -> {
        if(!packet.isMoved() || player.getCreation().isNotPassed(800L)
                || ((player.getInfo().lastRespawn.isNotPassed(500L)
                || player.getMovement().getMoveTicks() == 0) && lastFlag.isPassed(12))
                || player.getInfo().isCreative() || player.getInfo().isCanFly()) {
            return;
        }

        SimpleCollisionBox fromBox = player.getMovement().getFrom().getBox().copy(), toBox = fromBox.copy();

        double deltaX = player.getMovement().getDeltaX(), deltaY = player.getMovement().getDeltaY(),
                deltaZ = player.getMovement().getDeltaZ();

        List<SimpleCollisionBox> collisions = Helper.getCollisions(player, fromBox.copy().addCoord(deltaX, deltaY, deltaZ),
                Materials.SOLID);

        for (SimpleCollisionBox collision : collisions) {
            deltaY = collision.calculateYOffset(toBox, deltaY);
        }

        toBox.offset(0, deltaY, 0);

        for (SimpleCollisionBox collision : collisions) {
            deltaX = collision.calculateXOffset(toBox, deltaX);
        }

        toBox.offset(deltaX, 0, 0);

        for (SimpleCollisionBox collision : collisions) {
            deltaZ = collision.calculateZOffset(toBox, deltaZ);
        }

        toBox.offset(0, 0, deltaZ);

        debug("(%s): new=[%.3f, %.3f, %.3f] old=[%.3f, %.3f, %.3f]", deltaX, deltaY, deltaZ,
                player.getMovement().getDeltaX(), player.getMovement().getDeltaY(),
                player.getMovement().getDeltaZ());
    };
}
