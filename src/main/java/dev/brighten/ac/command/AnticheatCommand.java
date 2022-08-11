package dev.brighten.ac.command;

import co.aikar.commands.*;
import co.aikar.commands.annotation.*;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.messages.Messages;
import dev.brighten.ac.utils.Color;
import dev.brighten.ac.utils.Init;
import dev.brighten.ac.utils.MiscUtils;
import dev.brighten.ac.utils.Priority;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.stream.Collectors;

@Init(priority = Priority.LOW)
@CommandAlias("anticheat|ac")
@CommandPermission("anticheat.command")
public class AnticheatCommand extends BaseCommand {

    public AnticheatCommand() {
        BukkitCommandCompletions cc = (BukkitCommandCompletions) Anticheat.INSTANCE.getCommandManager()
                .getCommandCompletions();

        cc.registerCompletion("checks", (c) -> Anticheat.INSTANCE.getCheckManager().getCheckClasses().stream()
                .map(cs -> cs.getCheckClass().getAnnotation(CheckData.class).name())
                .sorted(Comparator.naturalOrder()).collect(Collectors.toList()));

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

    @Subcommand("playerinfo|info|pi")
    @Description("Get player's information")
    @Syntax("[player]")
    @CommandCompletion("@players")
    @CommandPermission("anticheat.command.info")
    public void onCommand(CommandSender sender, @Single Player target) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            APlayer player = Anticheat.INSTANCE.getPlayerRegistry().getPlayer(target.getUniqueId()).orElse(null);

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
}
