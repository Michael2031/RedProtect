package br.net.fabiozumbi12.RedProtect.Bukkit.listeners;

import br.net.fabiozumbi12.RedProtect.Bukkit.RPUtil;
import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import br.net.fabiozumbi12.RedProtect.Bukkit.config.RPConfig;
import br.net.fabiozumbi12.RedProtect.Bukkit.config.RPLang;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Crops;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class RPGlobalListener implements Listener {

    public RPGlobalListener() {
        RedProtect.get().logger.debug("Loaded RPGlobalListener...");
    }

    /**
     * @param p   - Player
     * @param b   - Block
     * @param fat - 1 = Place Block | 2 = Break Block
     * @return Boolean - Can build or not.
     */
    private boolean bypassBuild(Player p, Block b, int fat) {
        return fat == 1 && RPConfig.getGlobalFlagList(p.getWorld().getName() + ".if-build-false.place-blocks").stream().anyMatch(b.getType().name()::matches) || fat == 2 && RPConfig.getGlobalFlagList(p.getWorld().getName() + ".if-build-false.break-blocks").stream().anyMatch(b.getType().name()::matches) || p.hasPermission("redprotect.bypass.world") || (!RPConfig.needClaimToBuild(p, b) && RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".build"));
    }

    @EventHandler
    public void onLeafDecay(LeavesDecayEvent e) {
        RedProtect.get().logger.debug("RPBlockListener - Is LeavesDecayEvent event");
        Region r = RedProtect.get().rm.getTopRegion(e.getBlock().getLocation());
        if (r == null && !RPConfig.getGlobalFlagBool(e.getBlock().getWorld().getName() + ".allow-changes-of.leaves-decay")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFlow(BlockFromToEvent e) {
        RedProtect.get().logger.debug("RPGlobalListener - Is BlockFromToEvent event");
        if (e.isCancelled()) {
            return;
        }
        Block b = e.getToBlock();
        Block bfrom = e.getBlock();
        RedProtect.get().logger.debug("RPGlobalListener - Is BlockFromToEvent event is to " + b.getType().name() + " from " + bfrom.getType().name());
        Region r = RedProtect.get().rm.getTopRegion(b.getLocation());
        if (r != null) {
            return;
        }
        if (bfrom.isLiquid() && !RPConfig.getGlobalFlagBool(b.getWorld().getName() + ".liquid-flow")) {
            e.setCancelled(true);
            return;
        }

        if ((bfrom.getType().equals(Material.WATER) || (bfrom.getType().name().contains("WATER") && (bfrom.getType().name().contains("STATIONARY") || bfrom.getType().name().contains("FLOWING"))))
                && !RPConfig.getGlobalFlagBool(b.getWorld().getName() + ".allow-changes-of.water-flow")) {
            e.setCancelled(true);
            return;
        }

        if ((bfrom.getType().equals(Material.LAVA) || (bfrom.getType().name().contains("LAVA") && (bfrom.getType().name().contains("STATIONARY") || bfrom.getType().name().contains("FLOWING"))))
                && !RPConfig.getGlobalFlagBool(b.getWorld().getName() + ".allow-changes-of.lava-flow")) {
            e.setCancelled(true);
            return;
        }

        if (!b.isEmpty() && !RPConfig.getGlobalFlagBool(b.getWorld().getName() + ".allow-changes-of.flow-damage")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent e) {
        Region r = RedProtect.get().rm.getTopRegion(e.getEntity().getLocation());
        if (r != null) {
            return;
        }
        Entity ent = e.getEntity();
        if (ent instanceof LivingEntity && !(ent instanceof Monster)) {
            if (RPConfig.getGlobalFlagBool(ent.getWorld().getName() + ".invincible")) {
                if (ent instanceof Animals) {
                    ((Animals) ent).setTarget(null);
                }
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void PlayerDropItem(PlayerDropItemEvent e) {
        Location l = e.getItemDrop().getLocation();
        Player p = e.getPlayer();
        Region r = RedProtect.get().rm.getTopRegion(l);

        if (r == null && !RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".player-candrop")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void PlayerPickup(PlayerPickupItemEvent e) {
        Location l = e.getItem().getLocation();
        Player p = e.getPlayer();
        Region r = RedProtect.get().rm.getTopRegion(l);

        if (r == null && !RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".player-canpickup")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerFrostWalk(EntityBlockFormEvent e) {
        Region r = RedProtect.get().rm.getTopRegion(e.getBlock().getLocation());
        if (r != null) {
            return;
        }
        RedProtect.get().logger.debug("RPGlobalListener - EntityBlockFormEvent canceled? " + e.isCancelled());
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (!RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".iceform-by.player") && !p.hasPermission("redprotect.bypass.world")) {
                e.setCancelled(true);
            }
        } else if (!RPConfig.getGlobalFlagBool(e.getEntity().getWorld().getName() + ".iceform-by.entity")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();

        //set velocities
        if (!p.hasPermission("redprotect.bypass.velocity")) {
            if (RPConfig.getGlobalFlagFloat(p.getWorld().getName() + ".player-velocity.walk-speed") >= 0) {
                p.setWalkSpeed(RPConfig.getGlobalFlagFloat(p.getWorld().getName() + ".player-velocity.walk-speed"));
            }
            if (RPConfig.getGlobalFlagFloat(p.getWorld().getName() + ".player-velocity.fly-speed") >= 0) {
                p.setFlySpeed(RPConfig.getGlobalFlagFloat(p.getWorld().getName() + ".player-velocity.fly-speed"));
            }
        }

        if (RedProtect.get().version >= 191) {
            if (!RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".elytra.allow")) {
                ItemStack item = p.getInventory().getChestplate();
                if (item != null && item.getType().equals(Material.ELYTRA)) {
                    PlayerInventory inv = p.getInventory();
                    inv.setChestplate(new ItemStack(Material.AIR));
                    if (inv.firstEmpty() == -1) {
                        p.getWorld().dropItem(p.getLocation(), item);
                    } else {
                        inv.setItem(inv.firstEmpty(), item);
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 10, 1);
                    RPLang.sendMessage(p, "globallistener.elytra.cantequip");
                    return;
                }
            }

            double boost = RPConfig.getGlobalFlagDouble(p.getWorld().getName() + ".elytra.boost");
            if (boost > 0 && p.isGliding() && RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".elytra.allow")) {
                Vector vec = new Vector(0.0D, p.getLocation().getDirection().getY(), 0.0D);
                p.setVelocity(p.getVelocity().add(vec.multiply(0.1D * boost / 2)));
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();

        if (RedProtect.get().version >= 190) {
            Location to = e.getTo();
            if (p.getInventory().getChestplate() != null &&
                    p.getInventory().getChestplate().getType().equals(Material.ELYTRA) &&
                    !RPConfig.getGlobalFlagBool(to.getWorld().getName() + ".elytra.allow")) {
                RPLang.sendMessage(p, "globallistener.elytra.cantworld");
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent e) {
        RedProtect.get().logger.debug("RPGlobalListener - Is BlockPlaceEvent event! Cancelled? " + e.isCancelled());
        if (e.isCancelled() || e.getItemInHand() == null) {
            return;
        }

        Block b = e.getBlock();
        Player p = e.getPlayer();
        Material item = e.getItemInHand().getType();
        Region r = RedProtect.get().rm.getTopRegion(e.getBlock().getLocation());
        if (r != null) {
            return;
        }

        if (!RPUtil.canBuildNear(p, b.getLocation())) {
            e.setCancelled(true);
            return;
        }

        if (item.name().contains("MINECART") || item.name().contains("BOAT")) {
            if (!RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".use-minecart") && !p.hasPermission("redprotect.bypass.world")) {
                e.setCancelled(true);
                RedProtect.get().logger.debug("RPGlobalListener - Can't place minecart/boat!");
            }
        } else {
            if (!bypassBuild(p, b, 1)) {
                e.setCancelled(true);
                RedProtect.get().logger.debug("RPGlobalListener - Can't Build!");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        RedProtect.get().logger.debug("RPGlobalListener - Is BlockBreakEvent event! Cancelled? " + e.isCancelled());
        if (e.isCancelled()) {
            return;
        }

        Block b = e.getBlock();
        Player p = e.getPlayer();
        Region r = RedProtect.get().rm.getTopRegion(b.getLocation());
        if (r != null) {
            return;
        }

        if (!RPUtil.canBuildNear(p, b.getLocation())) {
            e.setCancelled(true);
            return;
        }

        if (!bypassBuild(p, b, 2)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent e) {
        RedProtect.get().logger.debug("RPGlobalListener - Is PlayerInteractEvent event! Cancelled? " + e.isCancelled());
        if (e.isCancelled()) {
            return;
        }
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        ItemStack itemInHand = e.getItem();
        Location l = null;

        if (b != null) {
            l = b.getLocation();
            RedProtect.get().logger.debug("RPGlobalListener - Is PlayerInteractEvent event. The block is " + b.getType().name());
        } else {
            l = p.getLocation();
        }

        if (b != null && b.getState() instanceof Sign) {
            Sign s = (Sign) b.getState();
            if (ChatColor.stripColor(s.getLine(1)).equals(ChatColor.stripColor(RPLang.get("_redprotect.prefix")))) {
                b.setType(Material.AIR);
                e.setUseInteractedBlock(Result.DENY);
                e.setUseItemInHand(Result.DENY);
                e.setCancelled(true);
                return;
            }
        }

        Region r = RedProtect.get().rm.getTopRegion(l);

        //deny item usage
        List<String> items = RPConfig.getGlobalFlagList(p.getWorld().getName() + ".deny-item-usage.items");
        if (e.getItem() != null && items.stream().anyMatch(e.getItem().getType().name()::matches)) {
            if (r != null && ((!RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".deny-item-usage.allow-on-claimed-rps") && r.canBuild(p)) ||
                    (RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".deny-item-usage.allow-on-claimed-rps") && !r.canBuild(p)))) {
                RPLang.sendMessage(p, "playerlistener.region.cantuse");
                e.setUseInteractedBlock(Event.Result.DENY);
                e.setUseItemInHand(Event.Result.DENY);
                e.setCancelled(true);
                return;
            }
            if (r == null && !RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".deny-item-usage.allow-on-wilderness") && !RedProtect.get().ph.hasPerm(p, "redprotect.bypass.world")) {
                RPLang.sendMessage(p, "playerlistener.region.cantuse");
                e.setUseInteractedBlock(Event.Result.DENY);
                e.setUseItemInHand(Event.Result.DENY);
                e.setCancelled(true);
                return;
            }
        }

        if (b == null || r != null) {
            return;
        }

        if ((b instanceof Crops
                || b.getType().equals(Material.PUMPKIN_STEM)
                || b.getType().equals(Material.MELON_STEM)
                || b.getType().toString().contains("CROPS")
                || b.getType().toString().contains("SOIL")
                || b.getType().toString().contains("CHORUS_")
                || b.getType().toString().contains("BEETROOT_")
                || b.getType().toString().contains("SUGAR_CANE")) &&
                !RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".allow-crops-trample") && !p.hasPermission("redprotect.bypass.world")) {
            e.setCancelled(true);
            return;
        }

        if (b.getType().equals(Material.DRAGON_EGG) ||
                b.getType().name().equalsIgnoreCase("BED") ||
                b.getType().name().contains("NOTE_BLOCK") ||
                b.getType().name().contains("CAKE")) {

            if ((RPConfig.getGlobalFlagList(p.getWorld().getName() + ".if-build-false.break-blocks").stream().noneMatch(b.getType().name()::matches)
                    || RPConfig.getGlobalFlagList(p.getWorld().getName() + ".if-interact-false.allow-blocks").stream().noneMatch(b.getType().name()::matches)) &&
                    !bypassBuild(p, null, 0)) {
                RPLang.sendMessage(p, "playerlistener.region.cantinteract");
                e.setCancelled(true);
                return;
            }
        }

        if (itemInHand != null) {
            if (itemInHand.getType().name().startsWith("BOAT") || itemInHand.getType().name().contains("MINECART")) {
                if (!RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".use-minecart") && !p.hasPermission("redprotect.bypass.world")) {
                    e.setUseItemInHand(Event.Result.DENY);
                    e.setCancelled(true);
                    return;
                }
            }
            if (itemInHand.getType().equals(Material.PAINTING) || itemInHand.getType().equals(Material.ITEM_FRAME) || itemInHand.getType().equals(Material.ARMOR_STAND)) {
                if (RPConfig.getGlobalFlagList(p.getWorld().getName() + ".if-build-false.place-blocks").stream().noneMatch(itemInHand.getType().name()::matches) && !bypassBuild(p, null, 0)) {
                    e.setUseItemInHand(Event.Result.DENY);
                    e.setCancelled(true);
                    return;
                }
            }
        }

        if (!RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".interact") && !p.hasPermission("redprotect.bypass.world")) {
            if (RPConfig.getGlobalFlagList(p.getWorld().getName() + ".if-interact-false.allow-blocks").stream().anyMatch(b.getType().name()::matches)) {
                return;
            }
            e.setUseItemInHand(Event.Result.DENY);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Player p = e.getPlayer();
        Entity ent = e.getRightClicked();
        Location l = ent.getLocation();
        Region r = RedProtect.get().rm.getTopRegion(l);
        if (r != null) {
            return;
        }

        if (ent instanceof ItemFrame || ent instanceof Painting) {
            if (!bypassBuild(p, null, 0)) {
                e.setCancelled(true);
                return;
            }
        }

        if (ent instanceof Minecart || ent instanceof Boat) {
            if (!RPConfig.getGlobalFlagBool(l.getWorld().getName() + ".use-minecart") && !p.hasPermission("redprotect.bypass.world")) {
                e.setCancelled(true);
                return;
            }
        }

        if (!RPConfig.getGlobalFlagBool(l.getWorld().getName() + ".interact") && !p.hasPermission("redprotect.bypass.world") && (!(ent instanceof Player))) {
            if (RPConfig.getGlobalFlagList(p.getWorld().getName() + ".if-interact-false.allow-entities").stream().anyMatch(ent.getType().name()::matches)) {
                return;
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingDamaged(HangingBreakByEntityEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Entity ent = e.getRemover();
        Location loc = e.getEntity().getLocation();
        Region r = RedProtect.get().rm.getTopRegion(loc);
        if (r != null) {
            return;
        }

        if (ent instanceof Player) {
            Player p = (Player) ent;
            if (!bypassBuild(p, null, 0)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBucketUse(PlayerBucketEmptyEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Location l = e.getBlockClicked().getLocation();
        Region r = RedProtect.get().rm.getTopRegion(l);
        if (r != null) {
            return;
        }

        if (!RPUtil.canBuildNear(e.getPlayer(), l)) {
            e.setCancelled(true);
            return;
        }

        if (!bypassBuild(e.getPlayer(), null, 0)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Location l = e.getBlockClicked().getLocation();
        Region r = RedProtect.get().rm.getTopRegion(l);
        if (r != null) {
            return;
        }

        if (!RPUtil.canBuildNear(e.getPlayer(), l)) {
            e.setCancelled(true);
            return;
        }

        if (!bypassBuild(e.getPlayer(), null, 0)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageEntity(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Entity e1 = e.getEntity();
        Entity e2 = e.getDamager();

        Location loc = e1.getLocation();
        Region r1 = RedProtect.get().rm.getTopRegion(loc);
        if (r1 != null) {
            return;
        }

        if (e2 instanceof Creeper || e2.getType().equals(EntityType.PRIMED_TNT) || e2.getType().equals(EntityType.MINECART_TNT)) {
            if (e1 instanceof Player) {
                if (!RPConfig.getGlobalFlagBool(loc.getWorld().getName() + ".explosion-entity-damage")) {
                    e.setCancelled(true);
                    return;
                }
            }
            if (e1 instanceof Animals || e1 instanceof Villager || e1 instanceof Golem) {
                if (!RPConfig.getGlobalFlagBool(loc.getWorld().getName() + ".explosion-entity-damage")) {
                    e.setCancelled(true);
                    return;
                }
            }
            if (e1 instanceof Monster) {
                if (!RPConfig.getGlobalFlagBool(loc.getWorld().getName() + ".explosion-entity-damage")) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        if (e2 instanceof Player) {
            Player p = (Player) e2;

            if (e.getCause().equals(DamageCause.LIGHTNING) || e.getCause().equals(DamageCause.BLOCK_EXPLOSION) || e.getCause().equals(DamageCause.ENTITY_EXPLOSION)) {
                if (!RPConfig.getGlobalFlagBool(loc.getWorld().getName() + ".entity-block-damage")) {
                    e.setCancelled(true);
                    return;
                }
            }
            if ((e1 instanceof Minecart || e1 instanceof Boat) && !RPConfig.getGlobalFlagBool(loc.getWorld().getName() + ".use-minecart") && !p.hasPermission("redprotect.bypass.world")) {
                e.setCancelled(true);
                return;
            }
            if (e1 instanceof Player) {
                if (!RPConfig.getGlobalFlagBool(loc.getWorld().getName() + ".pvp") && !p.hasPermission("redprotect.bypass.world")) {
                    e.setCancelled(true);
                    return;
                }
            }
            if (e1 instanceof Animals || e1 instanceof Villager || e1 instanceof Golem) {
                if (!RPConfig.getGlobalFlagBool(loc.getWorld().getName() + ".player-hurt-passives") && !p.hasPermission("redprotect.bypass.world")) {
                    e.setCancelled(true);
                    return;
                }
            }
            if (e1 instanceof Monster) {
                if (!RPConfig.getGlobalFlagBool(loc.getWorld().getName() + ".player-hurt-monsters") && !p.hasPermission("redprotect.bypass.world")) {
                    e.setCancelled(true);
                    return;
                }
            }
            if (e1 instanceof Hanging || e1 instanceof EnderCrystal || e1 instanceof ArmorStand) {
                if (RPConfig.getGlobalFlagList(p.getWorld().getName() + ".if-build-false.break-blocks").stream().noneMatch(e1.getType().name()::matches) && !bypassBuild(p, null, 0)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        if (e2 instanceof Projectile) {
            Projectile proj = (Projectile) e2;
            if (proj.getShooter() instanceof Player) {
                Player p = (Player) proj.getShooter();

                if (e1 instanceof Player) {
                    if (!RPConfig.getGlobalFlagBool(loc.getWorld().getName() + ".pvp") && !p.hasPermission("redprotect.bypass.world")) {
                        e.setCancelled(true);
                        return;
                    }
                }
                if (e1 instanceof Animals || e1 instanceof Villager || e1 instanceof Golem) {
                    if (!RPConfig.getGlobalFlagBool(loc.getWorld().getName() + ".player-hurt-passives") && !p.hasPermission("redprotect.bypass.world")) {
                        e.setCancelled(true);
                        return;
                    }
                }
                if (e1 instanceof Monster) {
                    if (!RPConfig.getGlobalFlagBool(loc.getWorld().getName() + ".player-hurt-monsters") && !p.hasPermission("redprotect.bypass.world")) {
                        e.setCancelled(true);
                        return;
                    }
                }
                if (e1 instanceof Hanging || e1 instanceof EnderCrystal || e1 instanceof ArmorStand) {
                    if (RPConfig.getGlobalFlagList(p.getWorld().getName() + ".if-build-false.break-blocks").stream().noneMatch(e1.getType().name()::matches) && !bypassBuild(p, null, 0)) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFrameBrake(HangingBreakEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Location l = e.getEntity().getLocation();
        Region r = RedProtect.get().rm.getTopRegion(l);
        if (r != null) {
            return;
        }

        if (e.getCause().toString().equals("EXPLOSION") || e.getCause().toString().equals("ENTITY")) {
            if (!RPConfig.getGlobalFlagBool(l.getWorld().getName() + ".entity-block-damage")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (e.isCancelled()) {
            return;
        }
        List<Block> toRemove = new ArrayList<>();
        for (Block b : e.blockList()) {
            Location l = b.getLocation();
            Region r = RedProtect.get().rm.getTopRegion(l);
            if (r == null && !RPConfig.getGlobalFlagBool(l.getWorld().getName() + ".entity-block-damage")) {
                toRemove.add(b);
            }
        }
        if (!toRemove.isEmpty()) {
            e.blockList().removeAll(toRemove);
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Block b = e.getBlock();
        Region r = RedProtect.get().rm.getTopRegion(b.getLocation());
        if (r != null) {
            return;
        }

        if (!RPConfig.getGlobalFlagBool(b.getWorld().getName() + ".fire-block-damage")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFireSpread(BlockSpreadEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Block b = e.getSource();
        Region r = RedProtect.get().rm.getTopRegion(b.getLocation());
        if (r != null) {
            return;
        }

        if ((b.getType().equals(Material.FIRE) || b.getType().name().contains("LAVA")) && !RPConfig.getGlobalFlagBool(b.getWorld().getName() + ".fire-spread")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        RedProtect.get().logger.debug("RPGlobalListener - Is CreatureSpawnEvent event! Cancelled? " + event.isCancelled());

        Entity e = event.getEntity();
        if (e == null) {
            return;
        }

        Location l = event.getLocation();
        Region r = RedProtect.get().rm.getTopRegion(l);
        if (r != null && RPConfig.getGlobalFlagBool(e.getWorld().getName() + ".spawn-allow-on-regions")) {
            return;
        }

        //blacklist
        if (e instanceof Monster && RPConfig.getGlobalFlagList(e.getWorld().getName() + ".spawn-blacklist").contains("MONSTER")) {
            event.setCancelled(true);
            return;
        }
        if ((e instanceof Animals || e instanceof Villager || e instanceof Golem) && RPConfig.getGlobalFlagList(e.getWorld().getName() + ".spawn-blacklist").contains("PASSIVES")) {
            event.setCancelled(true);
            return;
        }
        if (RPConfig.getGlobalFlagList(e.getWorld().getName() + ".spawn-blacklist").contains(e.getType().name())) {
            event.setCancelled(true);
            return;
        }

        //whitelist
        List<String> wtl = RPConfig.getGlobalFlagList(e.getWorld().getName() + ".spawn-whitelist");
        if (!wtl.isEmpty()) {
            if (e instanceof Monster && !wtl.contains("MONSTER")) {
                event.setCancelled(true);
                return;
            }
            if ((e instanceof Animals || e instanceof Villager || e instanceof Golem) && !wtl.contains("PASSIVES")) {
                event.setCancelled(true);
                return;
            }
            if (!wtl.contains(e.getType().name())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onVehicleBreak(VehicleDestroyEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (!(e.getAttacker() instanceof Player)) {
            return;
        }

        Vehicle cart = e.getVehicle();
        Player p = (Player) e.getAttacker();
        Region r = RedProtect.get().rm.getTopRegion(cart.getLocation());
        if (r != null) {
            return;
        }

        if (!RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".use-minecart") && !p.hasPermission("redprotect.bypass.world")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockStartBurn(BlockIgniteEvent e) {
        if (e.isCancelled()) {
            return;
        }

        Block b = e.getBlock();
        Block bignit = e.getIgnitingBlock();
        if (b == null || bignit == null) {
            return;
        }
        RedProtect.get().logger.debug("Is BlockIgniteEvent event from global-listener");
        Region r = RedProtect.get().rm.getTopRegion(b.getLocation());
        if (r != null) {
            return;
        }
        if ((bignit.getType().equals(Material.FIRE) || bignit.getType().name().contains("LAVA")) && !RPConfig.getGlobalFlagBool(b.getWorld().getName() + ".fire-spread")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void MonsterBlockBreak(EntityChangeBlockEvent event) {
        Entity e = event.getEntity();
        Block b = event.getBlock();
        Region r = RedProtect.get().rm.getTopRegion(event.getBlock().getLocation());
        if (r != null) {
            return;
        }

        if (b != null) {
            RedProtect.get().logger.debug("RPGlobalListener - Is EntityChangeBlockEvent event. Block: " + b.getType().name());
        }

        if (e instanceof Monster) {
            if (!RPConfig.getGlobalFlagBool(e.getWorld().getName() + ".entity-block-damage")) {
                event.setCancelled(true);
            }
        }
        if (e instanceof Player) {
            Player p = (Player) e;
            if (!bypassBuild(p, b, 2)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        if (e.getItem() == null) {
            return;
        }

        Player p = e.getPlayer();
        Location l = p.getLocation();

        Region r = RedProtect.get().rm.getTopRegion(l);

        //deny item usage
        List<String> items = RPConfig.getGlobalFlagList(p.getWorld().getName() + ".deny-item-usage.items");
        if (e.getItem() != null && items.stream().anyMatch(e.getItem().getType().name()::matches)) {
            if (r != null && ((!RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".deny-item-usage.allow-on-claimed-rps") && r.canBuild(p)) ||
                    (RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".deny-item-usage.allow-on-claimed-rps") && !r.canBuild(p)))) {
                RPLang.sendMessage(p, "playerlistener.region.cantuse");
                e.setCancelled(true);
                return;
            }
            if (r == null && !RPConfig.getGlobalFlagBool(p.getWorld().getName() + ".deny-item-usage.allow-on-wilderness") && !RedProtect.get().ph.hasPerm(p, "redprotect.bypass.world")) {
                RPLang.sendMessage(p, "playerlistener.region.cantuse");
                e.setCancelled(true);
            }
        }
    }
}