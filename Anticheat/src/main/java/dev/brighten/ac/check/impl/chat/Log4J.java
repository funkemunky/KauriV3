package dev.brighten.ac.check.impl.chat;

import dev.brighten.ac.api.check.CheckType;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.WCancellable;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInChat;
import dev.brighten.ac.utils.annotation.Bind;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CheckData(name = "Log4J", checkId = "log4j", type = CheckType.CHAT)
public class Log4J extends Check {

    public Log4J(APlayer player) {
        super(player);
    }

    private static final Pattern pattern = Pattern.compile("\\$\\{.*}");

    @Bind
    WCancellable<WPacketPlayInChat> chatPacket = packet -> {
        Matcher matcher = pattern.matcher(packet.getMessage());
        if(matcher.matches()) {
            flag("Tried to use JNDI exploit");
            return true;
        }

        debug("Sent chat message: " + packet.getMessage());
        return false;
    };
}
