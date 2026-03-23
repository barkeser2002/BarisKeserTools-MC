package tr.com.havasaldirisi;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class SudoOpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Komutu kullanan kişinin yetkisini kontrol et
        if (!sender.hasPermission("bariskesertools.sudoop") && !sender.isOp()) {
            sender.sendMessage("§cBu komutu kullanmak için yetkiniz yok!");
            return true;
        }

        // En az 2 argüman girilmiş mi kontrol et (/sudoop <oyuncu> <komut>)
        if (args.length < 2) {
            sender.sendMessage("§cKullanım: /sudoop <oyuncu> <komut>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cOyuncu bulunamadı veya çevrimdışı.");
            return true;
        }

        // İlk argümandan (oyuncu adı) sonrasını birleştirerek çalıştırılacak komutu oluştur
        String commandToRun = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Oyuncunun mevcut OP durumunu kaydet
        boolean wasOp = target.isOp();

        try {
            // Anlık OP ver ve komutu sunucuya ilet
            target.setOp(true);
            Bukkit.getServer().dispatchCommand(target, commandToRun);
            sender.sendMessage("§aBaşarılı: §e" + commandToRun + " §akomutu §e" + target.getName() + " §aadına çalıştırıldı!");
        } catch (Exception e) {
            sender.sendMessage("§cKomut çalıştırılırken bir hata oluştu.");
            e.printStackTrace();
        } finally {
            // İşlem bittiğinde veya hata verdiğinde OP durumunu eski haline döndür
            target.setOp(wasOp);
        }

        return true;
    }
}
