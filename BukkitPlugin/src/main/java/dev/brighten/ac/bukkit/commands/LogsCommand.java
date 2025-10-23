package dev.brighten.ac.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.annotation.Optional;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.CheckSettings;
import dev.brighten.ac.gui.Logs;
import dev.brighten.ac.logging.Log;
import dev.brighten.ac.utils.Color;
import dev.brighten.ac.utils.MojangAPI;
import dev.brighten.ac.utils.Pastebin;
import dev.brighten.ac.utils.Priority;
import dev.brighten.ac.utils.annotation.Init;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Init(priority = Priority.LOW)
@CommandAlias("kauri|anticheat|ac")
@CommandPermission("anticheat.command")
public class LogsCommand extends BaseCommand {

    @Subcommand("logs")
    @Syntax("[player] [check] [limit]")
    @CommandCompletion("@players @checkIds")
    @CommandPermission("anticheat.command.logs")
    @Description("Get player logs")
    public void onLogs(Player sender, @Single String playername,
                       @Single @Optional @Default("none") String check, @Single @Optional @Default("5000") int limit) {
        UUID uuid = MojangAPI.getUUID(playername).orElse(null);

        if(uuid == null) {
            sender.sendMessage(Color.Red + String.format("There is no player with the name \"%s\"", playername));
            return;
        }

        sender.sendMessage(Color.Red + "Getting logs for " + playername + " with UUID " + uuid + " and check " + check + " with limit " + limit + "...");

        Anticheat.INSTANCE.getRunUtils().taskAsync(() -> {
            Logs logs;
            if(check.equals("none")) {
                logs = new Logs(uuid);

            } else {
                logs = new Logs(uuid, check);

            }
            logs.showMenu(sender);
        });
    }

    @Subcommand("logs paste")
    @CommandPermission("kauri.command.logs")
    @Syntax("[player] [check|none]")
    @CommandCompletion("@players @checkIds")
    @Description("View logs via Pastebin")
    public void onLogsPasteBin(CommandSender sender,
                               @Single String playerName,
                               @Single @Optional @Default("none") String check,
                               String[] args) {
        if(args.length == 0) {
            sender.sendMessage(Color.Red + "Usage: /kauri logs paste <player>");
            return;
        }

        UUID uuid = MojangAPI.getUUID(playerName).orElse(null);

        if(uuid == null) {
            sender.sendMessage(Color.Red + String.format("There is no player with the name \"%s\"", playerName));
            return;
        }

        sender.sendMessage(Color.Red + "Getting logs for " + playerName + "...");

        if(check.equals("none")) {
            runPastebin(sender, uuid, playerName, null);
        } else {
            runPastebin(sender, uuid, playerName, check);
        }
    }

    @SuppressWarnings("deprecation")
    @Subcommand("logs web")
    @CommandPermission("kauri.command.logs")
    @Syntax("[player]")
    @CommandCompletion("@players")
    @Description("View the logs of a user")
    public void onLogsWeb(CommandSender sender, String[] args) {
        if(args.length == 0) {
            if(sender instanceof Player player) {
                runWebLog(sender, player);
            } else sender.sendMessage(Color.translate("You cannot view your own logs from console."));
        } else {
            OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);

            if(player == null) {
                sender.sendMessage(Color.translate("&cSomehow, out of hundreds of millions of"
                        + "Minecraft accounts, you found one that doesn't exist."));
                return;
            }

            runWebLog(sender, player);
        }
    }
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private void runPastebin(CommandSender sender, UUID uuid, String name, @Nullable String checkId) {
        Consumer<List<Log>> logHandle = logs -> {
            String body = logs.stream().map(log -> String.format("[%s] (%s; vl=%.2f): %s", timeToDate(log.getTime()),
                            log.getCheckName(), log.getVl(), log.getData()))
                    .collect(Collectors.joining("\n"));
            String title = String.format("%s's Anticheat Logs at %s", name, format.format(new Date()));
            try {
                var pastebin = Pastebin.makePaste(body, title, Pastebin.Privacy.UNLISTED);

                sender.sendMessage(String.format(Color.Green + "Logs for %s: %s", name, Color.White + pastebin));
            } catch (UnsupportedEncodingException e) {
                sender.sendMessage(Color.Red + "There was an error trying to make the pasted link. Check console.");
                Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Anticheat Logs could not be made", e);
            }
        };

        if(checkId != null) {
            Anticheat.INSTANCE.getLogManager().getLogs(uuid, checkId, 100000, 0, logHandle);
        } else {
            Anticheat.INSTANCE.getLogManager().getLogs(uuid, 100000, 0, logHandle);
        }
    }


    private String timeToDate(long timeStamp) {
        return format.format(new Date(timeStamp));
    }

    @SuppressWarnings("unchecked")
    private void runWebLog(CommandSender sender, OfflinePlayer target) {
        //val logs = Kauri.INSTANCE.loggerManager.getLogs(target.getUniqueId());
        Anticheat.INSTANCE.getLogManager().getLogs(target.getUniqueId(), 100000, 0, logs -> {
            Map<String, Integer> violations = new HashMap<>();
            for (Log log : logs) {
                violations.compute(log.getCheckId(), (name, count) -> {
                    if(count == null) {
                        return 1;
                    }

                    return count + 1;
                });
            }


            StringBuilder url = new StringBuilder("https://funkemunky.cc/api/kauri?uuid="
                    + target.getUniqueId().toString().replaceAll("-", "")
                    + (!violations.isEmpty() ? "&violations=" : ""));

            if (!violations.isEmpty()) {
                for (String key : violations.keySet()) {
                    if (Anticheat.INSTANCE.getCheckManager().isCheck(key)) {
                        CheckSettings checkData = Anticheat.INSTANCE.getCheckManager()
                                .getCheckSettings(key);
                        int vl = violations.get(key), maxVL = checkData.getPunishVl();
                        boolean developer = false;

                        String toAppend = key + ":" + vl + ":" + maxVL + ":" + developer + ";";
                        toAppend = toAppend.replaceAll(" ", "%20");

                        url.append(toAppend);

                    }
                }

                if (!violations.isEmpty()) {
                    url.deleteCharAt(url.length() - 1);
                }

                String finalURL = "https://funkemunky.cc/api/kauri/cache/%id%";

                try {
                    URL url2Run = new URL(url.toString());
                    //%3F
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url2Run
                            .openConnection().getInputStream(), StandardCharsets.UTF_8));

                    finalURL = finalURL.replace("%id%", readAll(reader));
                } catch (IOException e) {
                    Anticheat.INSTANCE.getLogger().log(Level.SEVERE, "Anticheat Logs could not be made", e);
                }

                sender.sendMessage(Color.translate("&aView the log here&7: &f" + finalURL));
            } else {
                sender.sendMessage(Color.translate("&cPlayer has no logs."));
            }
        });
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}
