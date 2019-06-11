package idk.plugin.fastrespawn;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

public class Main extends PluginBase implements Listener {

    private Config c;

    public void onEnable() {
        saveDefaultConfig();
        c = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void handleDeath(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();

            if (!p.hasPermission("fastrespawn")) {
                return;
            }

            if (e.getFinalDamage() >= p.getHealth()) {
                if (c.getBoolean("resetHealth")) {
                    p.setHealth(p.getMaxHealth());
                }

                if (c.getBoolean("resetFood")) {
                    p.getFoodData().setLevel(p.getFoodData().getMaxLevel());
                }

                if (c.getBoolean("dropInventory")) {
                    for (Item item : p.getDrops()) {
                        p.getLevel().dropItem(p, item, null, true, 40);
                    }

                    p.getLevel().dropItem(p, p.getCursorInventory().getItem(0), null, true, 40);

                    p.getInventory().clearAll();
                    p.getCursorInventory().clearAll();
                } else if (c.getBoolean("resetInventory")) {
                    p.getInventory().clearAll();
                    p.getCursorInventory().clearAll();
                }

                if (c.getBoolean("dropXp")) {
                    if (p.isSurvival() || p.isAdventure()) {
                        int exp = p.getExperience() * 7;
                        if (exp > 100) exp = 100;
                        int add = 1;

                        for (int ii = 1; ii < exp; ii += add) {
                            p.getLevel().dropExpOrb(p, add);
                            add = new NukkitRandom().nextRange(1, 3);
                        }
                    }

                    p.setExperience(0, 0);
                } else if (c.getBoolean("resetXp")) {
                    p.setExperience(0, 0);
                }

                p.teleport(p.getSpawn());
                e.setCancelled(true);
            }
        }
    }
}
