package com.harmony.harmonymod.horsefix;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;

/*
 * Client -> server: where the rider's client moved their horse this tick
 */
public class MountMoveMessage implements IMessage {
    public int entityId;
    public double x;
    public double y;
    public double z;
    public boolean onGround;

    public MountMoveMessage() {}

    public MountMoveMessage(Entity mount) {
        this.entityId = mount.getEntityId();
        this.x = mount.posX;
        this.y = mount.posY;
        this.z = mount.posZ;
        this.onGround = mount.onGround;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.onGround = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeBoolean(this.onGround);
    }

    public static class Handler implements IMessageHandler<MountMoveMessage, IMessage> {
        @Override
        public IMessage onMessage(MountMoveMessage message, MessageContext ctx) {
            HorseControl.handleMove(ctx.getServerHandler().playerEntity, message);
            return null;
        }
    }
}
