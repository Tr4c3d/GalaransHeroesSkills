package mccity.heroes.skills.totem;

import com.herocraftonline.heroes.characters.Hero;
import org.bukkit.block.Block;

import java.util.*;

public class TotemMap {

    private final Map<String, Totem> playerMap = new HashMap<String, Totem>();
    private final Map<Block, Totem> blockMap = new HashMap<Block, Totem>();

    public Totem getAt(Block block) {
        return blockMap.get(block);
    }

    public Totem getFor(Hero hero) {
        return playerMap.get(hero.getPlayer().getName());
    }

    public Collection<Totem> list() {
        return Collections.unmodifiableCollection(playerMap.values());
    }

    public int size() {
        return playerMap.size();
    }

    public Totem replaceForHero(Totem newTotem) {
        Hero hero = newTotem.getHero();
        Totem old = getFor(hero);
        if (old != null) {
            remove(old);
        }
        playerMap.put(hero.getPlayer().getName(), newTotem);
        for (Block newTotemBlock : newTotem.getStructure().getBlocks()) {
            blockMap.put(newTotemBlock, newTotem);
        }
        return old;
    }

    public Totem remove(Totem totem) {
        Totem removed = playerMap.remove(totem.getHero().getPlayer().getName());
        if (removed != null) {
            for (Block totemBlock : removed.getStructure().getBlocks()) {
                blockMap.remove(totemBlock);
            }
        }
        return removed;
    }

    public Set<Block> getTotemBlocks() {
        return blockMap.keySet();
    }
}
