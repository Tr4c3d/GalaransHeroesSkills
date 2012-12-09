package mccity.heroes.skills.rockslide;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class WorldColumn {

    private final int x;
    private final int z;
    private final World world;

    public WorldColumn(int x, int z, World world) {
        this.x = x;
        this.z = z;
        this.world = world;
    }

    public WorldColumn(Block block) {
        this(block.getX(), block.getZ(), block.getWorld());
    }

    public WorldColumn(Location loc) {
        this(loc.getBlockX(), loc.getBlockZ(), loc.getWorld());
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public World getWorld() {
        return world;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorldColumn that = (WorldColumn) o;

        if (x != that.x) return false;
        if (z != that.z) return false;
        if (world != null ? !world.equals(that.world) : that.world != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + z;
        result = 31 * result + (world != null ? world.hashCode() : 0);
        return result;
    }
}
