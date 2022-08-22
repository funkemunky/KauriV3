package dev.brighten.ac.check;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.*;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;
import lombok.Getter;
import lombok.val;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public abstract class Check {

    public final APlayer player;
    @Getter
    private final CheckData checkData;
    @Getter
    private int vl;
    private long lastFlagRun;
    private final Timer lastAlert = new MillisTimer();

    public static Set<UUID> alertsEnabled = new HashSet<>();

    public static final Map<String, List<Tuple<UUID, UUID>>> debugInstances = new HashMap<>();

    public Check(APlayer player) {
        this.player = player;
        this.checkData = getClass().getAnnotation(CheckData.class);
    }

    private String formatAlert(String toFormat, String info) {
        return addPlaceHolders(Color.translate(toFormat.replace("%desc%", String.join("\n",
                        MiscUtils
                                .splitIntoLine("", 20))))
                .replace("%info%", info));
    }

    private String addPlaceHolders(String string) {
        return string.replace("%player%", player.getBukkitPlayer().getName())
                .replace("%check%", checkData.name())
                .replace("%name%",  player.getBukkitPlayer().getName())
                .replace("%vl%", String.valueOf(MathUtils.round(vl, 1)));
    }

    public void cancel() {
        if(checkData.type() == CheckType.COMBAT) {
            player.hitsToCancel++;
        } else {
            player.getInfo().getLastCancel().reset();

            KLocation fromLoc = player.getInfo().getLastKnownGoodPosition() != null
                    ? player.getInfo().getLastKnownGoodPosition() : player.getMovement().getFrom().getLoc();

            player.getBukkitPlayer().teleport(fromLoc.toLocation(player.getBukkitPlayer().getWorld()));
        }
    }

    public void debug(String information, Object... variables) {
        if(!Anticheat.allowDebug) return;

        if(debugInstances.containsKey(checkData.name())) {
            val list = debugInstances.get(checkData.name());

            for (Tuple<UUID, UUID> tuple : list) {
                UUID toDebug = tuple.one;

                if(!toDebug.equals(player.getUuid())) continue;

                ComponentBuilder builder = new ComponentBuilder("[DEBUG] ").color(ChatColor.RED);

                BaseComponent[] message =
                        builder.append(String.format(information, variables)).color(ChatColor.GRAY).create();

                Anticheat.INSTANCE.getPlayerRegistry().getPlayer(tuple.two)
                        .ifPresent(player -> player.getBukkitPlayer().spigot().sendMessage(message));
            }
        }
    }

    public void flag(String information, Object... variables) {
        vl++;
        if(System.currentTimeMillis() - lastFlagRun < 50L) return;

        Anticheat.INSTANCE.getScheduler().execute(() -> {
            if(Anticheat.INSTANCE.getTps() < 18)
                vl = 0;
            lastFlagRun = System.currentTimeMillis();

            final String finalInformation = String.format(information, variables);

            boolean dev = Anticheat.INSTANCE.getTps() < 18;
            final String info = finalInformation
                    .replace("%p", String.valueOf(player.getLagInfo().getTransPing()))
                    .replace("%t", String.valueOf(MathUtils.round(Anticheat.INSTANCE.getTps(), 2)));
            //if(vl > 0) Anticheat.INSTANCE.loggerManager.addLog(player, this, info);

            //Sending Discord webhook alert

            List<TextComponent> components = new ArrayList<>();

            if(dev) {
                components.add(new TextComponent(createTxt("&8[&cDev&8] ")));
            }
            val text = createTxt("&8[&4!&8] &f%player% &7flagged &f%check%" +
                    " &8(&ex%vl%&8)", info);

            text.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {
                    createTxt("&eDescription&8: &f%desc%" +
                            "\n&eInfo: &f%info%\n&r\n&7&oClick to teleport to player.", info)}));

            components.add(text);

            TextComponent[] toSend = components.toArray(new TextComponent[0]);

            for (UUID uuid : alertsEnabled) {
                Anticheat.INSTANCE.getPlayerRegistry().getPlayer(uuid)
                        .ifPresent(apl -> apl.getBukkitPlayer().spigot().sendMessage(toSend));
            }
            lastAlert.reset();
        });
    }

    private TextComponent createTxt(String txt) {
        return createTxt(txt, "");
    }
    private TextComponent createTxt(String txt, String info) {
        return new TextComponent(TextComponent.fromLegacyText(Color.translate(formatAlert(txt, info))));
    }
}
