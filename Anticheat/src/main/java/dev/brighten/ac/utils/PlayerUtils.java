package dev.brighten.ac.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.enchantment.Enchantment;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import dev.brighten.ac.api.platform.KauriPlayer;
import lombok.val;
import me.hydro.emulator.util.PotionEffectType;

public class PlayerUtils {

    public static int getDepthStriderLevel(KauriPlayer player) {
        val boots = player.getInventory().getBoots();

        if(boots == null) return 0;

        return boots.getEnchantmentLevel(EnchantmentTypes.DEPTH_STRIDER);
    }

    public static boolean hasBlocksAround(KLocation loc) {
        KLocation one = loc.clone().subtract(1, 0, 1), two = loc.clone().add(1, 1, 1);

        int minX = Math.min(one.getBlockX(), two.getBlockX()), minY = Math.min(one.getBlockY(), two.getBlockY()), minZ = Math.min(one.getBlockZ(), two.getBlockZ());
        int maxX = Math.max(one.getBlockX(), two.getBlockX()), maxY = Math.max(one.getBlockY(), two.getBlockY()), maxZ = Math.max(one.getBlockZ(), two.getBlockZ());

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    KLocation blockLoc = new KLocation(loc.getWorld(), x, y, z);

                    if (BlockUtils.isSolid(BlockUtils.getBlock(blockLoc))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean facingOpposite(Entity one, Entity two) {
        return one.getLocation().getDirection().distance(two.getLocation().getDirection()) < 0.5;
    }

    public static boolean isGliding(Player p) {
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_9)) return false;

        boolean isGliding = false;
        try {
            isGliding = (boolean) p.getClass().getMethod("isGliding", new Class[0]).invoke(p, new Object[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isGliding;
    }

    public static double getAccurateDistance(LivingEntity attacked, LivingEntity entity) {
        KLocation origin = attacked.getEyeLocation(), point;
        if (entity.getLocation().getY() > attacked.getLocation().getBlockY()) {
            point = entity.getLocation();
        } else {
            point = entity.getEyeLocation();
        }


        return origin.distance(point);
    }

    public static double getAccurateDistance(KLocation origin, KLocation point) {
        return origin.distance(point) * Math.cos(origin.getPitch());
    }

    public static int getPotionEffectLevel(KauriPlayer player, PotionType pet) {
        for (KPotionEffect pe : player.getActivePotionEffects()) {
            if (!pe.potionType().equals(pet)) continue;
            return pe.properties().amplifier() + 1;
        }
        return 0;
    }

    public static float getJumpHeight(Player player) {
        float baseHeight = 0.42f;

        if(player.hasPotionEffect(PotionEffectType.JUMP)) {
            baseHeight+= PlayerUtils.getPotionEffectLevel(player, PotionEffectType.JUMP) * 0.1f;
        }

        return baseHeight;
    }

    static {
        try {
            if(PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_8)) {
                DEPTH = Enchantment.getByName("DEPTH_STRIDER");
            }
        } catch(Exception e) {
            DEPTH = null;
        }
    }
}