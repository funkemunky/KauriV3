package dev.brighten.ac.utils.math;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.util.Vector3d;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class RayTrace {

    //origin = start position
    //direction = direction in which the raytrace will go
    Vector3d origin, direction;

    public RayTrace(Vector3d origin, Vector3d direction) {
        this.origin = origin;
        this.direction = direction;
    }

    //general intersection detection
    public static boolean intersects(Vector3d position, Vector3d min, Vector3d max) {
        if (position.getX() < min.getX() || position.getX() > max.getX()) {
            return false;
        } else if (position.getY() < min.getY() || position.getY() > max.getY()) {
            return false;
        } else return !(position.getZ() < min.getZ()) && !(position.getZ() > max.getZ());
    }

    //get a point on the raytrace at X blocks away
    public Vector3d getPostion(double blocksAway) {
        return origin.add(direction.multiply(blocksAway));
    }

    //checks if a position is on contained within the position
    public boolean isOnLine(Vector3d position) {
        double t = (position.getX() - origin.getX()) / direction.getX();
        return MathUtils.floor(position.getY()) == origin.getY() + (t * direction.getY()) && MathUtils.floor(position.getZ()) == origin.getZ() + (t * direction.getZ());
    }

    //get all postions on a raytrace
    public List<Vector3d> traverse(double blocksAway, double accuracy) {
        List<Vector3d> positions = new ArrayList<>();
        for (double d = 0; d <= blocksAway; d += accuracy) {
            positions.add(getPostion(d));
        }
        return positions;
    }

    public List<Vector3d> traverse(double skip, double blocksAway, double accuracy) {
        List<Vector3d> positions = new ArrayList<>();
        for (double d = skip; d <= blocksAway; d += accuracy) {
            positions.add(getPostion(d));
        }
        return positions;
    }

    public List<Block> getBlocks(World world, double blocksAway, double accuracy) {
        List<Block> blocks = new ArrayList<>();

        traverse(blocksAway, accuracy).stream()
                .map(KLocation::new)
                .filter(vector -> vector.toLocation(world).getBlock().getType().isSolid())
                .forEach(vector -> blocks.add(vector.toLocation(world).getBlock()));
        return blocks;
    }

    //intersection detection for current raytrace with return
    public Vector3d positionOfIntersection(Vector3d min, Vector3d max, double blocksAway, double accuracy) {
        List<Vector3d> positions = traverse(blocksAway, accuracy);
        for (Vector3d position : positions) {
            if (intersects(position, min, max)) {
                return position;
            }
        }
        return null;
    }

    //intersection detection for current raytrace
    public boolean intersects(Vector3d min, Vector3d max, double blocksAway, double accuracy) {
        List<Vector3d> positions = traverse(blocksAway, accuracy);
        for (Vector3d position : positions) {
            if (intersects(position, min, max)) {
                return true;
            }
        }
        return false;
    }

    //bounding blockbox instead of vector
    public Vector3d positionOfIntersection(SimpleCollisionBox collisionBox, double blocksAway, double accuracy) {
        List<Vector3d> positions = traverse(blocksAway, accuracy);
        for (Vector3d position : positions) {
            if (intersects(position, collisionBox.min(), collisionBox.max())) {
                return position;
            }
        }
        return null;
    }

    public Vector3d positionOfIntersection(SimpleCollisionBox collisionBox, double skip, double blocksAway, double accuracy) {
        List<Vector3d> positions = traverse(skip, blocksAway, accuracy);
        for (Vector3d position : positions) {
            if (intersects(position, collisionBox.min(), collisionBox.max())) {
                return position;
            }
        }
        return null;
    }

    //bounding blockbox instead of vector
    public boolean intersects(SimpleCollisionBox collisionBox, double blocksAway, double accuracy) {
        List<Vector3d> positions = traverse(blocksAway, accuracy);
        for (Vector3d position : positions) {
            if (intersects(position, collisionBox.min(), collisionBox.max())) {
                return true;
            }
        }
        return false;
    }

    public boolean intersects(SimpleCollisionBox collisionBox, double skip, double blocksAway, double accuracy) {
        List<Vector3d> positions = traverse(blocksAway, accuracy);
        for (Vector3d position : positions) {
            if (intersects(position, collisionBox.min(), collisionBox.max())) {
                return true;
            }
        }
        return false;
    }

    //debug / effects
    public void highlight(World world, double blocksAway, double accuracy) {
        for (Vector3d position : traverse(blocksAway, accuracy)) {
            world.playEffect(new Location(world, position.x, position.y, position.z), (PacketEvents.getAPI().getServerManager().getVersion()
                    .isNewerThanOrEquals(ServerVersion.V_1_13) ? Effect.SMOKE : Effect.valueOf("COLOURED_DUST")), 0);
        }
    }

}