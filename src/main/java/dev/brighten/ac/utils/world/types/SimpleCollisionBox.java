package dev.brighten.ac.utils.world.types;

import dev.brighten.ac.packet.wrapper.objects.EnumParticle;
import dev.brighten.ac.utils.BoundingBox;
import dev.brighten.ac.utils.Helper;
import dev.brighten.ac.utils.KLocation;
import dev.brighten.ac.utils.reflections.impl.MinecraftReflection;
import dev.brighten.ac.utils.world.CollisionBox;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SimpleCollisionBox implements CollisionBox {
    public double minX, minY, minZ, maxX, maxY, maxZ;

    public SimpleCollisionBox() {
        this(0, 0, 0, 0, 0, 0);
    }

    public SimpleCollisionBox(double xMin, double yMin, double zMin, double xMax, double yMax, double zMax) {
        if (xMin < xMax) {
            this.minX = xMin;
            this.maxX = xMax;
        } else {
            this.minX = xMax;
            this.maxX = xMin;
        }
        if (yMin < yMax) {
            this.minY = yMin;
            this.maxY = yMax;
        } else {
            this.minY = yMax;
            this.maxY = yMin;
        }
        if (zMin < zMax) {
            this.minZ = zMin;
            this.maxZ = zMax;
        } else {
            this.minZ = zMax;
            this.maxZ = zMin;
        }
    }

    public SimpleCollisionBox(double width, double height) {
        minX = -(width / 2);
        minY = 0;
        minZ = -(width / 2);
        maxX = width / 2;
        maxY = height;
        maxZ = width / 2;
    }

    public SimpleCollisionBox(Vector min, Vector max) {
        this(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    public SimpleCollisionBox(KLocation loc, double width, double height) {
        this(loc.x, loc.y, loc.z, loc.x, loc.y, loc.z);

        expand(width / 2, 0, width / 2);
        maxY += height;
    }

    public SimpleCollisionBox(Location loc, double width, double height) {
        this(loc.toVector(), width, height);
    }

    public SimpleCollisionBox(Vector vec, double width, double height) {
        this(vec.getX(), vec.getY(), vec.getZ(), vec.getX(), vec.getY(), vec.getZ());

        expand(width / 2, 0, width / 2);
        maxY += height;
    }

    public SimpleCollisionBox(BoundingBox box) {
        this(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public SimpleCollisionBox(Object aabb) {
        this(MinecraftReflection.fromAABB(aabb));
    }

    public void sort() {
        double temp = 0;
        if (minX >= maxX) {
            temp = minX;
            this.minX = maxX;
            this.maxX = temp;
        }
        if (minY >= maxY) {
            temp = minY;
            this.minY = maxY;
            this.maxY = temp;
        }
        if (minZ >= maxZ) {
            temp = minZ;
            this.minZ = maxZ;
            this.maxZ = temp;
        }
    }

    public SimpleCollisionBox copy() {
        return new SimpleCollisionBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public SimpleCollisionBox offset(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    @Override
    public SimpleCollisionBox shrink(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX -= x;
        this.maxY -= y;
        this.maxZ -= z;

        return this;
    }

    @Override
    public void downCast(List<SimpleCollisionBox> list) {
        list.add(this);
    }

    @Override
    public boolean isNull() {
        return false;
    }

    public SimpleCollisionBox expandMin(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        return this;
    }

    public SimpleCollisionBox expandMax(double x, double y, double z) {
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    public SimpleCollisionBox expand(double x, double y, double z) {
        this.minX -= x;
        this.minY -= y;
        this.minZ -= z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }
    public SimpleCollisionBox addCoord(double x, double y, double z) {
        if (x < 0.0D) {
            minX += x;
        } else if (x > 0.0D) {
            maxX += x;
        }

        if (y < 0.0D) {
            minY += y;
        } else if (y > 0.0D) {
            maxY += y;
        }

        if (z < 0.0D) {
            minZ += z;
        } else if (z > 0.0D) {
            maxZ += z;
        }

        return this;
    }

    @Override
    public void draw(EnumParticle particle, Player... players) {
        Helper.drawCuboid(copy().expand(0.025), particle, Arrays.asList(players));
    }

    public SimpleCollisionBox expand(double value) {
        this.minX -= value;
        this.minY -= value;
        this.minZ -= value;
        this.maxX += value;
        this.maxY += value;
        this.maxZ += value;
        return this;
    }

    public Vector[] corners() {
        sort();
        Vector[] vectors = new Vector[8];
        vectors[0] = new Vector(minX, minY, minZ);
        vectors[1] = new Vector(minX, minY, maxZ);
        vectors[2] = new Vector(maxX, minY, minZ);
        vectors[3] = new Vector(maxX, minY, maxZ);
        vectors[4] = new Vector(minX, maxY, minZ);
        vectors[5] = new Vector(minX, maxY, maxZ);
        vectors[6] = new Vector(maxX, maxY, minZ);
        vectors[7] = new Vector(maxX, maxY, maxZ);
        return vectors;
    }

    public Vector min() {
        return new Vector(minX, minY, minZ);
    }

    public Vector max() {
        return new Vector(maxX, maxY, maxZ);
    }

    @Override
    public boolean isCollided(CollisionBox other) {
        if (other instanceof SimpleCollisionBox) {
            SimpleCollisionBox box = ((SimpleCollisionBox) other);
            box.sort();
            sort();
            return box.maxX >= this.minX && box.minX <= this.maxX
                    && box.maxY >= this.minY && box.minY <= this.maxY
                    && box.maxZ >= this.minZ && box.minZ <= this.maxZ;
        } else {
            return other.isCollided(this);
            // throw new IllegalStateException("Attempted to check collision with " + other.getClass().getSimpleName());
        }
    }

    @Override
    public boolean isIntersected(CollisionBox other) {
        if(other instanceof SimpleCollisionBox) {
            SimpleCollisionBox box = (SimpleCollisionBox) other;
            box.sort();
            sort();
            return box.maxX > this.minX && box.minX < this.maxX
                    && box.maxY > this.minY && box.minY < this.maxY
                    && box.maxZ > this.minZ && box.minZ < this.maxZ;
        } else {
            return other.isIntersected(this);
        }
    }

    /**
     * if instance and the argument bounding boxes overlap in the Y and Z dimensions, calculate the offset between them
     * in the X dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
     * calculated offset.  Otherwise return the calculated offset.
     */
    public double calculateXOffset(SimpleCollisionBox other, double offsetX) {
        if (other.maxY > this.minY && other.minY < this.maxY && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offsetX > 0.0D && other.maxX <= this.minX) {
                double d1 = this.minX - other.maxX;

                if (d1 < offsetX) {
                    offsetX = d1;
                }
            } else if (offsetX < 0.0D && other.minX >= this.maxX) {
                double d0 = this.maxX - other.minX;

                if (d0 > offsetX) {
                    offsetX = d0;
                }
            }

            return offsetX;
        } else {
            return offsetX;
        }
    }

    /**
     * if instance and the argument bounding boxes overlap in the X and Z dimensions, calculate the offset between them
     * in the Y dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
     * calculated offset.  Otherwise return the calculated offset.
     */
    public double calculateYOffset(SimpleCollisionBox other, double offsetY) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offsetY > 0.0D && other.maxY <= this.minY) {
                double d1 = this.minY - other.maxY;

                if (d1 < offsetY) {
                    offsetY = d1;
                }
            } else if (offsetY < 0.0D && other.minY >= this.maxY) {
                double d0 = this.maxY - other.minY;

                if (d0 > offsetY) {
                    offsetY = d0;
                }
            }

            return offsetY;
        } else {
            return offsetY;
        }
    }

    /**
     * if instance and the argument bounding boxes overlap in the Y and X dimensions, calculate the offset between them
     * in the Z dimension.  return var2 if the bounding boxes do not overlap or if var2 is closer to 0 then the
     * calculated offset.  Otherwise return the calculated offset.
     */
    public double calculateZOffset(SimpleCollisionBox other, double offsetZ) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxY > this.minY && other.minY < this.maxY) {
            if (offsetZ > 0.0D && other.maxZ <= this.minZ) {
                double d1 = this.minZ - other.maxZ;

                if (d1 < offsetZ) {
                    offsetZ = d1;
                }
            } else if (offsetZ < 0.0D && other.minZ >= this.maxZ) {
                double d0 = this.maxZ - other.minZ;

                if (d0 > offsetZ) {
                    offsetZ = d0;
                }
            }

            return offsetZ;
        } else {
            return offsetZ;
        }
    }

    public BoundingBox toBoundingBox() {
        return new BoundingBox(new Vector(minX, minY, minZ), new Vector(maxX, maxY, maxZ));
    }

    public <T> T toAxisAlignedBB() {
        return MinecraftReflection.toAABB(this);
    }

    public double distance(SimpleCollisionBox box) {
        double xwidth = (maxX - minX) / 2, zwidth = (maxZ - minZ) / 2;
        double bxwidth = (box.maxX - box.minX) / 2, bzwidth = (box.maxZ - box.minZ) / 2;
        double hxz = Math.hypot(minX - box.minX, minZ - box.minZ);

        return hxz - (xwidth + zwidth + bxwidth + bzwidth) / 4;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleCollisionBox that = (SimpleCollisionBox) o;
        return Double.compare(that.minX, minX) == 0 && Double.compare(that.minY, minY) == 0 && Double.compare(that.minZ, minZ) == 0 && Double.compare(that.maxX, maxX) == 0 && Double.compare(that.maxY, maxY) == 0 && Double.compare(that.maxZ, maxZ) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public String toString() {
        return "SimpleCollisionBox{" +
                "xMin=" + minX +
                ", yMin=" + minY +
                ", zMin=" + minZ +
                ", xMax=" + maxX +
                ", yMax=" + maxY +
                ", zMax=" + maxZ +
                '}';
    }
}