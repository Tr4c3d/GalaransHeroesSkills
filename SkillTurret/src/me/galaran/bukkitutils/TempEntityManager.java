package me.galaran.bukkitutils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TempEntityManager {

    private final List<Pair<Entity, Integer>> managedEntities = new ArrayList<Pair<Entity, Integer>>();

    public <T extends Entity> T spawn(Location loc, Class<T> clazz, int lifetimeLimit) throws IllegalArgumentException {
        T spawned = loc.getWorld().spawn(loc, clazz);
        add(spawned, lifetimeLimit);
        return spawned;
    }

    public void add(Entity entity, int lifetimeLimit) {
        managedEntities.add(new Pair<Entity, Integer>(entity, lifetimeLimit));
    }

    public void update() {
        if (managedEntities.isEmpty()) return;

        Iterator<Pair<Entity, Integer>> itr = managedEntities.iterator();
        while (itr.hasNext()) {
            Pair<Entity, Integer> pair = itr.next();
            Entity entity = pair.getLeft();

            if (entity.isDead()) {
                itr.remove();
            } else if (entity.getTicksLived() >= pair.getRight()) {
                entity.remove();
                itr.remove();
            }
        }
    }

    public void despawnAll() {
        for (Pair<Entity, Integer> entity : managedEntities) {
            entity.getLeft().remove();
        }
        managedEntities.clear();
    }
}
