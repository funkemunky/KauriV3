package dev.brighten.ac.command;

import co.aikar.commands.*;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.logging.Log;
import dev.brighten.ac.messages.Messages;
import dev.brighten.ac.packet.handler.HandlerAbstract;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.annotation.Init;
import dev.brighten.ac.utils.msg.ChatBuilder;
import io.netty.buffer.Unpooled;
import lombok.val;
import net.minecraft.server.v1_8_R3.PacketDataSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutCustomPayload;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftChatMessage;
import org.bukkit.entity.Player;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Init(priority = Priority.LOW)
@CommandAlias("anticheat|ac")
@CommandPermission("anticheat.command")
public class AnticheatCommand extends BaseCommand {

    public AnticheatCommand() {
        BukkitCommandCompletions cc = (BukkitCommandCompletions) Anticheat.INSTANCE.getCommandManager()
                .getCommandCompletions();

        cc.registerCompletion("checks", (c) -> Anticheat.INSTANCE.getCheckManager().getCheckClasses().keySet()
                .stream()  .sorted(Comparator.naturalOrder())
                .map(name -> name.replace(" ", "_")).collect(Collectors.toList()));

        BukkitCommandContexts contexts = (BukkitCommandContexts) Anticheat.INSTANCE.getCommandManager()
                .getCommandContexts();

        contexts.registerOptionalContext(Integer.class, c -> {
            String arg = c.popFirstArg();

            if(arg == null) return null;
            try {
                return Integer.parseInt(arg);
            } catch(NumberFormatException e) {
                throw new InvalidCommandArgument(String.format(Color.Red
                        + "Argument \"%s\" is not an integer", arg));
            }
        });

        contexts.registerOptionalContext(APlayer.class, c -> {
            if(c.hasFlag("other")) {
                String arg = c.popFirstArg();

                Player onlinePlayer = Bukkit.getPlayer(arg);

                if(onlinePlayer != null) {
                    return Anticheat.INSTANCE.getPlayerRegistry().getPlayer(onlinePlayer.getUniqueId())
                            .orElse(null);
                } else return null;
            } else {
                CommandSender sender = c.getSender();
                
                if(sender instanceof Player) {
                    return Anticheat.INSTANCE.getPlayerRegistry().getPlayer(((Player) sender).getUniqueId())
                            .orElse(null);
                }
                else if(!c.isOptional()) throw new InvalidCommandArgument(MessageKeys.NOT_ALLOWED_ON_CONSOLE,
                        false, new String[0]);
                else return null;
            }
        });
    }

    @HelpCommand
    @Syntax("")
    @Description("View the help page")
    public void onHelp(CommandSender sender, CommandHelp help) {
        sender.sendMessage(MiscUtils.line(Color.Dark_Gray));
        help.showHelp();
        sender.sendMessage(MiscUtils.line(Color.Dark_Gray));
    }

    @Subcommand("alerts")
    @HelpCommand
    @CommandPermission("anticheat.command.alerts")
    @Description("Toggle anticheat alerts")
    public void onAlerts(Player pl) {
        APlayer player = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(pl.getUniqueId()).orElse(null);

        if(player == null) {
            pl.spigot().sendMessage(Messages.NULL_APLAYER);
            return;
        }

        if(Check.alertsEnabled.contains(player.getBukkitPlayer().getUniqueId())) {
            Check.alertsEnabled.remove(player.getBukkitPlayer().getUniqueId());
            pl.spigot().sendMessage(Messages.ALERTS_OFF);
        } else {
            Check.alertsEnabled.add(player.getBukkitPlayer().getUniqueId());
            pl.spigot().sendMessage(Messages.ALERTS_ON);
        }
    }

    @Subcommand("logs")
    @Syntax("[player]")
    @CommandCompletion("@players")
    @CommandPermission("anticheat.command.logs")
    @Description("Get player logs")
    public void onLogs(CommandSender sender, @Single String playername) {
        UUID uuid = Bukkit.getOfflinePlayer(playername).getUniqueId();

        sender.sendMessage(Color.Red + "Getting logs for " + playername + "...");

        Anticheat.INSTANCE.getScheduler().execute(() -> {
            List<String> logs = new ArrayList<>();
            System.out.println("shit 1");
            Anticheat.INSTANCE.getLogManager().runQuery("select * from logs where uuid=" + uuid.hashCode(), rs -> {
                Log log = Log.builder()
                        .uuid(UUID.fromString(rs.getString("uuid")))
                        .checkId(rs.getString("check"))
                        .data(rs.getString("data"))
                        .vl(rs.getFloat("vl"))
                        .time(rs.getLong("time"))
                        .build();

                System.out.println("Shit");

                logs.add("Flagged " + Anticheat.INSTANCE.getCheckManager().getIdToName().get(log.getCheckId()) + " data: " + log.getData() + " VL: " + log.getVl() + " at " + log.getTime());
            });
            String url = null;
            try {
                url = Pastebin.makePaste(String.join("\n", logs), playername + "'s Logs", Pastebin.Privacy.UNLISTED);

                sender.sendMessage(Color.Green + "Logs for " + playername + ": " + Color.White + url);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Subcommand("title")
    @Private
    public void onTitle(CommandSender sender, OnlinePlayer target, String title) {
        PacketPlayOutTitle packetSubtitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, CraftChatMessage.fromString(Color.translate(title))[0]);
        HandlerAbstract.getHandler().sendPacket(target.getPlayer(), packetSubtitle);
        sender.sendMessage(Color.Green + "Sent title!");
    }

    @Subcommand("playerinfo|info|pi")
    @Description("Get player's information")
    @Syntax("[player]")
    @CommandCompletion("@players")
    @CommandPermission("anticheat.command.info")
    public void onCommand(CommandSender sender, @Single APlayer player) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {

            if(player == null) {
                sender.spigot().sendMessage(Messages.NULL_APLAYER);
                return;
            }

            sender.sendMessage(MiscUtils.line(Color.Dark_Gray));
            sender.sendMessage(Color.translate("&6&lPing&8: &f" + player.getLagInfo().getTransPing() * 50 + "ms"));
            sender.sendMessage(Color.translate("&6&lVersion&8: &f" + player.getPlayerVersion().name()));
            sender.sendMessage(Color.translate("&6&lSensitivity&8: &f" + player.getMovement().getSensXPercent() + "%"));
            sender.sendMessage(MiscUtils.line(Color.Dark_Gray));
        });
    }

    @Subcommand("runtest")
    public void onCommand(Player player) {
        long start = System.currentTimeMillis();
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer());
        serializer.writeLong(start);
        PacketPlayOutCustomPayload payload = new PacketPlayOutCustomPayload("Time|Send", serializer);

        HandlerAbstract.getHandler().sendPacket(player, payload);
        Anticheat.INSTANCE.getPlayerRegistry().getPlayer(player.getUniqueId()).ifPresent(aplayer -> {
            aplayer.runInstantAction(ka -> {
                if(!ka.isEnd()) {
                    long transDelta = System.currentTimeMillis() - start;

                    player.sendMessage("Transaction delta: " + transDelta + "ms");
                }
            });
        });
    }

    @Subcommand("debug")
    @CommandCompletion("@checks|none @players")
    @Description("Debug a player")
    @Syntax("[check] [player]")
    @CommandPermission("anticheat.command.debug")
    public void onDebug(Player sender, @Single String check, @Optional OnlinePlayer targetPlayer) {
        Player target = targetPlayer != null ? targetPlayer.player : sender;
        switch (check.toLowerCase()) {
            case "none": {
                synchronized (Check.debugInstances) {
                    Check.debugInstances.forEach((nameKey, list) -> {
                        val iterator = list.iterator();
                        while(iterator.hasNext()) {
                            val tuple = iterator.next();

                            if(tuple.two.equals(target.getUniqueId())) {
                                iterator.remove();
                                sender.spigot()
                                        .sendMessage(new ChatBuilder(
                                                "&cTurned off debug for check &f%s &con target &f%s", nameKey,
                                                target.getName()).build());
                            }
                        }
                    });
                }
                break;
            }
            case "sniff": {
                APlayer targetData = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(target.getUniqueId()).orElse(null);

                if(targetData != null) {
                    if(targetData.sniffing) {
                        targetData.sniffing = false;
                        sender.sendMessage(Color.Red + "Stopped sniff. Pasting...");
                        try {
                            sender.sendMessage(Color.Gray + "Paste: " + Color.White + Pastebin.makePaste(
                                    String.join("\n", targetData.sniffedPackets.toArray(new String[0])),
                                    "Sniffed from " + target.getName(), Pastebin.Privacy.UNLISTED));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        targetData.sniffedPackets.clear();
                    } else {
                        targetData.sniffing = true;
                        sender.sendMessage(Color.Green + "Started packet sniff on " + target.getName() + "!");
                    }
                } else {
                    sender.spigot().sendMessage(Messages.NULL_APLAYER);
                }
                break;
            }
            default: {
                if(!Anticheat.INSTANCE.getCheckManager().isCheck(check)) {
                    sender.sendMessage(Color.Red + "Check \"" + check + "\" is not a valid check!");
                    return;
                }
                synchronized (Check.debugInstances) {
                    Check.debugInstances.compute(check.replace("_", " "), (key, list) -> {
                        if(list == null) list = new ArrayList<>();

                        list.add(new Tuple<>(target.getUniqueId(), sender.getUniqueId()));

                        return list;
                    });

                    sender.spigot()
                            .sendMessage(new ChatBuilder(
                                    "&aTurned on debug for check &f%s &aon target &f%s",
                                    check.replace("_", " "),
                                    target.getName()).build());
                }
                break;
            }
        }
    }
}
