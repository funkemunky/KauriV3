package dev.brighten.ac.utils.world.blocks;

import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.utils.BlockUtils;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.CollisionFactory;
import dev.brighten.ac.utils.world.types.NoCollisionBox;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Door;

import java.util.Optional;

public class DoorHandler implements CollisionFactory {
    @Override
    public CollisionBox fetch(ClientVersion version, APlayer player, WrappedBlock b) {
        if(PacketEvents.getAPI().getServerManager().getVersion().isBelow(ClientVersion.V_1_13)) {
            Door state = new Door(b.getType(), b.getMaterialData().getData());
            byte data = state.getData();
            if (( data & 0b01000 ) != 0) {
                Optional<WrappedBlock> rel = BlockUtils.getRelative(player, b.getLocation(), BlockFace.DOWN);

                if(rel.isPresent() && BlockUtils.isDoor(rel.get().getType())) {
                    data = rel.get().getMaterialData().getData();
                } else return NoCollisionBox.INSTANCE;
            } else {
                Optional<WrappedBlock> rel = BlockUtils.getRelative(player, b.getLocation(), BlockFace.UP);

                if(rel.isPresent() && BlockUtils.isDoor(rel.get().getType())) {
                    state = new Door(rel.get().getType(), rel.get().getMaterialData().getData());
                } else return NoCollisionBox.INSTANCE;
            }

            SimpleCollisionBox box;
            float offset = 0.1875F;
            int direction = (data & 0b11);
            boolean open = (data & 0b100) != 0;
            boolean hinge = (state.getData() & 1) == 1;


            if (direction == 0) {
                if (open) {
                    if (!hinge) {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, offset);
                    } else {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 1.0F - offset, 1.0F, 1.0F, 1.0F);
                    }
                } else {
                    box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, offset, 1.0F, 1.0F);
                }
            } else if (direction == 1) {
                if (open) {
                    if (!hinge) {
                        box = new SimpleCollisionBox(1.0F - offset, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                    } else {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, offset, 1.0F, 1.0F);
                    }
                } else {
                    box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, offset);
                }
            } else if (direction == 2) {
                if (open) {
                    if (!hinge) {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 1.0F - offset, 1.0F, 1.0F, 1.0F);
                    } else {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, offset);
                    }
                } else {
                    box = new SimpleCollisionBox(1.0F - offset, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                }
            } else {
                if (open) {
                    if (!hinge) {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, offset, 1.0F, 1.0F);
                    } else {
                        box = new SimpleCollisionBox(1.0F - offset, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                    }
                } else {
                    box = new SimpleCollisionBox(0.0F, 0.0F, 1.0F - offset, 1.0F, 1.0F, 1.0F);
                }
            }
//        if (state.isTopHalf())
//            box.offset(0,1,0);
            return box;
        } else {
            WrappedBlock blockTwo;
            Door door = (Door) b.getMaterialData();
            if (door.isTopHalf()) {
                Optional<WrappedBlock> rel = BlockUtils.getRelative(player, b.getLocation(), BlockFace.DOWN);

                if (rel.isPresent() && BlockUtils.isDoor(rel.get().getType())) {
                    blockTwo = rel.get();
                } else {
                    return NoCollisionBox.INSTANCE;
                }
            } else if (PacketEvents.getAPI().getServerManager().getVersion().isBelow(ClientVersion.V_1_13)) {
                Optional<WrappedBlock> rel = BlockUtils.getRelative(player, b.getLocation(), BlockFace.UP);

                if (rel.isPresent() && BlockUtils.isDoor(rel.get().getType())) {
                    blockTwo = rel.get();
                } else {
                    return NoCollisionBox.INSTANCE;
                }
            } else blockTwo = b;

            SimpleCollisionBox box;
            float offset = 0.1875F;
            int direction = door.getFacing().ordinal();
            boolean open = door.isOpen();
            boolean hinge = ((Door) blockTwo.getMaterialData()).getHinge();
            if (direction == 0) {
                if (open) {
                    if (!hinge) {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, offset);
                    } else {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 1.0F - offset, 1.0F, 1.0F, 1.0F);
                    }
                } else {
                    box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, offset, 1.0F, 1.0F);
                }
            } else if (direction == 1) {
                if (open) {
                    if (!hinge) {
                        box = new SimpleCollisionBox(1.0F - offset, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                    } else {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, offset, 1.0F, 1.0F);
                    }
                } else {
                    box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, offset);
                }
            } else if (direction == 2) {
                if (open) {
                    if (!hinge) {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 1.0F - offset, 1.0F, 1.0F, 1.0F);
                    } else {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, offset);
                    }
                } else {
                    box = new SimpleCollisionBox(1.0F - offset, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                }
            } else {
                if (open) {
                    if (!hinge) {
                        box = new SimpleCollisionBox(0.0F, 0.0F, 0.0F, offset, 1.0F, 1.0F);
                    } else {
                        box = new SimpleCollisionBox(1.0F - offset, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                    }
                } else {
                    box = new SimpleCollisionBox(0.0F, 0.0F, 1.0F - offset, 1.0F, 1.0F, 1.0F);
                }
            }
//        if (state.isTopHalf())
//            box.offset(0,1,0);
            return box;
        }
    }
}
