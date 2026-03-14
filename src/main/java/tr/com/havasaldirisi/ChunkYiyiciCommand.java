package tr.com.havasaldirisi;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ChunkYiyiciCommand implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey oltaKey;
    private final NamespacedKey gucKey;

    public ChunkYiyiciCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.oltaKey = new NamespacedKey(plugin, "chunk_yiyici_olta");
        this.gucKey = new NamespacedKey(plugin, "chunk_yiyici_guc");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Sadece oyuncular kullanabilir!");
            return true;
        }

        if (!player.hasPermission("bariskesertools.admin") && !player.isOp()) {
            player.sendMessage(ChatColor.RED + "Bunun için yetkin yok!");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Kullanım: /chunk-yiyici [GÜÇ-20<500]");
            return true;
        }

        int guc;
        try {
            guc = Integer.parseInt(args[0]);
            if (guc < 20 || guc > 500) {
                player.sendMessage(ChatColor.RED + "Güç miktarı en az 20, en fazla 500 olabilir!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Lütfen geçerli bir sayı girin.");
            return true;
        }

        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED + "☠ Chunk Yiyici Olta ☠");
            meta.getPersistentDataContainer().set(oltaKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(gucKey, PersistentDataType.INTEGER, guc);
            rod.setItemMeta(meta);
        }

        player.getInventory().addItem(rod);
        player.sendMessage(ChatColor.GREEN + "Chunk Yiyici Olta (Güç: " + guc + ") başarıyla verildi.");
        player.sendMessage(ChatColor.YELLOW + "Yere atıp çektiğinizde 10 blok yukarıda bir TNT belirecek ve patladığı an dibe kadar inecektir.");
        return true;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.hasItemMeta()) {
            item = player.getInventory().getItemInOffHand();
            if (item == null || !item.hasItemMeta()) return;
        }

        if (item.getItemMeta().getPersistentDataContainer().has(oltaKey, PersistentDataType.BYTE)) {
            if (event.getState() == PlayerFishEvent.State.REEL_IN || event.getState() == PlayerFishEvent.State.IN_GROUND) {
                FishHook hook = event.getHook();
                Location target = hook.getLocation();

                int guc = item.getItemMeta().getPersistentDataContainer().getOrDefault(gucKey, PersistentDataType.INTEGER, 20);

                Location spawnLoc = target.clone().add(0, 10, 0);
                TNTPrimed tnt = player.getWorld().spawn(spawnLoc, TNTPrimed.class);
                tnt.setYield((float) guc); // TNT Patlama Gücü 
                tnt.setFuseTicks(60); // 3 saniye sonra patlar
                tnt.setMetadata("chunk_yiyici_root", new FixedMetadataValue(plugin, guc)); // İlk patlayan tnt olduğunu belirten etiket

                hook.remove(); // Oltayı çektikten sonra iğne yok olsun
            }
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof TNTPrimed tnt) {

            // Patlama devasa olduğu için lag önlemi - Bloğun eşya (drop) olarak düşmesini engeller. Aksi takdirde çöker!
            if (tnt.hasMetadata("chunk_yiyici_root") || tnt.hasMetadata("chunk_yiyici_child")) {
                event.setYield(0f); // Bloklar kırılır ama yere eşya olarak düşmezler.
            }

            // Eğer bu tnt havadan inen ve yeri kazan ilk TNT ise
            if (tnt.hasMetadata("chunk_yiyici_root")) {
                int guc = tnt.getMetadata("chunk_yiyici_root").get(0).asInt();
                Location loc = tnt.getLocation();
                World world = loc.getWorld();
                
                int currentY = loc.getBlockY();
                int minY = world.getMinHeight(); // Katman kayası sınırına kadar inebilmek için haritanın en alt sınırını alır.
                
                // Güç ne kadar yüksekse etki alanı o kadar büyüyeceğinden sunucu TPS'i açısından aralığı ona göre orantılı açıyoruz.
                int aralik = (guc <= 50) ? 5 : (guc <= 100) ? 10 : (guc <= 250) ? 15 : 20;

                int delay = 5; // İlk tnt patladıktan sonra yeraltına inecek olanlar 5'er tick(0.25 sn) gecikmeyle sırasıyla patlasınlar
                
                for (int y = currentY - aralik; y > minY; y -= aralik) {
                    Location childLoc = new Location(world, loc.getX(), y, loc.getZ());
                    TNTPrimed childTnt = world.spawn(childLoc, TNTPrimed.class);
                    childTnt.setYield((float) guc);
                    childTnt.setFuseTicks(delay);
                    childTnt.setMetadata("chunk_yiyici_child", new FixedMetadataValue(plugin, true));
                    
                    delay += 5; // Aşağı indikçe yarım saniye gecikmeli patlar, muhteşem bir drill (sondaj) efekti verir!
                }
            }
        }
    }
}
