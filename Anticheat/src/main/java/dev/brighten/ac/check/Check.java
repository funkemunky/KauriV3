package dev.brighten.ac.check;

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
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;

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
        CancelResult result = CancelResult.builder().cancelled(false).build();

        for (AnticheatEvent event : AnticheatAPI.INSTANCE.getAllEvents()) {
            result = event.onCancel(player.getBukkitPlayer(), this, result.isCancelled());
        }

        if(result.isCancelled()) return;

        if(checkData.type() == CheckType.COMBAT) {
            player.hitsToCancel++;
        } else {
            player.getInfo().getLastCancel().reset();

            final Location ground = player.getInfo().isServerGround()
                    ? player.getMovement().getFrom().getLoc()
                    .toLocation(player.getBukkitPlayer().getWorld())
                    : MovementUtils.findGroundLocation(player.getMovement().getFrom().getLoc()
                    .toLocation(player.getBukkitPlayer().getWorld()), 10);

            Anticheat.INSTANCE.getRunUtils().task(() -> player.getBukkitPlayer().teleport(ground));
        }
    }

    public void correctMovement(KLocation toLoc) {
        CancelResult result = CancelResult.builder().cancelled(false).build();

        for (AnticheatEvent event : AnticheatAPI.INSTANCE.getAllEvents()) {
            result = event.onCancel(player.getBukkitPlayer(), this, result.isCancelled());
        }

        if(result.isCancelled()) return;

        player.getInfo().getLastCancel().reset();

        final Location CORRECTED = toLoc.toLocation(player.getBukkitPlayer().getWorld());

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
                        .color(ChatColor.RED);

                BaseComponent[] message =
                        builder.append(String.format(information, variables)).color(ChatColor.GRAY).create();

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

    static final TextComponent[] devComponents, components;
    static {
        List<TextComponent> devList = new ArrayList<>(), flagList = new ArrayList<>();
        for (BaseComponent dev : new ComponentBuilder("[").color(ChatColor.DARK_GRAY).append("Dev")
                .color(ChatColor.RED).append("]").color(ChatColor.DARK_GRAY).create()) {
            devList.add((TextComponent)dev);
        }

        BaseComponent[] textComp = new ComponentBuilder("[").color(ChatColor.DARK_GRAY).append("!")
                .color(ChatColor.DARK_RED).append("]").color(ChatColor.DARK_GRAY).append(" %player%")
                .color(ChatColor.WHITE).append(" flagged").color(ChatColor.GRAY).append(" %check%")
                .color(ChatColor.WHITE).append(" (").color(ChatColor.DARK_GRAY).append("x%vl%")
                .color(ChatColor.YELLOW).append(")").color(ChatColor.DARK_GRAY).create();

        for (BaseComponent bc : textComp) {
            devList.add(new TextComponent((TextComponent)bc));
            flagList.add(new TextComponent((TextComponent)bc));
        }

        devComponents = devList.toArray(new TextComponent[0]);
        components = flagList.toArray(new TextComponent[0]);
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

               List<BaseComponent> toSend = new ArrayList<>();

               for (TextComponent tc : components) {
                   TextComponent ntc = new TextComponent(tc);
                   ntc.setText(formatAlert(tc.getText(), info));

                   ntc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Description:")
                           .color(ChatColor.YELLOW)
                           .append(formatAlert(" %desc%\n", info)).color(ChatColor.WHITE).append("Info:")
                           .color(ChatColor.YELLOW)
                           .append(formatAlert(" %info%\n", info)).color(ChatColor.WHITE)
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
