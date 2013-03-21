package mccity.heroes.skills.totem.types;

import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.party.HeroParty;
import mccity.heroes.skills.totem.Totem;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RecoveryTotem extends Totem {

    private final int healAmount;

    public RecoveryTotem(int healAmount) {
        this.healAmount = healAmount;
    }

    @Override
    public int totemTick() {
        int hpHealed = 0;

        if (checkDistance(getHero())) {
            hpHealed += heal(getHero());
        }
        HeroParty party = getHero().getParty();
        if (party != null) {
            for (Hero partyMember : party.getMembers()) {
                if (checkDistance(partyMember)) {
                    hpHealed += heal(partyMember);
                }
            }
        }

        if (hpHealed > 0) {
            Location effectLoc = getStructure().getBase().getLocation().add(0.5, 2.5, 0.5);
            effectLoc.getWorld().playEffect(effectLoc, Effect.POTION_BREAK, 5);
        }

        return hpHealed;
    }

    public boolean checkDistance(Hero hero) {
        Location totemLoc = getStructure().getBase().getLocation();
        Location heroLoc = hero.getPlayer().getLocation();
        return totemLoc.getWorld().equals(heroLoc.getWorld()) && totemLoc.distance(heroLoc) <= getRadius();
    }

    public int heal(Hero hero) {
        Player player = hero.getPlayer();
        int amount = Math.min(player.getMaxHealth() - player.getHealth(), healAmount);
        if (amount > 0) {
            HeroRegainHealthEvent hrh = new HeroRegainHealthEvent(hero, amount, getSkill());
            Bukkit.getPluginManager().callEvent(hrh);
            if (!hrh.isCancelled()) {
                hero.heal(hrh.getAmount());
            }
        }
        return amount;
    }
}
