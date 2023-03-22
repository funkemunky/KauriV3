//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package dev.brighten.ac.utils;

import lombok.SneakyThrows;
import me.hydro.emulator.util.mcp.MathHelper;

import javax.annotation.Nullable;
import java.util.Objects;

public class Vec3D implements Cloneable {
    public static final Vec3D a = new Vec3D(0.0D, 0.0D, 0.0D);
    public final double x;
    public final double y;
    public final double z;

    public Vec3D(double var1, double var3, double var5) {
        if (var1 == -0.0D) {
            var1 = 0.0D;
        }

        if (var3 == -0.0D) {
            var3 = 0.0D;
        }

        if (var5 == -0.0D) {
            var5 = 0.0D;
        }

        this.x = var1;
        this.y = var3;
        this.z = var5;
    }

    public Vec3D a(Vec3D var1) {
        return new Vec3D(var1.x - this.x, var1.y - this.y, var1.z - this.z);
    }

    public Vec3D a() {
        double var1 = MathHelper.sqrt_double(this.x * this.x + this.y * this.y + this.z * this.z);
        return var1 < 1.0E-4D ? a : new Vec3D(this.x / var1, this.y / var1, this.z / var1);
    }

    @SneakyThrows
    public Vec3D clone() {
        return (Vec3D) super.clone();
    }

    public double b(Vec3D var1) {
        return this.x * var1.x + this.y * var1.y + this.z * var1.z;
    }

    public Vec3D d(Vec3D var1) {
        return this.a(var1.x, var1.y, var1.z);
    }

    public Vec3D a(double var1, double var3, double var5) {
        return this.add(-var1, -var3, -var5);
    }

    public Vec3D e(Vec3D var1) {
        return this.add(var1.x, var1.y, var1.z);
    }

    public Vec3D add(double var1, double var3, double var5) {
        return new Vec3D(this.x + var1, this.y + var3, this.z + var5);
    }

    public double f(Vec3D var1) {
        double var2 = var1.x - this.x;
        double var4 = var1.y - this.y;
        double var6 = var1.z - this.z;
        return MathHelper.sqrt_double(var2 * var2 + var4 * var4 + var6 * var6);
    }

    public double distanceSquared(Vec3D var1) {
        double var2 = var1.x - this.x;
        double var4 = var1.y - this.y;
        double var6 = var1.z - this.z;
        return var2 * var2 + var4 * var4 + var6 * var6;
    }

    public double c(double var1, double var3, double var5) {
        double var7 = var1 - this.x;
        double var9 = var3 - this.y;
        double var11 = var5 - this.z;
        return var7 * var7 + var9 * var9 + var11 * var11;
    }

    public Vec3D a(double var1) {
        return new Vec3D(this.x * var1, this.y * var1, this.z * var1);
    }

    public double b() {
        return MathHelper.sqrt_double(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    @Nullable
    public Vec3D a(Vec3D var1, double var2) {
        double var4 = var1.x - this.x;
        double var6 = var1.y - this.y;
        double var8 = var1.z - this.z;
        if (var4 * var4 < 1.0000000116860974E-7D) {
            return null;
        } else {
            double var10 = (var2 - this.x) / var4;
            return var10 >= 0.0D && var10 <= 1.0D ? new Vec3D(this.x + var4 * var10, this.y + var6 * var10, this.z + var8 * var10) : null;
        }
    }

    @Nullable
    public Vec3D b(Vec3D var1, double var2) {
        double var4 = var1.x - this.x;
        double var6 = var1.y - this.y;
        double var8 = var1.z - this.z;
        if (var6 * var6 < 1.0000000116860974E-7D) {
            return null;
        } else {
            double var10 = (var2 - this.y) / var6;
            return var10 >= 0.0D && var10 <= 1.0D ? new Vec3D(this.x + var4 * var10, this.y + var6 * var10, this.z + var8 * var10) : null;
        }
    }

    @Nullable
    public Vec3D c(Vec3D var1, double var2) {
        double var4 = var1.x - this.x;
        double var6 = var1.y - this.y;
        double var8 = var1.z - this.z;
        if (var8 * var8 < 1.0000000116860974E-7D) {
            return null;
        } else {
            double var10 = (var2 - this.z) / var8;
            return var10 >= 0.0D && var10 <= 1.0D ? new Vec3D(this.x + var4 * var10, this.y + var6 * var10, this.z + var8 * var10) : null;
        }
    }

    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }

    public Vec3D a(float var1) {
        float var2 = MathHelper.cos(var1);
        float var3 = MathHelper.sin(var1);
        double var6 = this.y * (double)var2 + this.z * (double)var3;
        double var8 = this.z * (double)var2 - this.y * (double)var3;
        return new Vec3D(this.x, var6, var8);
    }

    public Vec3D b(float var1) {
        float var2 = MathHelper.cos(var1);
        float var3 = MathHelper.sin(var1);
        double var4 = this.x * (double)var2 + this.z * (double)var3;
        double var8 = this.z * (double)var2 - this.x * (double)var3;
        return new Vec3D(var4, this.y, var8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec3D vec3D = (Vec3D) o;
        return Double.compare(vec3D.x, x) == 0 && Double.compare(vec3D.y, y) == 0 && Double.compare(vec3D.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
