package dev.brighten.ac.utils;

import dev.brighten.ac.data.APlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;
import dev.brighten.ac.utils.reflections.types.WrappedField;
import dev.brighten.ac.utils.world.BlockData;
import dev.brighten.ac.utils.world.CollisionBox;
import dev.brighten.ac.utils.world.types.SimpleCollisionBox;
import lombok.val;
import me.hydro.emulator.util.mcp.MathHelper;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MovementUtils {

    private static Enchantment DEPTH;

    public static double getJumpHeight(APlayer data) {
        float baseHeight = 0.42f;

        baseHeight+= data.getInfo().groundJumpBoost.map(ef -> ef.getAmplifier() + 1)
                .orElse(0) * 0.1f;

        return baseHeight;
    }

    public static boolean isSameLocation(KLocation one, KLocation two) {
        return one.getX() == two.getX() && one.getY() == two.getY() && one.getZ() == two.getZ();
    }

    public static boolean isSameLocation(Location one, Location two) {
        return one.getX() == two.getX() && one.getY() == two.getY() && one.getZ() == two.getZ();
    }

    public static boolean isOnLadder(APlayer data) {
        try {
            int i = MathHelper.floor_double(data.getMovement().getTo().getLoc().getX());
            int j = MathHelper.floor_double(data.getMovement().getTo().getBox().minY);
            int k = MathHelper.floor_double(data.getMovement().getTo().getLoc().getZ());
            Optional<Block> block = BlockUtils.getBlockAsync(new Location(data.getBukkitPlayer().getWorld(), i, j, k));

            return block.filter(value -> Materials.checkFlag(value.getType(), Materials.LADDER)).isPresent();

        } catch(NullPointerException e) {
            return false;
        }
    }

    private static final WrappedField checkMovement = PacketEvents.getAPI().getServerManager().getVersion().isBelow(ClientVersion.V_1_9)
            ? MinecraftReflection.playerConnection.getFieldByName("checkMovement")
            : MinecraftReflection.playerConnection.getFieldByName(PacketEvents.getAPI().getServerManager().getVersion()
            .isNewerThanOrEquals(ServerVersion.V_1_17) ? "y" : "teleportPos");
    public static boolean checkMovement(Object playerConnection) {
        if(PacketEvents.getAPI().getServerManager().getVersion().isBelow(ClientVersion.V_1_9)) {
            return checkMovement.get(playerConnection);
        } else return (checkMovement.get(playerConnection) == null);
    }

    public static int getDepthStriderLevel(Player player) {
        if(DEPTH == null) return 0;

        val boots = player.getInventory().getBoots();

        if(boots == null) return 0;

        return boots.getEnchantmentLevel(DEPTH);
    }

    public static double getHorizontalDistance(KLocation one, KLocation two) {
        return MathUtils.hypot(one.getX() - two.getX(), one.getZ() - two.getZ());
    }

    public static float getFriction(Block block) {
        XMaterial matched = BlockUtils.getXMaterial(block.getType());

        if(matched == null) return 0.6f;
        return switch (matched) {
            case SLIME_BLOCK -> 0.8f;
            case ICE, BLUE_ICE, FROSTED_ICE, PACKED_ICE -> 0.98f;
            default -> 0.6f;
        };
    }

    public static Location findGroundLocation(APlayer player, int lowestBelow) {
        // Player is on the ground, so no need to calculate
        if(player.getInfo().isServerGround()) {
            return player.getMovement().getTo().getLoc().toLocation(player.getBukkitPlayer().getWorld());
        }
        int x = MathHelper.floor_double(player.getMovement().getTo().getX()),
                startY = MathHelper.floor_double(player.getMovement().getTo().getY()),
                z = MathHelper.floor_double(player.getMovement().getTo().getZ());

        for(int y = startY ; y > startY - lowestBelow ; y--) {
            val block = BlockUtils
                    .getBlockAsync(new Location(player.getBukkitPlayer().getWorld(), x, y, z));

            if(block.isEmpty()) break; //No point in continuing since the one below will still be not present.

            if(Materials.checkFlag(block.get().getType(), Materials.SOLID)
                    && Materials.checkFlag(block.get().getType(), Materials.LIQUID)) {
                CollisionBox box = BlockData.getData(block.get().getType())
                        .getBox(block.get(), PacketEvents.getAPI().getServerManager().getVersion());

                if(box instanceof SimpleCollisionBox sbox) {

                    return new Location(block.get().getWorld(), x, sbox.maxY, z);
                } else {
                    List<SimpleCollisionBox> sboxes = new ArrayList<>();

                    box.downCast(sboxes);

                    double maxY = sboxes.stream().max(Comparator.comparing(sbox -> sbox.maxY)).map(s -> s.maxY)
                            .orElse(y + 1.);

                    return new Location(block.get().getWorld(), x, maxY, z);
                }
            }
        }

        return new Location(player.getBukkitPlayer().getWorld(), player.getMovement().getTo().getX(), startY, player.getMovement().getTo().getZ());
    }

    public static Location findGroundLocation(Location toStart, int lowestBelow) {
        // Player is on the ground, so no need to calculate
        int x = MathHelper.floor_double(toStart.getX()),
                startY = MathHelper.floor_double(toStart.getY()),
                z = MathHelper.floor_double(toStart.getZ());

        for(int y = startY ; y > startY - lowestBelow ; y--) {
            val block = BlockUtils
                    .getBlockAsync(new Location(toStart.getWorld(), x, y, z));

            if(block.isEmpty()) break; //No point in continuing since the one below will still be not present.

            if(Materials.checkFlag(block.get().getType(), Materials.SOLID)
                    || Materials.checkFlag(block.get().getType(), Materials.LIQUID)) {
                CollisionBox box = BlockData.getData(block.get().getType())
                        .getBox(block.get(), PacketEvents.getAPI().getServerManager().getVersion());

                if(box instanceof SimpleCollisionBox sbox) {

                    return new Location(block.get().getWorld(), x, sbox.maxY, z);
                } else {
                    List<SimpleCollisionBox> sboxes = new ArrayList<>();

                    box.downCast(sboxes);

                    double maxY = sboxes.stream().max(Comparator.comparing(sbox -> sbox.maxY)).map(s -> s.maxY)
                            .orElse(y + 0.01);

                    return new Location(block.get().getWorld(), x, maxY, z);
                }
            }
        }

        return new Location(toStart.getWorld(), toStart.getX(), startY, toStart.getZ());
    }

    public static float getTotalHeight(float initial) {
        return getTotalHeight(ClientVersion.V_1_8_9, initial);
    }

    public static float getTotalHeight(ClientVersion version, float initial) {
        float nextCalc = initial, total = initial;
        int count = 0;
        while ((nextCalc = (nextCalc - 0.08f) * 0.98f) > (version.isOrBelow(ClientVersion.V_1_8_9) ?  0.005 : 0)) {
            total+= nextCalc;
            if(count++ > 15) {
                return total * 4;
            }
        }

        return total;
    }

    static {
        try {
            if(PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8)) {
                DEPTH = Enchantment.getByName("DEPTH_STRIDER");
            }

            String test = "%%__USER__%%";
        } catch(Exception e) {
            DEPTH = null;
        }
    }
}
