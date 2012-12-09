package me.galaran.bukkitutils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

public class BlockLocation implements Cloneable {

    private World world;
    private int x;
    private int y;
    private int z;

    public BlockLocation(World world, int x, int y, int z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockLocation(Location loc) {
        this(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public BlockLocation(Block block) {
        this(block.getWorld(), block.getX(), block.getY(), block.getZ());
    }

    public Block getBlock() {
        return world.getBlockAt(x, y, z);
    }

    public Location getLocation() {
        return new Location(world, x, y, z);
    }

    /**
     * @return this BlockLocation
     */
    public BlockLocation add(BlockVector vec) {
        x += vec.getBlockX();
        y += vec.getBlockY();
        z += vec.getBlockZ();
        return this;
    }

    /**
     * @return this BlockLocation
     */
    public BlockLocation add(int dx, int dy, int dz) {
        x += dx;
        y += dy;
        z += dz;
        return this;
    }

    /**
     * @return this BlockLocation
     */
    public BlockLocation subtract(BlockVector vec) {
        x -= vec.getBlockX();
        y -= vec.getBlockY();
        z -= vec.getBlockZ();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BlockLocation that = (BlockLocation) o;

        if (world != null ? !world.equals(that.world) : that.world != null) return false;
        if (x != that.x) return false;
        if (y != that.y) return false;
        if (z != that.z) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = world != null ? world.hashCode() : 0;
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public BlockLocation clone() {
        try {
            return (BlockLocation) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }
}
