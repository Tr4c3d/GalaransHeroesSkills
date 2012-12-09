package mccity.heroes.skills.turret;

import org.bukkit.block.Block;

import java.util.*;

public class TurretMap {

    private final Map<Block, Turret> baseMap = new LinkedHashMap<Block, Turret>();
    private final Map<String, List<Turret>> playerMap = new HashMap<String, List<Turret>>();

    public void add(Turret turret) {
        String ownerName = turret.getOwnerName();

        baseMap.put(turret.getBaseBlock(), turret);

        List<Turret> playerTurrets = playerMap.get(ownerName);
        if (playerTurrets == null) {
            playerTurrets = new LinkedList<Turret>();
            playerMap.put(ownerName, playerTurrets);
        }
        playerTurrets.add(turret);
    }

    /**
     * @return immutable turret list for specified player or empty list if player has no turrets
     */
    public List<Turret> getByPlayer(String playerName) {
        List<Turret> playerTurrets = playerMap.get(playerName);
        return playerTurrets == null ? Collections.<Turret>emptyList() : Collections.unmodifiableList(playerTurrets);
    }

    public void removeByPlayer(String playerName) {
        List<Turret> removedTurrets = playerMap.remove(playerName);
        if (removedTurrets != null) {
            for (Turret removedTurret : removedTurrets) {
                baseMap.remove(removedTurret.getBaseBlock());
            }
        }
    }

    public Turret removeByBase(Block baseBlock) {
        Turret removed = baseMap.remove(baseBlock);
        if (removed != null) {
            List<Turret> playerTurrets = playerMap.get(removed.getOwnerName());
            if (playerTurrets != null) {
                for (Iterator<Turret> itr = playerTurrets.iterator(); itr.hasNext();) {
                    if (itr.next() == removed) {
                        itr.remove();
                        break;
                    }
                }
            }
        }
        return removed;
    }

    public Turret getAt(Block baseBlock) {
        return baseMap.get(baseBlock);
    }

    public void setContent(List<Turret> newContent) {
        baseMap.clear();
        playerMap.clear();

        for (Turret curTurret : newContent) {
            add(curTurret);
        }
    }

    /**
     * @return immutable turret Collection
     */
    public Collection<Turret> getContent() {
        return Collections.unmodifiableCollection(baseMap.values());
    }

    public int size() {
        return baseMap.size();
    }
}
