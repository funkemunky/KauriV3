package dev.brighten.ac.packet.wrapper.impl;

import dev.brighten.ac.packet.wrapper.PacketConverter;
import dev.brighten.ac.packet.wrapper.in.*;
import dev.brighten.ac.packet.wrapper.objects.WrappedEnumDirection;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.util.Vector;

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
                .vector(new Vector(useEntity.b().a, useEntity.b().b, useEntity.b().c))
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
                .direction(WrappedEnumDirection.values()[packet.getFace()])
                .itemStack(CraftItemStack.asCraftMirror(packet.getItemStack()))
                .vecX(packet.d()).vecY(packet.e()).vecZ(packet.f())
                .build();
    }


    private static final WrappedClass classCloseWindow = new WrappedClass(PacketPlayInCloseWindow.class);
    private static final WrappedField fieldWindowId = classAbilities.getFieldByType(int.class, 0);
    @Override
    public WPacketPlayInCloseWindow processCloseWindow(Object object) {
        PacketPlayInCloseWindow packet = (PacketPlayInCloseWindow) object;

        return WPacketPlayInCloseWindow.builder().id(fieldWindowId.get(packet)).build();
    }
}
