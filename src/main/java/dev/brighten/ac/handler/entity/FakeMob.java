package dev.brighten.ac.handler.entity;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntity;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityEffect;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutEntityTeleport;
import dev.brighten.ac.packet.wrapper.out.WPacketPlayOutRemoveEntityEffect;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.EntityZombie;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class FakeMob {
    private int entityId;
    private EntityType type;
    private EntityZombie zombie;

    private List<APlayer> watching = Collections.emptyList();

    public FakeMob(World world) {
        entityId = ThreadLocalRandom.current().nextInt(15000, 20000);

        zombie = new EntityZombie(((CraftWorld)world).getHandle());
        entityId = zombie.getId();
    }

    public void spawn(Location location, APlayer... players) {
        if(watching.size() > 0) {
            despawn();
        }

        zombie = new EntityZombie(((CraftWorld)location.getWorld()).getHandle());
        zombie.setInvisible(true);
        zombie.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        zombie.setHealth(20f);

        entityId = zombie.getId();
        watching = new ArrayList<>();
        for (APlayer player : players) {
            PacketPlayOutSpawnEntityLiving living = new PacketPlayOutSpawnEntityLiving(zombie);
            player.sendPacket(living);
            watching.add(player);
        }

        Anticheat.INSTANCE.getFakeTracker().trackEntity(this);
    }

    public void setInvisible(boolean invisible) {
        if(invisible) {
            WPacketPlayOutEntityEffect packet = WPacketPlayOutEntityEffect.builder().entityId(entityId)
                    .effectId(PotionEffectType.INVISIBILITY.getId() & 255).amplifier((byte)0).duration(32767).flags((byte)0x02).build();

            for (APlayer player : watching) {
                player.sendPacket(packet);
            }
            zombie.setInvisible(true);
        } else {
            WPacketPlayOutRemoveEntityEffect packet = WPacketPlayOutRemoveEntityEffect.builder()
                    .effect(PotionEffectType.INVISIBILITY).entityId(entityId).build();

            for (APlayer player : watching) {
                player.sendPacket(packet);
            }
            zombie.setInvisible(false);
        }
    }

    public void despawn() {
        for (APlayer aPlayer : watching) {
            PacketPlayOutEntityDestroy destroyEntity = new PacketPlayOutEntityDestroy(entityId);

            aPlayer.sendPacket(destroyEntity);
        }
        watching = Collections.emptyList();
        zombie = null;

        Anticheat.INSTANCE.getFakeTracker().untrackEntity(this);
    }

    public void move(double dx, double dy, double dz) {
        WPacketPlayOutEntity packet = WPacketPlayOutEntity.builder().x(dx).y(dy).z(dz).moved(true).build();

        for (APlayer player : watching) {
            player.sendPacket(packet);
        }
    }

    public void move(double dx, double dy, double dz, float dyaw, float dpitch) {
        WPacketPlayOutEntity packet = WPacketPlayOutEntity.builder().x(dx).y(dy).z(dz).yaw(dyaw)
                .pitch(dpitch).moved(true).looked(true).build();

        for (APlayer player : watching) {
            player.sendPacket(packet);
        }
    }

    public void move(float dyaw, float dpitch) {
        WPacketPlayOutEntity packet = WPacketPlayOutEntity.builder().yaw(dyaw).pitch(dpitch)
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
