package mccity.heroes.skills.rockslide;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;

import java.util.*;

public class Rockslide implements Runnable {

    private static final EnumSet<Material> FALL_FREE_MATS = EnumSet.of(Material.AIR, Material.WATER, Material.STATIONARY_WATER,
            Material.LAVA, Material.STATIONARY_LAVA, Material.FIRE);
    private static final EnumSet<Material> ROLLBACK_SAFE_MATS = EnumSet.of(Material.AIR, Material.WATER, Material.STATIONARY_WATER,
            Material.LAVA, Material.STATIONARY_LAVA, Material.FIRE, Material.SNOW, Material.LONG_GRASS, Material.DEAD_BUSH);

    public static final Map<WorldColumn, Rockslide> columnsGlobal = new HashMap<WorldColumn, Rockslide>();

    private final List<Block> initialBlocks = new ArrayList<Block>();
    private Map<Block, MaterialData> replacedBlocks;

    public Rockslide(Location rockslideLoc, int radius, int height) throws IllegalStateException {
        int x = rockslideLoc.getBlockX();
        int y = rockslideLoc.getBlockY();
        int z = rockslideLoc.getBlockZ();
        World world = rockslideLoc.getWorld();

        for (int ix = x - radius; ix <= x + radius; ix++) {
            for (int iz = z - radius; iz <= z + radius; iz++) {
                for (int iy = y; iy < y + height; iy++) {
                    Block curBlock = world.getBlockAt(ix, iy, iz);
                    if (curBlock.getType() == Material.AIR) {
                        addFalling(curBlock);
                    }
                }
            }
        }
    }

    private void addFalling(Block blockPos) throws IllegalStateException {
        initialBlocks.add(blockPos);
        WorldColumn wc = new WorldColumn(blockPos);
        if (columnsGlobal.containsKey(wc)) {
            throw new IllegalStateException();
        }
    }

    public boolean hasNoBlocks() {
        return initialBlocks.isEmpty();
    }

    public void launch() {
        replacedBlocks = new HashMap<Block, MaterialData>();
        for (Block initialBlock : initialBlocks) {
            initialBlock.setType(Material.GRAVEL);
            columnsGlobal.put(new WorldColumn(initialBlock), this);
        }
    }

    public void addReplacingBlock(Block block, MaterialData prevMatData) {
        if (ROLLBACK_SAFE_MATS.contains(prevMatData.getItemType())) {
            replacedBlocks.put(block, prevMatData);
        }
    }

    /* Rollback */
    @Override
    public void run() {
        for (Block initialBlock : initialBlocks) {
            Block curBlock = initialBlock;
            while (true) {
                Material curType = curBlock.getType();
                if (curType == Material.GRAVEL) {
                    curBlock.setType(Material.AIR);
                } else if (FALL_FREE_MATS.contains(curType)) {
                    curBlock = curBlock.getRelative(BlockFace.DOWN);
                    continue;
                }
                break;
            }
            columnsGlobal.remove(new WorldColumn(initialBlock));
        }

        for (Map.Entry<Block, MaterialData> entry : replacedBlocks.entrySet()) {
            Block replacedBlock = entry.getKey();
            MaterialData prevMatData = entry.getValue();

            replacedBlock.setType(prevMatData.getItemType());
            replacedBlock.setData(prevMatData.getData());
        }
    }
}