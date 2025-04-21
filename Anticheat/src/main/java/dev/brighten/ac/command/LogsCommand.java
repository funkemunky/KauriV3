package dev.brighten.ac.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.annotation.Optional;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.CheckSettings;
import dev.brighten.ac.gui.Logs;
import dev.brighten.ac.logging.Log;
import dev.brighten.ac.utils.Color;
import dev.brighten.ac.utils.Pastebin;
import dev.brighten.ac.utils.Priority;
import dev.brighten.ac.utils.annotation.Init;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Init(priority = Priority.LOW)
@CommandAlias("kauri|anticheat|ac")
@CommandPermission("anticheat.command")
public class LogsCommand extends BaseCommand {

    @Subcommand("logs")
    @Syntax("[player] [check] [limit]")
    @CommandCompletion("@players @checkIds")
    @CommandPermission("anticheat.command.logs")
    @Description("Get player logs")
    public void onLogs(CommandSender sender, @Single String playername,
                       @Single @Optional @Default("none") String check, @Single @Optional @Default("5000") int limit) {
        UUID uuid = Bukkit.getOfflinePlayer(playername).getUniqueId();

        sender.sendMessage(Color.Red + "Getting logs for " + playername + "...");

        Anticheat.INSTANCE.getRunUtils().taskAsync(() -> {
            if(sender instanceof Player) {
                if(check.equals("none")) {
                    Logs logs = new Logs(uuid);

                    logs.showMenu((Player) sender);
                } else {
                    Logs logs = new Logs(uuid, check);

                    logs.showMenu((Player) sender);
                }
            } else {
                List<String> logs = new ArrayList<>();

                if(check.equals("none")) {
                    Anticheat.INSTANCE.getLogManager().getLogs(uuid, limit, logsList -> {
                        logsList.forEach(log -> {
                            logs.add("[" + new Timestamp(log.getTime()).toLocalDateTime()
                                    .format(DateTimeFormatter.ISO_DATE_TIME) + "] funkemunky failed "
                                    + Anticheat.INSTANCE.getCheckManager().getIdToName().get(log.getCheckId()) + "(VL: "
                                    + log.getVl() + ") {" + log.getData() + "}");
                        });
                        if(logs.size() == 0) {
                            sender.sendMessage(Color.Gray + "There are no logs for player \"" + playername + "\"");
                        } else {
                            String url = null;
                            try {
                                url = Pastebin.makePaste(String.join("\n", logs), playername + "'s Logs",
                                        Pastebin.Privacy.UNLISTED);

                                sender.sendMessage(Color.Green + "Logs for " + playername + ": " + Color.White + url);
                            } catch (UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                } else {
                    Anticheat.INSTANCE.getLogManager().getLogs(uuid, check, limit, logsList -> {
                        logsList.forEach(log -> {
                            logs.add("[" + new Timestamp(log.getTime()).toLocalDateTime()
                                    .format(DateTimeFormatter.ISO_DATE_TIME) + "] funkemunky failed "
                                    + Anticheat.INSTANCE.getCheckManager().getIdToName().get(log.getCheckId())
                                    + "(VL: " + log.getVl() + ") {" + log.getData() + "}");
                        });
                        if(logs.size() == 0) {
                            sender.sendMessage(Color.Gray + " does not have any violations for check \"" + check + "\"");
                        } else {
                            String url = null;
                            try {
                                url = Pastebin.makePaste(String.join("\n", logs), playername + "'s Logs",
                                        Pastebin.Privacy.UNLISTED);

                                sender.sendMessage(Color.Green + "Logs for " + playername + ": " + Color.White + url);
                            } catch (UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            }
        });
    }

    @Subcommand("logs paste")
    @CommandPermission("kauri.command.logs")
    @Syntax("[player]")
    @CommandCompletion("@players")
    @Description("View logs via Pastebin")
    public void onLogsPasteBin(CommandSender sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage(Color.Red + "Usage: /kauri logs web <player>");
            return;
        }

        String playername = args[0];

        UUID uuid = Bukkit.getOfflinePlayer(playername).getUniqueId();

        sender.sendMessage(Color.Red + "Getting logs for " + playername + "...");

        Anticheat.INSTANCE.getRunUtils().taskAsync(() -> {
            if(sender instanceof Player) {
                Logs logs = new Logs(uuid);

                logs.showMenu((Player) sender);
            } else {
                List<String> logs = new ArrayList<>();

                Anticheat.INSTANCE.getLogManager().getLogs(uuid, logsList -> {
                    logsList.forEach(log -> {
                        logs.add("[" + new Timestamp(log.getTime()).toLocalDateTime()
                                .format(DateTimeFormatter.ISO_DATE_TIME) + "] funkemunky failed "
                                + Anticheat.INSTANCE.getCheckManager().getIdToName().get(log.getCheckId()) + "(VL: "
                                + log.getVl() + ") {" + log.getData() + "}");
                    });
                    if(logs.size() == 0) {
                        sender.sendMessage(Color.Gray + "There are no logs for player \"" + playername + "\"");
                    } else {
                        String url = null;
                        try {
                            url = Pastebin.makePaste(String.join("\n", logs), playername + "'s Logs",
                                    Pastebin.Privacy.UNLISTED);

                            sender.sendMessage(Color.Green + "Logs for " + playername + ": " + Color.White + url);
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        });
    }

    @Subcommand("logs web")
    @CommandPermission("kauri.command.logs")
    @Syntax("[player]")
    @CommandCompletion("@players")
    @Description("View the logs of a user")
    public void onLogsWeb(CommandSender sender, String[] args) {
        if(args.length == 0) {
            if(sender instanceof Player) {
                Player player = (Player) sender;
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


            StringBuilder url = new StringBuilder("https://funkemunky.cc/api/kauri?uuid=" + target.getUniqueId().toString().replaceAll("-", "") + (violations.keySet().size() > 0 ? "&violations=" : ""));

            if (violations.keySet().size() > 0) {
                for (String key : violations.keySet()) {
                    if (Anticheat.INSTANCE.getCheckManager().isCheck(key)) {
                        CheckSettings checkData = Anticheat.INSTANCE.getCheckManager().getCheckSettings(Anticheat.INSTANCE.getCheckManager().getCheckClasses()
                                .get(Anticheat.INSTANCE.getCheckManager().getIdToName().get(key)).getCheckClass().getParent());
                        int vl = violations.get(key), maxVL = checkData.getPunishVl();
                        boolean developer = false;

                        String toAppend = key + ":" + vl + ":" + maxVL + ":" + developer + ";";
                        toAppend = toAppend.replaceAll(" ", "%20");

                        url.append(toAppend);

                    }
                }

                if (violations.keySet().size() > 0) {
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
                    e.printStackTrace();
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
