package dev.brighten.ac.check;

import com.viaversion.viaversion.libs.mcstructs.text.events.hover.HoverEvent;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.api.AnticheatAPI;
import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.api.check.ECheck;
import dev.brighten.ac.api.event.AnticheatEvent;
import dev.brighten.ac.api.event.result.CancelResult;
import dev.brighten.ac.api.event.result.FlagResult;
import dev.brighten.ac.api.event.result.PunishResult;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.*;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.*;
import java.util.stream.Collectors;

public class Check implements ECheck {

    public final APlayer player;
    @Getter
    private final CheckData checkData;
    @Getter
    public float vl;
    @Getter
    @Setter
    private boolean enabled, punishable, cancellable;
    @Getter
    @Setter
    private int punishVl;

    public static Set<UUID> alertsEnabled = new HashSet<>();

    public static final Map<String, List<Tuple<UUID, UUID>>> debugInstances = new HashMap<>();

    public Check(APlayer player) {
        this.player = player;
        this.checkData = getClass().getAnnotation(CheckData.class);
    }

    @Override
    public String getName() {
        return checkData.name();
    }

    @Override
    public CheckType getCheckType() {
        return checkData.type();
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
                .replace("%p", String.valueOf(player.getLagInfo().getTransPing()))
                .replace("%t", String.valueOf(MathUtils.round(Anticheat.INSTANCE.getTps(), 2)))
                .replace("%vl%", String.valueOf(MathUtils.round(vl, 1)));
    }

    public void cancel() {
        if(!cancellable) return;

        CancelResult result = CancelResult.builder().cancelled(false).build();

        for (AnticheatEvent event : AnticheatAPI.INSTANCE.getAllEvents()) {
            result = event.onCancel(player.getBukkitPlayer(), this, result.isCancelled());
        }

        if(result.isCancelled()) return;

        if(checkData.type() == CheckType.COMBAT) {
            player.hitsToCancel++;
        } else {
            player.getInfo().getLastCancel().reset();

            final KLocation ground = player.getInfo().isServerGround()
                    ? player.getMovement().getFrom().getLoc()
                    : MovementUtils.findGroundLocation(player, player.getMovement().getFrom().getLoc(), 10);

            Anticheat.INSTANCE.getRunUtils().task(() -> player.getBukkitPlayer().teleport(ground));
        }
    }

    public void correctMovement(KLocation toLoc) {
        if(!isCancellable()) return;

        CancelResult result = CancelResult.builder().cancelled(false).build();

        for (AnticheatEvent event : AnticheatAPI.INSTANCE.getAllEvents()) {
            result = event.onCancel(player.getBukkitPlayer(), this, result.isCancelled());
        }

        if(result.isCancelled()) return;

        player.getInfo().getLastCancel().reset();

        final Location CORRECTED = toLoc;

        Anticheat.INSTANCE.getRunUtils().task(() -> player.getBukkitPlayer().teleport(CORRECTED));
    }

    public void debug(String information, Object... variables) {
        if(!Anticheat.allowDebug) return;

        val list = debugInstances.get(checkData.name());

        if(list != null) {
            for (Tuple<UUID, UUID> tuple : list) {
                UUID toDebug = tuple.one;

                if(!toDebug.equals(player.getUuid())) continue;

                ComponentBuilder builder = new ComponentBuilder("[ " + getName() + ": " + player.getBukkitPlayer().getName() + "] ")
                        .color(NamedTextColor.RED);

                BaseComponent[] message =
                        builder.append(String.format(information, variables)).color(NamedTextColor.GRAY).create();

                Anticheat.INSTANCE.getPlayerRegistry().getPlayer(tuple.two)
                        .ifPresent(player -> player.getBukkitPlayer().spigot().sendMessage(message));
            }
        }
    }

    public <T extends Check> Optional<T> find(Class<T> checkClass) {
        Check check = player.getCheckHandler().findCheck(checkClass);

        if(check != null) {
            return Optional.of(checkClass.cast(check));
        }

        return Optional.empty();
    }

    static final Component devComponents, components;
    static {
        Component textComp = Component.text("[").color(NamedTextColor.DARK_GRAY)
                .append(Component.text("!").color(NamedTextColor.DARK_RED))
                .append(Component.text("]").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(" %player%").color(NamedTextColor.WHITE))
                .append(Component.text(" flagged").color(NamedTextColor.GRAY))
                .append(Component.text(" %check%").color(NamedTextColor.WHITE))
                .append(Component.text(" (").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("x%vl%").color(NamedTextColor.YELLOW))
                .append(Component.text(")").color(NamedTextColor.DARK_GRAY));

        devComponents = Component.text("[").color(NamedTextColor.DARK_GRAY)
                .append(Component.text("Dev").color(NamedTextColor.RED))
                .append(Component.text("] ").color(NamedTextColor.DARK_GRAY)).append(textComp);

        components = textComp;
    }

    public void flag(String information, Object... variables) {
        flag(true, information, variables);
    }

    public void flag(boolean punish, String information, Object... variables) {
        vl++;

       Anticheat.INSTANCE.getScheduler().execute(() -> {
           if(Anticheat.INSTANCE.getTps() < 18)
               vl = 0;

           final String info = String.format(information, variables);

           FlagResult currentResult = FlagResult.builder().cancelled(false).build();

           for (AnticheatEvent event : AnticheatAPI.INSTANCE.getAllEvents()) {
               currentResult = event.onFlag(player.getBukkitPlayer(), this, info,
                       currentResult.isCancelled());
           }

           Anticheat.INSTANCE.getLogManager()
                   .insertLog(player, checkData, vl, System.currentTimeMillis(), info);

           if(player.getCheckHandler().getAlertCountReset().isPassed(20)) {
               player.getCheckHandler().getAlertCount().set(0);
               player.getCheckHandler().getAlertCountReset().reset();
           }

           if(player.getCheckHandler().getAlertCount().incrementAndGet() < 80) {
               //Sending Discord webhook alert

               for (TextComponent tc : components) {
                   TextComponent ntc = new TextComponent(tc);
                   ntc.setText(formatAlert(tc.getText(), info));

                   ntc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Description:")
                           .color(NamedTextColor.YELLOW)
                           .append(formatAlert(" %desc%\n", info)).color(NamedTextColor.WHITE).append("Info:")
                           .color(NamedTextColor.YELLOW)
                           .append(formatAlert(" %info%\n", info)).color(NamedTextColor.WHITE)
                           .append("\n").append("Click to teleport to player")
                           .create()));
                   ntc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                           addPlaceHolders(CheckConfig.clickCommand)));

                   toSend.add(ntc);
               }

               for (UUID uuid : alertsEnabled) {
                   Anticheat.INSTANCE.getPlayerRegistry().getPlayer(uuid)
                           .ifPresent(apl -> apl.getBukkitPlayer().spigot().sendMessage(toSend
                                   .toArray(new BaseComponent[0])));
               }
               player.getCheckHandler().getAlertCountReset().reset();
           }
           if(punish && vl >= punishVl) {
               punish();
           }
       });
    }

    public void punish() {
        if (!punishable || player.getCheckHandler().getLastPunish().isNotPassed(20)) return;

        player.getCheckHandler().getLastPunish().reset();

        List<String> commands = CheckConfig.punishmentCommands.stream().map(this::addPlaceHolders)
                .collect(Collectors.toList());

        PunishResult result = PunishResult.builder().cancelled(false).commands(commands).build();

        for (AnticheatEvent event : AnticheatAPI.INSTANCE.getAllEvents()) {
            result = event.onPunish(player.getBukkitPlayer(), this, commands, result.isCancelled());
        }
        PunishResult finalResult = result;
        if (finalResult != null && finalResult.getCommands() != null && !finalResult.isCancelled()) {
            Anticheat.INSTANCE.getRunUtils().task(() -> {
                for (String punishmentCommand : finalResult.getCommands()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), punishmentCommand);
                }
            });
        }
    }
}
