package dev.brighten.ac.handler.entity;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.objects.WrappedWatchableObject;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntity;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityMetadata;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityTeleport;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutSpawnEntityLiving;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class FakeMob {
    private int entityId;
    private EntityType type;

    private List<APlayer> watching = Collections.emptyList();

    public FakeMob(EntityType type) {
        entityId = ThreadLocalRandom.current().nextInt(15000, 20000);
        this.type = type;
    }

    /*
    protected void b(int i, boolean flag) {
        byte b0 = this.datawatcher.getByte(0);
        if (flag) {
            this.datawatcher.watch(0, (byte)(b0 | 1 << i));
        } else {
            this.datawatcher.watch(0, (byte)(b0 & ~(1 << i)));
        }

    }
     */
    public void spawn(boolean invisible, Location location, APlayer... players) {
        spawn(invisible, location, new ArrayList<>(), players);
    }

    public void spawn(boolean invisible, Location location, List<WrappedWatchableObject> objects, APlayer... players) {
        if(watching.size() > 0) {
            despawn();
        }

        watching = new ArrayList<>();
        for (APlayer player : players) {
            if(invisible) {
                objects.add(new WrappedWatchableObject(0, 0, (byte)((byte)1 << 5)));
            }
            WPacketPlayOutSpawnEntityLiving packet = WPacketPlayOutSpawnEntityLiving.builder()
                    .entityId(entityId)
                    .type(type)
                    .x(location.getX())
                    .y(location.getY())
                    .z(location.getZ())
                    .yaw(location.getYaw())
                    .pitch(location.getPitch())
                    .headYaw(location.getYaw())
                    .motionX(0)
                    .motionY(0)
                    .motionZ(0)
                    .watchedObjects(objects)
                    .build();

            player.sendPacket(packet);
            watching.add(player);
        }

        Anticheat.INSTANCE.getFakeTracker().trackEntity(this);
    }

    public void setInvisible(boolean invisible) {
        List<WrappedWatchableObject> objects = new ArrayList<>();

        if(invisible) {
            objects.add(new WrappedWatchableObject(0, 0, (byte)((byte)1 << 5)));
        } else {
            objects.add(new WrappedWatchableObject(0, 0, (byte)~((byte)1 << 5)));
        }

        WPacketPlayOutEntityMetadata packet = WPacketPlayOutEntityMetadata.builder()
                .entityId(entityId)
                .watchedObjects(objects)
                .build();

        watching.forEach(player -> player.sendPacket(packet));
    }

    public void despawn() {
        for (APlayer aPlayer : watching) {
            PacketPlayOutEntityDestroy destroyEntity = new PacketPlayOutEntityDestroy(entityId);

            aPlayer.sendPacket(destroyEntity);
        }
        watching = Collections.emptyList();

        Anticheat.INSTANCE.getFakeTracker().untrackEntity(this);
    }

    public void move(double dx, double dy, double dz) {
        WPacketPlayOutEntity packet = WPacketPlayOutEntity.builder().id(entityId).x(dx).y(dy).z(dz).moved(true).build();

        for (APlayer player : watching) {
            player.sendPacket(packet);
        }
    }

    public void move(double dx, double dy, double dz, float dyaw, float dpitch) {
        WPacketPlayOutEntity packet = WPacketPlayOutEntity.builder().id(entityId).x(dx).y(dy).z(dz).yaw(dyaw)
                .pitch(dpitch).moved(true).looked(true).build();

        for (APlayer player : watching) {
            player.sendPacket(packet);
        }
    }

    public void move(float dyaw, float dpitch) {
        WPacketPlayOutEntity packet = WPacketPlayOutEntity.builder().id(entityId).yaw(dyaw).pitch(dpitch)
                .looked(true).build();

        for (APlayer player : watching) {
            player.sendPacket(packet);
        }
    }

    public void teleport(double x, double y, double z, float yaw, float pitch) {
        WPacketPlayOutEntityTeleport packet = WPacketPlayOutEntityTeleport.builder()
                .entityId(entityId)
                .x(x).y(y).z(z).yaw(yaw).pitch(pitch).onGround(false)
                .build();

        for (APlayer player : watching) {
            player.sendPacket(packet);
        }
    }
}
