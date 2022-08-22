package dev.brighten.ac.check.impl.world;

import dev.brighten.ac.check.Action;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.CheckType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInBlockPlace;
import dev.brighten.ac.utils.MathUtils;
import org.bukkit.util.Vector;

@CheckData(name = "Block (A)", type = CheckType.INTERACT)
public class BlockA extends Check {

    public BlockA(APlayer player) {
        super(player);
    }

    private int buffer;

    @Action
    public void onPlace(WPacketPlayInBlockPlace packet) {
        Vector dir = new Vector(packet.getDirection().getAdjacentX(), 0, packet.getDirection().getAdjacentZ()),
                opposite = new Vector(packet.getDirection().opposite().getAdjacentX(),
                        0, packet.getDirection().opposite().getAdjacentZ());

        if(!packet.getItemStack().getType().isBlock()) return;

        Vector delta = new Vector(player.getMovement().getDeltaX(), player.getMovement().getDeltaY(), player.getMovement().getDeltaZ());

        double dist = delta.distance(dir), dist2 = opposite.distance(MathUtils.getDirection(player.getMovement().getTo().getLoc()).setY(0));
        boolean check = dist <= 1 && dist > 0.7 && dist2 >= 0.5 && dist2 < 1;

        if(check && packet.getDirection().getAdjacentY() == 0 && player.getInfo().isSprinting()) {
            if((buffer+= 4) != 15) {
                flag("dist=%.3f dist2=%.3f placeVec=%s", dist, dist2, dir.toString());
                buffer = 14;
            }
        } else if(buffer > 0) buffer--;

        debug("dist=%.3f dist2=%.3f buffer=%s", dist, dist2, buffer);
    }
}
