package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.handler.block.WrappedBlock;
import lombok.val;
import me.hydro.emulator.util.mcp.MathHelper;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

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
            WrappedBlock block = data.getBlockUpdateHandler().getBlock(i, j, k);

            return Materials.checkFlag(block.getType(), Materials.LADDER);

        } catch(NullPointerException e) {
            return false;
        }
    }

    public static int getDepthStriderLevel(Player player) {
        if(DEPTH == null) return 0;

        val boots = player.getInventory().getBoots();

        if(boots == null) return 0;

        return boots.getEnchantmentLevel(DEPTH);
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
    public static Location findGroundLocation(Location toStart, int lowestBelow) {
        // Player is on the ground, so no need to calculate
        int x = MathHelper.floor_double(toStart.getX()),
                startY = MathHelper.floor_double(toStart.getY()),
                z = MathHelper.floor_double(toStart.getZ());

        for(int y = startY ; y > startY - lowestBelow ; y--) {
            val block = BlockUtils
                    .getBlockAsync(new Location(toStart.getWorld(), x, y, z));

            if(block.isEmpty()) break; //No point in continuing since the one below will still be not present.

            if(block.get().getType().isSolid()) {
                return new Location(block.get().getWorld(), x, y + 0.001, z);
            }
        }

        return new Location(toStart.getWorld(), toStart.getX(), startY, toStart.getZ());
    }

    public static float getTotalHeight(float initial) {
        return getTotalHeight(ClientVersion.V_1_8, initial);
    }

    public static float getTotalHeight(ClientVersion version, float initial) {
        float nextCalc = initial, total = initial;
        int count = 0;
        while ((nextCalc = (nextCalc - 0.08f) * 0.98f) > (version.isOlderThanOrEquals(ClientVersion.V_1_8) ?  0.005 : 0)) {
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
