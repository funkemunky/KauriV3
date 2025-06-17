package dev.brighten.ac.handler.entity;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityMetadataProvider;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.WPacketPlayOutEntity;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class FakeMob {
    private final int entityId;
    private final EntityType type;

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

    public void spawn(boolean invisible, Location location, List<EntityData<?>> objects, APlayer... players) {
        if(watching.size() > 0) {
            despawn();
        }

        watching = new ArrayList<>();
        for (APlayer player : players) {
            if(invisible) {
                EntityData<?> entityData = new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20);

                objects.add(entityData);
            }
            WrapperPlayServerSpawnLivingEntity packet = new WrapperPlayServerSpawnLivingEntity(entityId, UUID.randomUUID(), type,
                    new Vector3d(location.getX(), location.getY(), location.getZ()), location.getYaw(), location.getPitch(), location.getYaw(),
                    new Vector3d(0, 0, 0), objects);

            player.sendPacketSilently(packet);
            watching.add(player);
        }

        Anticheat.INSTANCE.getFakeTracker().trackEntity(this);
    }

    public void setInvisible(boolean invisible) {
        List<EntityData<?>> entityMetadata = new ArrayList<>();

        if(invisible) {
            EntityData<?> entityData = new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20);

            entityMetadata.add(entityData);
        } else {
            EntityData<?> entityData = new EntityData<>(0, EntityDataTypes.BYTE, (byte) ~0x20);

            entityMetadata.add(entityData);
        }

        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, entityMetadata);

        watching.forEach(player -> player.sendPacketSilently(packet));
    }

    public void despawn() {
        for (APlayer aPlayer : watching) {
            PacketPlayOutEntityDestroy destroyEntity = new PacketPlayOutEntityDestroy(entityId);

            aPlayer.sendPacketSilently(destroyEntity);
        }
        watching = Collections.emptyList();

        Anticheat.INSTANCE.getFakeTracker().untrackEntity(this);
    }

    public void move(double dx, double dy, double dz) {
        WPacketPlayOutEntity packet = WPacketPlayOutEntity.builder().id(entityId).x(dx).y(dy).z(dz).moved(true).build();

        for (APlayer player : watching) {
            player.sendPacketSilently(packet);
        }
    }

    public void move(double dx, double dy, double dz, float dyaw, float dpitch) {
        WPacketPlayOutEntity packet = WPacketPlayOutEntity.builder().id(entityId).x(dx).y(dy).z(dz).yaw(dyaw)
                .pitch(dpitch).moved(true).looked(true).build();

        for (APlayer player : watching) {
            player.sendPacketSilently(packet);
        }
    }

    public void move(float dyaw, float dpitch) {
        WPacketPlayOutEntity packet = WPacketPlayOutEntity.builder().id(entityId).yaw(dyaw).pitch(dpitch)
                .looked(true).build();

        for (APlayer player : watching) {
            player.sendPacketSilently(packet);
        }
    }

    public void teleport(double x, double y, double z, float yaw, float pitch) {
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(entityId, new Vector3d(x, y, z), yaw, pitch, false);

        for (APlayer player : watching) {
            player.sendPacketSilently(packet);
        }
    }
}
