package dev.brighten.ac.packet.wrapper.objects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Map;

public enum EnumParticle {
    EXPLOSION_NORMAL("explode", 0, true),
    EXPLOSION_LARGE("largeexplode", 1, true),
    EXPLOSION_HUGE("hugeexplosion", 2, true),
    FIREWORKS_SPARK("fireworksSpark", 3, false),
    WATER_BUBBLE("bubble", 4, false),
    WATER_SPLASH("splash", 5, false),
    WATER_WAKE("wake", 6, false),
    SUSPENDED("suspended", 7, false),
    SUSPENDED_DEPTH("depthsuspend", 8, false),
    CRIT("crit", 9, false),
    CRIT_MAGIC("magicCrit", 10, false),
    SMOKE_NORMAL("smoke", 11, false),
    SMOKE_LARGE("largesmoke", 12, false),
    SPELL("spell", 13, false),
    SPELL_INSTANT("instantSpell", 14, false),
    SPELL_MOB("mobSpell", 15, false),
    SPELL_MOB_AMBIENT("mobSpellAmbient", 16, false),
    SPELL_WITCH("witchMagic", 17, false),
    DRIP_WATER("dripWater", 18, false),
    DRIP_LAVA("dripLava", 19, false),
    VILLAGER_ANGRY("angryVillager", 20, false),
    VILLAGER_HAPPY("happyVillager", 21, false),
    TOWN_AURA("townaura", 22, false),
    NOTE("note", 23, false),
    PORTAL("portal", 24, false),
    ENCHANTMENT_TABLE("enchantmenttable", 25, false),
    FLAME("flame", 26, false),
    LAVA("lava", 27, false),
    FOOTSTEP("footstep", 28, false),
    CLOUD("cloud", 29, false),
    REDSTONE("reddust", 30, false),
    SNOWBALL("snowballpoof", 31, false),
    SNOW_SHOVEL("snowshovel", 32, false),
    SLIME("slime", 33, false),
    HEART("heart", 34, false),
    BARRIER("barrier", 35, false),
    ITEM_CRACK("iconcrack_", 36, false, 2),
    BLOCK_CRACK("blockcrack_", 37, false, 1),
    BLOCK_DUST("blockdust_", 38, false, 1),
    WATER_DROP("droplet", 39, false),
    ITEM_TAKE("take", 40, false),
    MOB_APPEARANCE("mobappearance", 41, true);

    private final String Q;
    private final int R;
    private final boolean S;
    private final int T;
    private static final Map<Integer, EnumParticle> U = Maps.newHashMap();
    private static final String[] V;

    private EnumParticle(String var3, int var4, boolean var5, int var6) {
        this.Q = var3;
        this.R = var4;
        this.S = var5;
        this.T = var6;
    }

    private EnumParticle(String var3, int var4, boolean var5) {
        this(var3, var4, var5, 0);
    }

    public static String[] a() {
        return V;
    }

    public String b() {
        return this.Q;
    }

    public int c() {
        return this.R;
    }

    public int d() {
        return this.T;
    }

    public boolean e() {
        return this.S;
    }

    public boolean f() {
        return this.T > 0;
    }

    public static EnumParticle a(int var0) {
        return (EnumParticle)U.get(var0);
    }

    static {
        ArrayList var0 = Lists.newArrayList();
        EnumParticle[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            EnumParticle var4 = var1[var3];
            U.put(var4.c(), var4);
            if (!var4.b().endsWith("_")) {
                var0.add(var4.b());
            }
        }

        V = (String[])var0.toArray(new String[var0.size()]);
    }
}
