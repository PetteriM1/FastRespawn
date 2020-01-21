package idk.plugin.fastrespawn;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByBlockEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.item.Item;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main extends PluginBase implements Listener {

    private Config c;

    public void onEnable() {
        saveDefaultConfig();
        c = getConfig();
        if (c.getInt("configVersion") != 2) {
            c.set("removeEffects", true);
            c.set("extinguish", true);
            c.save();
            getLogger().info("Config updated");
            c = getConfig();
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void handleDeath(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();

            if (!p.hasPermission("fastrespawn")) {
                return;
            }

            if (p.getHealth() - e.getFinalDamage() < 1f) {
                boolean dmsg = c.getBoolean("deathMessages");
                String msg = "";
                List<String> params = new ArrayList<>();

                if (dmsg) {
                    params.add(p.getDisplayName());

                    switch (e.getCause()) {
                        case ENTITY_ATTACK:
                            if (e instanceof EntityDamageByEntityEvent) {
                                Entity ent = ((EntityDamageByEntityEvent) e).getDamager();
                                if (ent instanceof Player) {
                                    msg = "death.attack.player";
                                    params.add(((Player) ent).getDisplayName());
                                    break;
                                } else if (ent instanceof EntityLiving) {
                                    msg = "death.attack.mob";
                                    params.add(!Objects.equals(ent.getNameTag(), "") ? ent.getNameTag() : ent.getName());
                                    break;
                                } else {
                                    params.add("Unknown");
                                }
                            }
                            break;
                        case PROJECTILE:
                            if (e instanceof EntityDamageByEntityEvent) {
                                Entity ent = ((EntityDamageByEntityEvent) e).getDamager();
                                if (ent instanceof Player) {
                                    msg = "death.attack.arrow";
                                    params.add(((Player) ent).getDisplayName());
                                } else if (ent instanceof EntityLiving) {
                                    msg = "death.attack.arrow";
                                    params.add(!Objects.equals(ent.getNameTag(), "") ? ent.getNameTag() : ent.getName());
                                    break;
                                } else {
                                    params.add("Unknown");
                                }
                            }
                            break;
                        case VOID:
                            msg = "death.attack.outOfWorld";
                            break;
                        case FALL:
                            if (e.getFinalDamage() > 2) {
                                msg = "death.fell.accident.generic";
                                break;
                            }
                            msg = "death.attack.fall";
                            break;
                        case SUFFOCATION:
                            msg = "death.attack.inWall";
                            break;
                        case LAVA:
                            msg = "death.attack.lava";
                            break;
                        case FIRE:
                            msg = "death.attack.onFire";
                            break;
                        case FIRE_TICK:
                            msg = "death.attack.inFire";
                            break;
                        case DROWNING:
                            msg = "death.attack.drown";
                            break;
                        case CONTACT:
                            if (e instanceof EntityDamageByBlockEvent) {
                                if (((EntityDamageByBlockEvent) e).getDamager().getId() == Block.CACTUS) {
                                    msg = "death.attack.cactus";
                                }
                            }
                            break;
                        case BLOCK_EXPLOSION:
                        case ENTITY_EXPLOSION:
                            if (e instanceof EntityDamageByEntityEvent) {
                                Entity ent = ((EntityDamageByEntityEvent) e).getDamager();
                                if (ent instanceof Player) {
                                    msg = "death.attack.explosion.player";
                                    params.add(((Player) ent).getDisplayName());
                                } else if (ent instanceof EntityLiving) {
                                    msg = "death.attack.explosion.player";
                                    params.add(!Objects.equals(ent.getNameTag(), "") ? ent.getNameTag() : ent.getName());
                                    break;
                                } else {
                                    msg = "death.attack.explosion";
                                }
                            } else {
                                msg = "death.attack.explosion";
                            }
                            break;
                        case MAGIC:
                            msg = "death.attack.magic";
                            break;
                        case HUNGER:
                            msg = "death.attack.starve";
                            break;
                        default:
                            msg = "death.attack.generic";
                            break;
                    }
                }

                PlayerDeathEvent ev = new PlayerDeathEvent(p, p.getDrops(), new TranslationContainer(msg, params.toArray(new String[0])), p.getExperienceLevel());
                getServer().getPluginManager().callEvent(ev);
                
                if (c.getBoolean("resetHealth")) {
                    p.setHealth(p.getMaxHealth());
                }

                if (c.getBoolean("resetFood")) {
                    p.getFoodData().setLevel(p.getFoodData().getMaxLevel());
                }

                if (c.getBoolean("removeEffects")) {
                    p.removeAllEffects();
                }

                if (c.getBoolean("extinguish")) {
                    p.extinguish();
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

                if (dmsg) {
                    getServer().broadcast(ev.getDeathMessage(), Server.BROADCAST_CHANNEL_USERS);
                }

                p.teleport(p.getSpawn());
                e.setCancelled(true);
            }
        }
    }
}
