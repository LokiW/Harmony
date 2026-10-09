package com.harmony.harmonymod.sync;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;

/*
 * Server -> client: an animal's Harmony data (traits, happiness, tricks)
 */
public class AnimalSyncMessage implements IMessage {
	public int entityId;
	public NBTTagCompound data;

	public AnimalSyncMessage() {}

	public AnimalSyncMessage(Entity animal, NBTTagCompound data) {
		this.entityId = animal.getEntityId();
		this.data = data;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.entityId = buf.readInt();
		this.data = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.entityId);
		ByteBufUtils.writeTag(buf, this.data);
	}

	public static class Handler implements IMessageHandler<AnimalSyncMessage, IMessage> {
		@Override
		public IMessage onMessage(AnimalSyncMessage message, MessageContext ctx) {
			// Only ever received on the client, so AnimalSyncClient is never loaded on a server
			AnimalSyncClient.apply(message);
			return null;
		}
	}
}
