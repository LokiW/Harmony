package com.harmony.harmonymod.horsefix;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;

/*
 * Server -> client: the server disagreed with where the rider moved their horse, put it back here
 */
public class MountCorrectionMessage implements IMessage {
    public int entityId;
    public double x;
    public double y;
    public double z;

    public MountCorrectionMessage() {}

    public MountCorrectionMessage(Entity mount) {
        this.entityId = mount.getEntityId();
        this.x = mount.posX;
        this.y = mount.posY;
        this.z = mount.posZ;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
    }

    public static class Handler implements IMessageHandler<MountCorrectionMessage, IMessage> {
        @Override
        public IMessage onMessage(MountCorrectionMessage message, MessageContext ctx) {
            // Only ever received on the client, so HorseControlClient is never loaded on a server
            HorseControlClient.applyCorrection(message);
            return null;
        }
    }
}
