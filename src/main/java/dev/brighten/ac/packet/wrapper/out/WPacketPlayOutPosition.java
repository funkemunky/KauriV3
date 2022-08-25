package dev.brighten.ac.packet.wrapper.out;

import dev.brighten.ac.packet.wrapper.PacketType;
import dev.brighten.ac.packet.wrapper.WPacket;
import lombok.Builder;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

@Builder
@Getter
public class WPacketPlayOutPosition extends WPacket {

    private double x, y, z;
    private float yaw, pitch;
    private int teleportAwait;
    private Set<EnumPlayerTeleportFlags> flags;


    @Override
    public PacketType getPacketType() {
        return PacketType.SERVER_POSITION;
    }

    @Override
    public Object getPacket() {
        return null;
    }

    public enum EnumPlayerTeleportFlags {
        X(0),
        Y(1),
        Z(2),
        Y_ROT(3),
        X_ROT(4);

        private int f;

        private EnumPlayerTeleportFlags(int var3) {
            this.f = var3;
        }

        private int a() {
            return 1 << this.f;
        }

        private boolean b(int var1) {
            return (var1 & this.a()) == this.a();
        }

        public static Set<EnumPlayerTeleportFlags> a(int var0) {
            EnumSet var1 = EnumSet.noneOf(EnumPlayerTeleportFlags.class);
            EnumPlayerTeleportFlags[] var2 = values();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                EnumPlayerTeleportFlags var5 = var2[var4];
                if (var5.b(var0)) {
                    var1.add(var5);
                }
            }

            return var1;
        }

        public static int a(Set<EnumPlayerTeleportFlags> var0) {
            int var1 = 0;

            EnumPlayerTeleportFlags var3;
            for(Iterator var2 = var0.iterator(); var2.hasNext(); var1 |= var3.a()) {
                var3 = (EnumPlayerTeleportFlags)var2.next();
            }

            return var1;
        }


    }

    @Override
    public String toString() {
        return "WPacketPlayOutPosition{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", teleportAwait=" + teleportAwait +
                ", flags=" + flags +
                '}';
    }
}
