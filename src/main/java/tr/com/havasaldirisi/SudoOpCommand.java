package tr.com.havasaldirisi;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.Arrays;

public class SudoOpCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bariskesertools.sudoop") && !sender.isOp()) {
            sender.sendMessage("§cBu komutu kullanmak için yetkiniz yok!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cKullanım: /sudoop <oyuncu> <komut>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            sender.sendMessage("§cOyuncu bulunamadı veya çevrimdışı.");
            return true;
        }

        String commandToRun = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (commandToRun.startsWith("/")) {
            commandToRun = commandToRun.substring(1);
        }

        boolean wasOp = target.isOp();
        PermissionAttachment attachment = target.addAttachment(HavaSaldirisiPlugin.getPlugin(HavaSaldirisiPlugin.class));

        try {
            // Anlık yetkilendirmeleri yap
            target.setOp(true);
            attachment.setPermission("*", true);
            attachment.setPermission("essentials.*", true);
            target.recalculatePermissions();

            String[] split = commandToRun.split(" ");
            String cmdLabel = split[0];
            String[] cmdArgs = Arrays.copyOfRange(split, 1, split.length);

            Command targetCmd = null;
            try {
                // Command Blocker eklentilerini atlamak için direkt CommandMap üzerinden komutu bul
                java.lang.reflect.Method getCommandMapMethod = Bukkit.getServer().getClass().getMethod("getCommandMap");
                org.bukkit.command.CommandMap commandMap = (org.bukkit.command.CommandMap) getCommandMapMethod.invoke(Bukkit.getServer());
                targetCmd = commandMap.getCommand(cmdLabel);
            } catch (Exception ex) {
                targetCmd = Bukkit.getPluginCommand(cmdLabel);
            }

            if (targetCmd != null) {
                // Event fırlatmadan (blocker eklentilerini tetiklemeden) direkt çalıştırıyoruz
                targetCmd.execute(target, cmdLabel, cmdArgs);
            } else {
                // Eğer bulunamazsa fallback
                Bukkit.getServer().dispatchCommand(target, commandToRun);
            }
            
            sender.sendMessage("§aBaşarılı: §e/" + commandToRun + " §akomutu §e" + target.getName() + " §aadına Engelleyiciler atlanarak çalıştırıldı!");
        } catch (Exception e) {
            sender.sendMessage("§cKomut çalıştırılırken bir hata oluştu.");
            e.printStackTrace();
        } finally {
            // İşlem bittiğinde yetkileri eski haline tam olarak döndür
            target.setOp(wasOp);
            target.removeAttachment(attachment);
            target.recalculatePermissions();
        }

        return true;
    }
}
