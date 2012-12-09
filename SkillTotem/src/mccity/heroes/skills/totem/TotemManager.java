package mccity.heroes.skills.totem;

import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.util.Messaging;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.material.MaterialData;

import java.util.*;

public class TotemManager implements Listener, Runnable {

    private static final String PERM_BYPASS_PROTECTION = "skilltotem.bypassprotection";

    private final SkillTotem skillTotem;
    private final Map<MaterialData, TotemBuilder> registeredTypes = new HashMap<MaterialData, TotemBuilder>();

    private final TotemMap totemMap = new TotemMap();

    public TotemManager(SkillTotem skillTotem, TotemBuilder... builders) {
        this.skillTotem = skillTotem;
        for (TotemBuilder builder : builders) {
            registeredTypes.put(builder.getBaseMaterial(), builder);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        Player player = event.getPlayer();
        Hero hero = skillTotem.plugin.getCharacterManager().getHero(player);
        if (!hero.canUseSkill(skillTotem)) return;

        TotemStructure structure = TotemStructure.detectAt(placed, totemMap.getTotemBlocks());
        if (structure == null) return;
        TotemBuilder builder = registeredTypes.get(structure.getBaseMatdata());

        Long expiry = hero.getCooldown("Totem");
        if (expiry == null || expiry <= System.currentTimeMillis()) {
            int reqMana = builder.getManaCost(hero);
            int mana = hero.getMana();
            if (mana >= reqMana) {
                Totem old = totemMap.replaceForHero(builder.buildFor(hero, structure));
                if (old != null) {
                    old.breakStructure();
                    Message.TOTEM_REPLACED.send(player);
                }
                Message.TOTEM_PLACED_BY.broadcast(player.getLocation(), player.getName());
                int cooldown = builder.getCooldown(hero);
                hero.setCooldown("Totem", System.currentTimeMillis() + cooldown);
                hero.setMana(mana - reqMana);
            } else {
                Messaging.send(hero.getPlayer(), "Not enough mana!");
            }
        } else {
            long remaining = expiry - System.currentTimeMillis();
            Messaging.send(hero.getPlayer(), "Sorry, $1 still has $2 seconds left on cooldown!", "Totem", remaining / 1000);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Totem totem = totemMap.getAt(block);
        if (totem == null) return;

        Player destroyer = event.getPlayer();
        boolean isOwner = destroyer.getName().equals(totem.getHero().getPlayer().getName());

        if (isOwner || !totem.isProtected() || destroyer.hasPermission(PERM_BYPASS_PROTECTION) || destroyer.isOp()) {
            destroyTotemFor(totem.getHero());
        } else {
            Message.TOTEM_PROTECTED.send(destroyer);
            event.setCancelled(true);
        }
    }

    @Override
    public void run() {
        if (totemMap.size() == 0) return;

        List<Totem> forRemove = new ArrayList<Totem>();
        for (Totem curTotem : totemMap.list()) {
            if (!curTotem.onTick()) {
                forRemove.add(curTotem);
            }
        }

        for (Totem removing : forRemove) {
            destroyTotem(removing);
        }
    }

    public List<TotemBuilder> getTypesFor(Hero hero) {
        List<TotemBuilder> types = new ArrayList<TotemBuilder>();
        for (TotemBuilder curType : registeredTypes.values()) {
            if (curType.isAvailable(hero)) {
                types.add(curType);
            }
        }
        return types;
    }

    public Collection<TotemBuilder> getRegisteredTypes() {
        return registeredTypes.values();
    }

    public void destroyTotemFor(Hero hero) {
        Totem forRemove = totemMap.getFor(hero);
        if (forRemove != null) {
            destroyTotem(forRemove);
        }
    }

    private void destroyTotem(Totem totem) {
        totemMap.remove(totem);
        totem.breakStructure();
        Message.YOUR_TOTEM_LOST_MAGIC_POWER.send(totem.getHero().getPlayer());
    }

    public Set<MaterialData> getBaseMats() {
        return registeredTypes.keySet();
    }
}
