package mccity.heroes.skills.totem;

import me.galaran.bukkitutils.skilltotem.GUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.MaterialData;

import java.util.*;

public class TotemStructure {

    private static Set<MaterialData> baseMatSet;
    private static MaterialData groundMat;
    private static MaterialData sideMat;
    private static final BlockFace[] SIDE_TO_BASE_DIRECTIONS = { BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };

    private final Block base;
    private final MaterialData baseMat;
    private final Block ground;
    private final List<Block> side;

    private final List<Block> blocks;

    public static void init(MaterialData ground, MaterialData side, Set<MaterialData> baseMats) {
        baseMatSet = baseMats;
        groundMat = ground;
        sideMat = side;
    }

    public static TotemStructure detectAt(Block placed, Set<Block> existingTotemBlocks) {
        MaterialData placedMatData = new MaterialData(placed.getTypeId(), placed.getData());
        if (!sideMat.equals(placedMatData)) return null;

        List<TotemStructure> possiblyTotems = new ArrayList<TotemStructure>();
        for (BlockFace curFace : SIDE_TO_BASE_DIRECTIONS) {
            Block possiblyBase = placed.getRelative(curFace);
            MaterialData pBaseMatData = new MaterialData(possiblyBase.getType(), possiblyBase.getData());
            if (baseMatSet.contains(pBaseMatData)) {
                Block upperBlock = possiblyBase.getRelative(BlockFace.UP);
                Block groundBlock = possiblyBase.getRelative(BlockFace.DOWN);
                possiblyTotems.add(new TotemStructure(possiblyBase, pBaseMatData, groundBlock, upperBlock,
                        possiblyBase.getRelative(BlockFace.NORTH), possiblyBase.getRelative(BlockFace.SOUTH)));
                possiblyTotems.add(new TotemStructure(possiblyBase, pBaseMatData, groundBlock, upperBlock,
                        possiblyBase.getRelative(BlockFace.WEST), possiblyBase.getRelative(BlockFace.EAST)));
            }
        }

        // check all possible totem structures
        for (TotemStructure possiblyTotem : possiblyTotems) {
            boolean overlapped = false;
            for (Block curBlock : possiblyTotem.getBlocks()) {
                if (existingTotemBlocks.contains(curBlock)) {
                    overlapped = true;
                    break;
                }
            }
            if (!overlapped && possiblyTotem.check()) return possiblyTotem;
        }
        return null;
    }

    private TotemStructure(Block base, MaterialData baseMatdata, Block ground, Block... sideBlocks) {
        this.base = base;
        this.baseMat = baseMatdata;
        this.ground = ground;
        side = Arrays.asList(sideBlocks);

        List<Block> totemBlocks = new ArrayList<Block>();
        totemBlocks.add(base);
        totemBlocks.add(ground);
        totemBlocks.addAll(side);
        blocks = Collections.unmodifiableList(totemBlocks);
    }

    public Block getBase() {
        return base;
    }

    public MaterialData getBaseMatdata() {
        return new MaterialData(base.getType(), base.getData());
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void destroy(boolean dropBaseBlock) {
        // make sure that chunks are loaded
        for (Block totemBlock : blocks) {
            Chunk chunk = totemBlock.getChunk();
            if (!chunk.isLoaded()) {
                chunk.load();
            }
        }

        if (dropBaseBlock) {
            base.breakNaturally();
        } else {
            base.setType(Material.AIR);
        }
        ground.breakNaturally();
        for (Block sideBlock : side) {
            sideBlock.breakNaturally();
        }
    }

    public boolean check() {
        if (!GUtils.isBlockMatchsMatData(base, baseMat)) return false;
        if (!GUtils.isBlockMatchsMatData(ground, groundMat)) return false;
        for (Block curSide : side) {
            if (!GUtils.isBlockMatchsMatData(curSide, sideMat)) return false;
        }
        return true;
    }
}
