package dev.brighten.ac.utils.world.blocks;

import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.CollisionFactory;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;

public class TrapDoorHandler implements CollisionFactory {
    @Override
    public CollisionBox fetch(ProtocolVersion version, APlayer player, WrappedBlock block) {
        byte data = block.getData();
        double var2 = 0.1875;

        if ((data & 4) != 0) {
            if ((data & 3) == 0) {
                return new SimpleCollisionBox(0.0, 0.0, 1.0 - var2, 1.0, 1.0, 1.0);
            }

            if ((data & 3) == 1) {
                return new SimpleCollisionBox(0.0, 0.0, 0.0, 1.0, 1.0, var2);
            }

            if ((data & 3) == 2) {
                return new SimpleCollisionBox(1.0 - var2, 0.0, 0.0, 1.0, 1.0, 1.0);
            }

            if ((data & 3) == 3) {
                return new SimpleCollisionBox(0.0, 0.0, 0.0, var2, 1.0, 1.0);
            }
        } else {
            if ((data & 8) != 0) {
                return new SimpleCollisionBox(0.0, 1.0 - var2, 0.0, 1.0, 1.0, 1.0);
            } else {
                return new SimpleCollisionBox(0.0, 0.0, 0.0, 1.0, var2, 1.0);
            }
        }
        return null;
    }
}
