package dev.brighten.ac.packet.wrapper.impl;

import dev.brighten.ac.packet.wrapper.PacketConverter;
import dev.brighten.ac.packet.wrapper.in.*;
import dev.brighten.ac.packet.wrapper.objects.WrappedEnumDirection;
import dev.brighten.ac.packet.wrapper.out.*;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.stream.Collectors;

public class Processor_18 implements PacketConverter {
    @Override
    public WPacketPlayInFlying processFlying(Object object) {
        PacketPlayInFlying flying = (PacketPlayInFlying) object;
        return WPacketPlayInFlying.builder().x(flying.a()).y(flying.b()).z(flying.c()).yaw(flying.d())
                .pitch(flying.e()).onGround(flying.f()).moved(flying.g()).looked(flying.h()).build();
    }

    private static WrappedClass classUseEntity = new WrappedClass(PacketPlayInUseEntity.class);
    private static WrappedField fieldEntityId = classUseEntity.getFieldByName("a");
    @Override
    public WPacketPlayInUseEntity processUseEntity(Object object) {
        PacketPlayInUseEntity useEntity = (PacketPlayInUseEntity) object;

        return WPacketPlayInUseEntity.builder().entityId(fieldEntityId.get(useEntity))
                .action(WPacketPlayInUseEntity.EnumEntityUseAction.valueOf(useEntity.a().name()))
                .vector(useEntity.b() != null ? new Vector(useEntity.b().a, useEntity.b().b, useEntity.b().c) : null)
                .build();
    }

    private static final WrappedClass classAbilities = new WrappedClass(PacketPlayInAbilities.class);
    private static final WrappedField fieldFlySpeed = classAbilities.getFieldByType(float.class, 0),
            fieldWalkSpeed = classAbilities.getFieldByType(float.class, 1);
    @Override
    public WPacketPlayInAbilities processAbilities(Object object) {
        PacketPlayInAbilities abilities = (PacketPlayInAbilities) object;

        return WPacketPlayInAbilities.builder().allowedFlight(abilities.a()).flying(abilities.isFlying())
                .allowedFlight(abilities.c()).creativeMode(abilities.d()).flySpeed(fieldFlySpeed.get(abilities))
                .walkSpeed(fieldWalkSpeed.get(abilities)).build();
    }

    @Override
    public WPacketPlayInArmAnimation processAnimation(Object object) {
        PacketPlayInArmAnimation packet = (PacketPlayInArmAnimation) object;
        return WPacketPlayInArmAnimation.builder().timestamp(packet.timestamp).build();
    }

    @Override
    public WPacketPlayInBlockDig processBlockDig(Object object) {
        PacketPlayInBlockDig packet = (PacketPlayInBlockDig) object;

        BlockPosition pos = packet.a();

        return WPacketPlayInBlockDig.builder().blockPos(new IntVector(pos.getX(), pos.getY(), pos.getZ()))
                .direction(WrappedEnumDirection.valueOf(packet.b().name()))
                .digType(WPacketPlayInBlockDig.EnumPlayerDigType.valueOf(packet.c().name()))
                .build();
    }

    @Override
    public WPacketPlayInBlockPlace processBlockPlace(Object object) {
        PacketPlayInBlockPlace packet = (PacketPlayInBlockPlace) object;

        BlockPosition pos = packet.a();

        return WPacketPlayInBlockPlace.builder().blockPos(new IntVector(pos.getX(), pos.getY(), pos.getZ()))
                .direction(WrappedEnumDirection.values()[Math.min(packet.getFace(), 5)])
                .itemStack(CraftItemStack.asCraftMirror(packet.getItemStack()))
                .vecX(packet.d()).vecY(packet.e()).vecZ(packet.f())
                .build();
    }


    private static final WrappedClass classCloseWindow = new WrappedClass(PacketPlayInCloseWindow.class);
    private static final WrappedField fieldWindowId = classCloseWindow.getFieldByType(int.class, 0);
    @Override
    public WPacketPlayInCloseWindow processCloseWindow(Object object) {
        PacketPlayInCloseWindow packet = (PacketPlayInCloseWindow) object;

        return WPacketPlayInCloseWindow.builder().id(fieldWindowId.get(packet)).build();
    }

    @Override
    public WPacketPlayInEntityAction processEntityAction(Object object) {
        PacketPlayInEntityAction packet = (PacketPlayInEntityAction) object;

        return WPacketPlayInEntityAction.builder().action(WPacketPlayInEntityAction.EnumPlayerAction
                .valueOf(packet.b().name())).build();
    }

    @Override
    public WPacketPlayOutEntityEffect processEntityEffect(Object object) {
        PacketPlayOutEntityEffect packet = (PacketPlayOutEntityEffect) object;
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer());
        try {
            packet.b(serializer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return WPacketPlayOutEntityEffect.builder().entityId(serializer.e())
                .effectId(serializer.readByte())
                .duration(serializer.readByte())
                .duration(serializer.e())
                .flags(serializer.readByte()).build();
    }

    @Override
    public WPacketPlayOutPosition processServerPosition(Object object) {
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer());
        PacketPlayOutPosition position = (PacketPlayOutPosition) object;

        try {
            position.b(serializer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return WPacketPlayOutPosition.builder()
                .x(serializer.readDouble())
                .y(serializer.readDouble())
                .z(serializer.readDouble())
                .yaw(serializer.readFloat())
                .pitch(serializer.readFloat())
                .flags(PacketPlayOutPosition.EnumPlayerTeleportFlags.a(serializer.readUnsignedByte()).stream()
                        .map(f -> WPacketPlayOutPosition.EnumPlayerTeleportFlags.valueOf(f.name()))
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    public WPacketPlayOutAttachEntity processAttach(Object object) {
        PacketDataSerializer serial = new PacketDataSerializer(Unpooled.buffer());
        PacketPlayOutAttachEntity packet = (PacketPlayOutAttachEntity) object;

        try {
            packet.b(serial);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        return WPacketPlayOutAttachEntity.builder()
                .attachedEntityId(serial.readInt())
                .holdingEntityId(serial.readInt())
                .isLeashModifer((int)(serial.readUnsignedByte()) == 1)
                .build();
    }

    @Override
    public WPacketPlayOutEntity processOutEntity(Object object) {
        PacketDataSerializer serial = new PacketDataSerializer(Unpooled.buffer());
        PacketPlayOutEntity packet = (PacketPlayOutEntity) object;

        try {
            packet.b(serial);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int id = serial.e();
        byte x = 0, y = 0, z = 0, yaw = 0, pitch = 0;
        boolean ground = false, looked = false, moved = false;

        if(packet instanceof PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook) {
            x = serial.readByte();
            y = serial.readByte();
            z = serial.readByte();
            yaw = serial.readByte();
            pitch = serial.readByte();
            ground = serial.readBoolean();
            looked = moved = true;
        } else if(packet instanceof PacketPlayOutEntity.PacketPlayOutRelEntityMove) {
            x = serial.readByte();
            y = serial.readByte();
            z = serial.readByte();
            ground = serial.readBoolean();

            moved = true;
        } else if(packet instanceof PacketPlayOutEntity.PacketPlayOutEntityLook) {
            yaw = serial.readByte();
            pitch = serial.readByte();
            ground = serial.readBoolean();

            looked = true;
        }

        return WPacketPlayOutEntity.builder()
                .x(x / 32D).y(y / 32D).z(z / 32D).yaw(yaw / 256.0F * 360.0F).pitch(pitch / 256.0F * 360.0F)
                .onGround(ground).moved(moved).looked(looked)
                .build();
    }

    @Override
    public WPacketPlayOutEntityTeleport processEntityTeleport(Object object) {
        PacketDataSerializer serial = new PacketDataSerializer(Unpooled.buffer());
        PacketPlayOutEntityTeleport packet = (PacketPlayOutEntityTeleport) object;

        try {
            packet.b(serial);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return WPacketPlayOutEntityTeleport.builder()
                .entityId(serial.e())
                .x(serial.readInt() / 32D)
                .y(serial.readInt() / 32D)
                .z(serial.readInt() / 32D)
                .yaw(serial.readByte() / 256.0F * 360.0F)
                .pitch(serial.readByte() / 256.0F * 360.0F)
                .onGround(serial.readBoolean())
                .build();
    }

    @Override
    public WPacketHandshakingInSetProtocol processHandshakingProtocol(Object object) {
        PacketDataSerializer serial = new PacketDataSerializer(Unpooled.buffer());
        PacketHandshakingInSetProtocol packet = (PacketHandshakingInSetProtocol) object;

        try {
            packet.b(serial);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return WPacketHandshakingInSetProtocol.builder()
                .versionNumber(serial.e())
                .hostname(serial.c(32767))
                .port(serial.readUnsignedShort())
                .protocol(WPacketHandshakingInSetProtocol.EnumProtocol.valueOf(EnumProtocol.a(serial.e()).name()))
                .build();
    }
}
