package dev.brighten.ac.check;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.Color;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.MiscUtils;
import dev.brighten.ac.utils.timer.Timer;
import dev.brighten.ac.utils.timer.impl.MillisTimer;
import lombok.Getter;
import lombok.val;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class Check {

    private final APlayer player;

    private final CheckData checkData;
    private int vl;
    private long lastFlagRun;
    private Timer lastAlert = new MillisTimer();

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

    public void flag(boolean devAlerts, int resetVLTime, String information, Object... variables) {
        vl++;
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            if(Anticheat.INSTANCE.getTps() < 18)
                vl = 0;

            if(System.currentTimeMillis() - lastFlagRun < 50L) return;
            lastFlagRun = System.currentTimeMillis();

            final String finalInformation = String.format(information, variables);

            boolean dev = devAlerts || Anticheat.INSTANCE.getTps() < 18;
            final String info = finalInformation
                    .replace("%p", String.valueOf(player.getLagInfo().getTransPing()))
                    .replace("%t", String.valueOf(MathUtils.round(Anticheat.INSTANCE.getTps(), 2)));
            //if(vl > 0) Anticheat.INSTANCE.loggerManager.addLog(player, this, info);

            if (true) {
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

                player.getBukkitPlayer().spigot().sendMessage(toSend);
                lastAlert.reset();
            }
        });
    }

    private TextComponent createTxt(String txt) {
        return createTxt(txt, "");
    }
    private TextComponent createTxt(String txt, String info) {
        return new TextComponent(TextComponent.fromLegacyText(Color.translate(formatAlert(txt, info))));
    }
}
