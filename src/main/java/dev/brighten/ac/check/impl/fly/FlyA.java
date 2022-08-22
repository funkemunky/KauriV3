package dev.brighten.ac.check.impl.fly;

import dev.brighten.ac.check.Action;
import dev.brighten.ac.check.Check;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.check.CheckType;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.packet.ProtocolVersion;
import dev.brighten.ac.packet.wrapper.in.WPacketPlayInFlying;
import dev.brighten.ac.utils.MathUtils;
import dev.brighten.ac.utils.MovementUtils;

@CheckData(name = "Fly (A)", type = CheckType.MOVEMENT)
public class FlyA extends Check {

    public FlyA(APlayer player) {
        super(player);
    }

    private float buffer;

    //our predicted value using client movement maths.
    private double predictedValue = 0.0;
    //how much will the buffer get added
    private double bufferAdd;

    @Action
    public void onFlying(WPacketPlayInFlying packet) {
        if (!packet.isMoved() || (player.getMovement().getDeltaXZ() == 0
                && player.getMovement().getDeltaY() == 0) || !player.getMovement().getTo().isOnGround())
            return;

        final double delta = player.getMovement().getDeltaY();

        //using the last y axis movement we got from the player, to estimate his next movement
        predictedValue = player.getMovement().getLDeltaY();

        //if predicted < this, player motion y is set to 0 in mcp, so we'll do same.
        final double min = player.getPlayerVersion().isBelow(ProtocolVersion.V1_9) ? 0.005 : 0.003;

        //we can be sure here that he jumped
        if (player.getInfo().getClientAirTicks() == 1) {
            //so we're handling his jump
            predictedValue = MovementUtils.getJumpHeight(player);
            bufferAdd = 2;
        } else {
            //this is how minecraft makes player falls, a fly wouldn't respect this rule

            //gravity
            predictedValue -= 0.08;
            //air drag
            predictedValue *= 0.98F;

            //sometimes, player doesn't send a position packet, its called "0.03", because under a 0.03 xyz length move
            //player doesn't send a position packet
            if (Math.abs(predictedValue - delta) > 1.0E-6) {
                final double mightFixedPrediction = (predictedValue - 0.08) * 0.98F;
                if (Math.abs(mightFixedPrediction - delta) < 1.0E-6)
                    predictedValue = mightFixedPrediction;
            }

            bufferAdd = 1;
        }

        if(Math.abs(predictedValue) < min)
            predictedValue = 0;

        //basically the difference from what we expect the player to move, and what he actually moved.
        final double offset = MathUtils.getDelta(predictedValue, delta);

        if (!player.getInfo().isGeneralCancel()
                && player.getInfo().getBlockAbove().isPassed(1)
                && !player.getInfo().isOnLadder()
                && !player.getBlockInfo().inWeb
                && !player.getBlockInfo().inScaffolding
                && !player.getBlockInfo().inLiquid
                && !player.getBlockInfo().fenceBelow
                && !player.getBlockInfo().onHalfBlock
                && player.getInfo().getVelocity().isPassed(1)
                && !player.getBlockInfo().onSlime && offset > 1.0E-5) {
            if ((buffer += bufferAdd) > 5) {
                buffer = 5;
                flag("dY=%.3f p=%.3f dx=%.3f", player.getMovement().getDeltaY(), predictedValue,
                        player.getMovement().getDeltaXZ());
            }
        } else buffer -= buffer > 0 ? 0.25f : 0;

        debug("dY=%.3f p=%.3f dx=%.3f b=%s velocity=%s", player.getMovement().getDeltaY(), predictedValue,
                player.getMovement().getDeltaXZ(), buffer, player.getInfo().getVelocity().getPassed());


    }
}