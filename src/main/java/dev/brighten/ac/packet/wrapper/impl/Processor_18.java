package dev.brighten.ac.packet.wrapper.impl;

import dev.brighten.ac.packet.wrapper.PacketConverter;
import dev.brighten.ac.packet.wrapper.in.*;
import dev.brighten.ac.packet.wrapper.login.WPacketHandshakingInSetProtocol;
import dev.brighten.ac.packet.wrapper.objects.EnumParticle;
import dev.brighten.ac.packet.wrapper.objects.PlayerCapabilities;
import dev.brighten.ac.packet.wrapper.objects.WrappedEnumDirection;
import dev.brighten.ac.packet.wrapper.objects.WrappedWatchableObject;
import dev.brighten.ac.packet.wrapper.out.*;
import dev.brighten.ac.utils.MathHelper;
import dev.brighten.ac.utils.MiscUtils;
import dev.brighten.ac.utils.math.IntVector;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import io.netty.buffer.Unpooled;
import lombok.SneakyThrows;
import lombok.val;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Processor_18 implements PacketConverter {
    @Override
    public WPacketPlayInFlying processFlying(Object object) {
        PacketPlayInFlying flying = (PacketPlayInFlying) object;

        return WPacketPlayInFlying.builder().x(flying.a()).y(flying.b()).z(flying.c()).yaw(flying.d())
                .pitch(flying.e()).onGround(flying.f()).moved(flying.g()).looked(flying.h()).build();
    }

    private static final WrappedClass classUseEntity = new WrappedClass(PacketPlayInUseEntity.class);
    private static final WrappedField fieldEntityId = classUseEntity.getFieldByName("a");
    @Override
    public WPacketPlayInUseEntity processUseEntity(Object object) {
        PacketPlayInUseEntity useEntity = (PacketPlayInUseEntity) object;

        return WPacketPlayInUseEntity.builder().entityId(fieldEntityId.get(useEntity))
                .action(WPacketPlayInUseEntity.EnumEntityUseAction.valueOf(useEntity.a().name()))
                .vector(useEntity.b() != null ? new Vector(useEntity.b().a, useEntity.b().b, useEntity.b().c) : null)
                .build();
    }

    private static final WrappedClass classAbilities = new WrappedClass(PacketPlayInAbilities.class),
            outClassAbilities = new WrappedClass(PacketPlayOutAbilities.class);
    private static final WrappedField fieldFlySpeed = classAbilities.getFieldByType(float.class, 0),
            fieldWalkSpeed = classAbilities.getFieldByType(float.class, 1);

    private static final WrappedField outfieldFlySpeed = outClassAbilities.getFieldByType(float.class, 0),
            outfieldWalkSpeed = outClassAbilities.getFieldByType(float.class, 1);

    @Override
    public WPacketPlayInAbilities processAbilities(Object object) {
        PacketPlayInAbilities abilities = (PacketPlayInAbilities) object;

        return WPacketPlayInAbilities.builder()
                .capabilities(PlayerCapabilities.builder()
                        .isInvulnerable(abilities.a())
                        .isFlying(abilities.isFlying())
                        .canFly(abilities.c())
                        .canInstantlyBuild(abilities.d())
                        .flySpeed(fieldFlySpeed.get(abilities))
                        .walkSpeed(fieldWalkSpeed.get(abilities))
                        .build())
                .build();
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

        return WPacketPlayInBlockDig.builder()
                .blockPos(new IntVector(pos.getX(), pos.getY(), pos.getZ()))
                .direction(WrappedEnumDirection.valueOf(packet.b().name()))
                .digType(WPacketPlayInBlockDig.EnumPlayerDigType.valueOf(packet.c().name()))
                .build();
    }

    @Override
    public WPacketPlayInBlockPlace processBlockPlace(Object object) {
        PacketPlayInBlockPlace packet = (PacketPlayInBlockPlace) object;

        BlockPosition pos = packet.a();

        return WPacketPlayInBlockPlace.builder()
                .blockPos(new IntVector(pos.getX(), pos.getY(), pos.getZ()))
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
                .amplifier(serializer.readByte())
                .duration(serializer.e())
                .flags(serializer.readByte()).build();
    }

    @Override
    public Object processEntityEffect(WPacketPlayOutEntityEffect packet) {
        PacketPlayOutEntityEffect vanilla = new PacketPlayOutEntityEffect();
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer());

        serializer.b(packet.getEntityId());
        serializer.writeByte(packet.getEffectId());
        serializer.writeByte(packet.getAmplifier());
        serializer.b(packet.getDuration());
        serializer.writeByte(packet.getFlags());

        try {
            vanilla.a(serializer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return vanilla;
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
                .id(id)
                .x(x / 32D).y(y / 32D).z(z / 32D).yaw(yaw / 256.0F * 360.0F).pitch(pitch / 256.0F * 360.0F)
                .onGround(ground).moved(moved).looked(looked)
                .build();
    }

    @Override
    public Object processOutEntity(WPacketPlayOutEntity packet) {
        PacketDataSerializer serial = new PacketDataSerializer(Unpooled.buffer());

        serial.b(packet.getId()); //No matter what this will always be written

        Object packetToReturn = null;

        if(packet.isLooked() && packet.isMoved()) { // Moved and looked
            serial.writeByte(MathHelper.floor_double(packet.getX() * 32.));
            serial.writeByte(MathHelper.floor_double(packet.getY() * 32.));
            serial.writeByte(MathHelper.floor_double(packet.getZ() * 32.));
            serial.writeByte((byte)((int)(packet.getYaw() * 256.F / 360.F)));
            serial.writeByte((byte)((int)(packet.getPitch() * 256.F / 360.F)));
            serial.writeBoolean(packet.isOnGround());

            PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook vanilla = new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook();

            try {
                vanilla.a(serial);
                packetToReturn = vanilla;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if(packet.isLooked()) { // Only looked
            serial.writeByte((byte)((int)(packet.getYaw() * 256.F / 360.F)));
            serial.writeByte((byte)((int)(packet.getPitch() * 256.F / 360.F)));
            serial.writeBoolean(packet.isOnGround());

            PacketPlayOutEntity.PacketPlayOutEntityLook vanilla = new PacketPlayOutEntity.PacketPlayOutEntityLook();

            try {
                vanilla.a(serial);
                packetToReturn = vanilla;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if(packet.isMoved()) { // Only moved
            serial.writeByte(MathHelper.floor_double(packet.getX() * 32.));
            serial.writeByte(MathHelper.floor_double(packet.getY() * 32.));
            serial.writeByte(MathHelper.floor_double(packet.getZ() * 32.));
            serial.writeBoolean(packet.isOnGround());

            PacketPlayOutEntity.PacketPlayOutRelEntityMove vanilla = new PacketPlayOutEntity.PacketPlayOutRelEntityMove();

            try {
                vanilla.a(serial);
                packetToReturn = vanilla;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            PacketPlayOutEntity vanilla = new PacketPlayOutEntity();

            try {
                vanilla.a(serial);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            packetToReturn = vanilla;
        }
        return packetToReturn;
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
    public Object processEntityTeleport(WPacketPlayOutEntityTeleport packet) {
        PacketDataSerializer serial = new PacketDataSerializer(Unpooled.buffer());
        PacketPlayOutEntityTeleport vanilla = new PacketPlayOutEntityTeleport();

        serial.b(packet.getEntityId());
        serial.writeInt(MathHelper.floor_double(packet.getX() * 32.));
        serial.writeInt(MathHelper.floor_double(packet.getY() * 32.));
        serial.writeInt(MathHelper.floor_double(packet.getZ() * 32.));
        serial.writeByte((byte)((int)(packet.getYaw() * 256.F / 360.F)));
        serial.writeByte((byte)((int)(packet.getPitch() * 256.F / 360.F)));
        serial.writeBoolean(packet.isOnGround());

        try {
            vanilla.a(serial);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return vanilla;
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
                .protocol(WPacketHandshakingInSetProtocol.EnumProtocol.valueOf(EnumProtocol.a(serial.e()).toString()))
                .build();
    }

    @Override
    public WPacketPlayOutBlockChange processBlockChange(Object object) {
        PacketPlayOutBlockChange packet = (PacketPlayOutBlockChange) object;
        PacketDataSerializer serial = serialize(packet);

        BlockPosition blockPos = serial.c();
        IntVector vecPos = new IntVector(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        Material material = CraftMagicNumbers.getMaterial(packet.block.getBlock());

        return WPacketPlayOutBlockChange.builder()
                .blockLocation(vecPos)
                .material(material)
                .blockData((byte)packet.block.getBlock().toLegacyData(packet.block))
                .build();
    }

    @Override
    public WPacketPlayOutMultiBlockChange processMultiBlockChange(Object object) {
        PacketPlayOutMultiBlockChange packet = (PacketPlayOutMultiBlockChange) object;
        PacketDataSerializer serial = serialize(packet);

        final int[] chunkLoc = new int[] {serial.readInt(), serial.readInt()};
        final WPacketPlayOutMultiBlockChange.BlockChange[]
                blockChanges = new WPacketPlayOutMultiBlockChange.BlockChange[serial.e()];

        for (int i = 0; i < blockChanges.length; i++) {
            short encodedloc = serial.readShort();

            IntVector loc = new IntVector(encodedloc >> 12 & 15, encodedloc & 255, encodedloc >> 8 & 15);
            IBlockData blockData = Block.d.a(serial.e());
            Material blockType = CraftMagicNumbers.getMaterial(blockData.getBlock());

            blockChanges[i] = new WPacketPlayOutMultiBlockChange.BlockChange(loc, blockType,
                    (byte)blockData.getBlock().toLegacyData(blockData));
        }

        return WPacketPlayOutMultiBlockChange.builder()
                .chunk(chunkLoc)
                .changes(blockChanges)
                .build();
    }

    @Override
    public WPacketPlayOutEntityVelocity processVelocity(Object object) {
        PacketPlayOutEntityVelocity packet = (PacketPlayOutEntityVelocity) object;
        PacketDataSerializer serial = serialize(packet);

        return WPacketPlayOutEntityVelocity.builder()
                .entityId(serial.e())
                .deltaX(serial.readShort() / 8000D)
                .deltaY(serial.readShort() / 8000D)
                .deltaZ(serial.readShort() / 8000D)
                .build();
    }

    @Override
    public WPacketPlayOutAbilities processOutAbilities(Object object) {
        PacketPlayOutAbilities packet = (PacketPlayOutAbilities) object;

        return WPacketPlayOutAbilities.builder()
                .capabilities(PlayerCapabilities.builder()
                        .isInvulnerable(packet.a())
                        .isFlying(packet.b())
                        .canFly(packet.c())
                        .canInstantlyBuild(packet.d())
                        .flySpeed(outfieldFlySpeed.get(packet))
                        .walkSpeed(outfieldWalkSpeed.get(packet))
                        .build())
                .build();
    }

    @Override
    public Object processOutAbilities(WPacketPlayOutAbilities packet) {
        PlayerAbilities abilities = new PlayerAbilities();

        abilities.canFly = packet.getCapabilities().canFly;
        abilities.isFlying = packet.getCapabilities().isFlying;
        abilities.isInvulnerable = packet.getCapabilities().isInvulnerable;
        abilities.canInstantlyBuild = packet.getCapabilities().canInstantlyBuild;
        abilities.flySpeed = packet.getCapabilities().flySpeed;
        abilities.walkSpeed = packet.getCapabilities().walkSpeed;

        return new PacketPlayOutAbilities(abilities);
    }

    @Override
    public Object processVelocity(WPacketPlayOutEntityVelocity packet) {
        return new PacketPlayOutEntityVelocity(packet.getEntityId(), packet.getDeltaX(), packet.getDeltaY(), packet.getDeltaZ());
    }

    @Override
    public WPacketPlayOutWorldParticles processParticles(Object object) {
        PacketPlayOutWorldParticles packet = (PacketPlayOutWorldParticles) object;
        PacketDataSerializer serial = serialize(packet);

        EnumParticle particle = EnumParticle.a(serial.readInt());

        if(particle == null) particle = EnumParticle.BARRIER;

        int[] data = new int[particle.d()];

        val builder = WPacketPlayOutWorldParticles.builder()
                .particle(particle)
                .longD(serial.readBoolean())
                .x(serial.readFloat())
                .y(serial.readFloat())
                .z(serial.readFloat())
                .offsetX(serial.readFloat())
                .offsetY(serial.readFloat())
                .offsetZ(serial.readFloat())
                .speed(serial.readFloat())
                .amount(serial.readInt());

        for (int i = 0; i < data.length; i++) {
            data[i] = serial.e();
        }

        return builder.data(data).build();
    }

    @Override
    public Object processParticles(WPacketPlayOutWorldParticles packet) {
        return new PacketPlayOutWorldParticles(net.minecraft.server.v1_8_R3.EnumParticle.valueOf(packet.getParticle().name()),
                packet.isLongD(), packet.getX(), packet.getY(), packet.getZ(), packet.getOffsetX(), packet.getOffsetY(), packet.getOffsetZ(),
                packet.getSpeed(), packet.getAmount(), packet.getData());
    }

    @Override
    public WPacketPlayInChat processChat(Object object) {
        PacketPlayInChat packet = (PacketPlayInChat) object;

        return WPacketPlayInChat.builder()
                .message(packet.a())
                .build();
    }

    @Override
    public Object processChat(WPacketPlayInChat packet) {
        return new PacketPlayInChat(packet.getMessage());
    }

    @Override
    public WPacketPlayOutPlayerInfo processInfo(Object object) {

        return null;
    }

    private static final WrappedClass wrappedNamedEntitySpawn = new WrappedClass(PacketPlayOutNamedEntitySpawn.class);
    private static final WrappedField dataWatcherField = wrappedNamedEntitySpawn.getFieldByType(DataWatcher.class, 0);
    @Override
    public WPacketPlayOutNamedEntitySpawn processNamedEntitySpawn(Object object) {
        PacketPlayOutNamedEntitySpawn packet = (PacketPlayOutNamedEntitySpawn) object;
        PacketDataSerializer serial = serialize(packet);

        return WPacketPlayOutNamedEntitySpawn.builder().entityId(serial.e()).uuid(serial.g())
                .x(serial.readInt() / 32.).y(serial.readInt() / 32.).z(serial.readInt() / 32.)
                .yaw(serial.readByte() * 360.F / 256.F).pitch(serial.readByte() * 360.F / 256.F)
                .itemInHand(MiscUtils.getById(serial.readShort()))
                .build();
    }

    @Override
    public Object processNamedEntitySpawn(WPacketPlayOutNamedEntitySpawn packet) {
        PacketPlayOutNamedEntitySpawn vanilla = new PacketPlayOutNamedEntitySpawn();
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer());

        try {
            serializer.b(packet.getEntityId());
            serializer.a(packet.getUuid());
            serializer.writeInt(MathHelper.floor_double(packet.getX() * 32.));
            serializer.writeInt(MathHelper.floor_double(packet.getY() * 32.));
            serializer.writeInt(MathHelper.floor_double(packet.getZ() * 32.));
            serializer.writeByte((byte)((int)(packet.getYaw() * 256.0F / 360.0F)));
            serializer.writeByte((byte)((int)(packet.getPitch() * 256.0F / 360.0F)));
            serializer.writeShort(packet.getItemInHand() == null ? 0 : packet.getItemInHand().getId());
            dataWatcherField.set(vanilla, new DataWatcher(null));
            new DataWatcher(null).a(serializer);

            vanilla.a(serializer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return vanilla;
    }

    private static final WrappedClass classSpawnEntityLiving = new WrappedClass(PacketPlayOutSpawnEntityLiving.class);
    private static final WrappedField splDataWatcher = classSpawnEntityLiving.getFieldByType(DataWatcher.class, 0),
            splWatchList = classSpawnEntityLiving.getFieldByType(List.class, 0);

    @SneakyThrows
    @Override
    public WPacketPlayOutSpawnEntityLiving processSpawnLiving(Object object) {
        PacketPlayOutSpawnEntityLiving packet = (PacketPlayOutSpawnEntityLiving) object;
        PacketDataSerializer s = serialize(packet);

        val builder = WPacketPlayOutSpawnEntityLiving.builder().entityId(s.e())
                .type(EntityType.fromId(s.readByte() & 255)).x(s.readInt() / 32.).y(s.readInt() / 32.)
                .z(s.readInt()/ 32.).yaw(s.readByte() * 360.F / 256.F).pitch(s.readByte() * 360.F / 256.F)
                .headYaw(s.readByte() * 360.F / 256.F).motionX(s.readShort() / 8000.).motionY(s.readShort() / 8000.)
                .motionZ(s.readShort()  / 8000.);

        val watchedObjects = DataWatcher.b(s);

        if(watchedObjects != null) {
            builder.watchedObjects(watchedObjects.stream().map(WrappedWatchableObject::new).collect(Collectors.toList()));
        } else builder.watchedObjects(new ArrayList<>());

        return builder.build();
    }

    private static final WrappedClass dataWatcherClass = new WrappedClass(DataWatcher.class);
    private static final WrappedField watchableMap = dataWatcherClass.getFieldByName("d");
    @Override
    public Object processSpawnLiving(WPacketPlayOutSpawnEntityLiving packet) {
        PacketPlayOutSpawnEntityLiving vanilla = new PacketPlayOutSpawnEntityLiving();
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer());

        serializer.b(packet.getEntityId());
        serializer.writeByte(packet.getType().getTypeId() & 255);
        serializer.writeInt(MathHelper.floor_double(packet.getX() * 32.));
        serializer.writeInt(MathHelper.floor_double(packet.getY() * 32.));
        serializer.writeInt(MathHelper.floor_double(packet.getZ() * 32.));
        serializer.writeByte((byte)((int)(packet.getYaw() * 256.0F / 360.0F)));
        serializer.writeByte((byte)((int)(packet.getPitch() * 256.0F / 360.0F)));
        serializer.writeByte((byte)((int)(packet.getHeadYaw() * 256.0F / 360.0F)));
        serializer.writeShort((int)(packet.getMotionX() * 8000.));
        serializer.writeShort((int)(packet.getMotionY() * 8000.));
        serializer.writeShort((int)(packet.getMotionZ() * 8000.));

        try {
            DataWatcher watcher = new DataWatcher(null);

            for (WrappedWatchableObject w : packet.getWatchedObjects()) {
                watcher.a(w.getDataValueId(), w.getWatchedObject());;
            }

            watcher.a(serializer);
            splDataWatcher.set(vanilla, watcher);
            vanilla.a(serializer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return vanilla;
    }

    @Override
    public WPacketPlayOutRemoveEntityEffect processRemoveEffect(Object object) {
        PacketPlayOutRemoveEntityEffect packet = (PacketPlayOutRemoveEntityEffect) object;
        
        PacketDataSerializer serializer = serialize(packet);

        return WPacketPlayOutRemoveEntityEffect.builder()
                .entityId(serializer.e())
                .effect(PotionEffectType.getById(serializer.readUnsignedByte())).build();
    }

    @Override
    public Object processRemoveEffect(WPacketPlayOutRemoveEntityEffect packet) {
        PacketPlayOutRemoveEntityEffect vanilla = new PacketPlayOutRemoveEntityEffect();

        PacketDataSerializer serial = new PacketDataSerializer(Unpooled.buffer());

        serial.b(packet.getEntityId());
        serial.writeByte(packet.getEffect().getId());

        try {
            vanilla.b(serial);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return vanilla;
    }

    @Override
    public WPacketPlayOutEntityMetadata processEntityMetadata(Object object) {
        PacketPlayOutEntityMetadata packet = (PacketPlayOutEntityMetadata) object;

        PacketDataSerializer serialized = serialize(packet);

        try {
            int entityId = serialized.e();
            List<DataWatcher.WatchableObject> watchedObject = DataWatcher.b(serialized);

            return WPacketPlayOutEntityMetadata.builder().entityId(entityId)
                    .watchedObjects(watchedObject.stream().map(WrappedWatchableObject::new)
                    .collect(Collectors.toList())).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object processEntityMetadata(WPacketPlayOutEntityMetadata packet) {
        PacketPlayOutEntityMetadata vanilla = new PacketPlayOutEntityMetadata();

        PacketDataSerializer serialized = new PacketDataSerializer(Unpooled.buffer());

        serialized.b(packet.getEntityId());

        List<DataWatcher.WatchableObject> watchedObjects = packet.getWatchedObjects().stream()
                .map(w -> (DataWatcher.WatchableObject)w.toVanillaObject()).collect(Collectors.toList());

        try {
            DataWatcher.a(watchedObjects, serialized);
            vanilla.a(serialized);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return vanilla;
    }

    @Override
    public WPacketPlayOutEntityDestroy processEntityDestroy(Object object) {
        PacketPlayOutEntityDestroy packet = (PacketPlayOutEntityDestroy) object;

        PacketDataSerializer serialized = serialize(packet);

        int[] entityIds = new int[serialized.e()];

        for(int var2 = 0; var2 < entityIds.length; ++var2) {
            entityIds[var2] = serialized.e();
        }

        return WPacketPlayOutEntityDestroy.builder().entityIds(entityIds).build();
    }

    private PacketDataSerializer serialize(Packet<?> packet) {
        PacketDataSerializer serial = new PacketDataSerializer(Unpooled.buffer());
        try {
            packet.b(serial);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return serial;
    }

    private static void writePacketData(PacketDataSerializer serializer, List<WrappedWatchableObject> objects) {
        for (WrappedWatchableObject object : objects) {
            int i = (object.getObjectType() << 5 | object.getDataValueId() & 31) & 255;
            serializer.writeByte(i);
            switch (object.getObjectType()) {
                case 0: {
                    serializer.writeByte((Byte)object.getWatchedObject());
                    break;
                }
                case 1: {
                    serializer.writeShort((Short)object.getWatchedObject());
                    break;
                }
                case 2: {
                    serializer.writeInt((Integer)object.getWatchedObject());
                    break;
                }
                case 3: {
                    serializer.writeFloat((Float)object.getWatchedObject());
                    break;
                }
                case 4: {
                    serializer.a((String)object.getWatchedObject());
                    break;
                }
                case 5: {
                    ItemStack itemStack = (ItemStack)object.getWatchedObject();
                    serializer.a(itemStack);
                    break;
                }
                case 6: {
                    BlockPosition blockPosition = (BlockPosition)object.getWatchedObject();
                    serializer.writeInt(blockPosition.getX());
                    serializer.writeInt(blockPosition.getY());
                    serializer.writeInt(blockPosition.getZ());
                    break;
                }
                case 7: {
                    Vector3f vector3f = (Vector3f)object.getWatchedObject();
                    serializer.writeFloat(vector3f.getX());
                    serializer.writeFloat(vector3f.getY());
                    serializer.writeFloat(vector3f.getZ());
                    break;
                }
            }
        }
        serializer.writeByte(127);
    }
}
