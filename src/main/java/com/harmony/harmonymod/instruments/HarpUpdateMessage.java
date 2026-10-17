package com.harmony.harmonymod.instruments;

import com.harmony.harmonymod.sounds.Phrases;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/*
 * Client -> server: the phrases composed on the held harp's sheet music, and the active slot.
 * Sent as harp pitches (0-24) rather than phrase keys, and rebuilt on the server, so only real
 * phrases can be written onto the item.
 */
public class HarpUpdateMessage implements IMessage {
	public int active;
	// For each slot, its steps, each a chord of pitches
	public List<List<SortedSet<Integer>>> slots = new ArrayList<List<SortedSet<Integer>>>();

	public HarpUpdateMessage() {}

	public HarpUpdateMessage(List<List<SortedSet<Integer>>> slots, int active) {
		this.slots = slots;
		this.active = active;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeByte(active);
		buf.writeByte(slots.size());
		for (List<SortedSet<Integer>> steps : slots) {
			buf.writeByte(steps.size());
			for (SortedSet<Integer> step : steps) {
				buf.writeByte(step.size());
				for (int pitch : step) {
					buf.writeByte(pitch);
				}
			}
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		active = buf.readByte();
		int slotCount = buf.readByte();
		for (int s = 0; s < slotCount; s++) {
			List<SortedSet<Integer>> steps = new ArrayList<SortedSet<Integer>>();
			int stepCount = buf.readByte();
			for (int i = 0; i < stepCount; i++) {
				SortedSet<Integer> step = new TreeSet<Integer>();
				int noteCount = buf.readByte();
				for (int n = 0; n < noteCount; n++) {
					step.add((int) buf.readByte());
				}
				steps.add(step);
			}
			slots.add(steps);
		}
	}

	public static class Handler implements IMessageHandler<HarpUpdateMessage, IMessage> {
		@Override
		public IMessage onMessage(HarpUpdateMessage message, MessageContext ctx) {
			EntityPlayer player = ctx.getServerHandler().playerEntity;
			ItemStack held = player.getCurrentEquippedItem();
			if (held == null || !(held.getItem() instanceof ItemHarp)
					|| message.active < 0 || message.active >= ItemHarp.SLOTS || message.slots.size() != ItemHarp.SLOTS) {
				return null;
			}
			String[] phrases = new String[ItemHarp.SLOTS];
			for (int s = 0; s < ItemHarp.SLOTS; s++) {
				phrases[s] = toPhrase(message.slots.get(s), ((ItemHarp) held.getItem()).instrument);
			}
			ItemHarp.setSlots(held, phrases, message.active);
			return null;
		}

		/*
		 * Phrase key for harp pitches, dropping anything invalid
		 */
		private static String toPhrase(List<SortedSet<Integer>> pitchSteps, int instrument) {
			List<SortedSet<Integer>> steps = new ArrayList<SortedSet<Integer>>();
			for (SortedSet<Integer> pitches : pitchSteps) {
				SortedSet<Integer> step = new TreeSet<Integer>();
				for (int pitch : pitches) {
					if (pitch >= 0 && pitch <= 24) {
						step.add(Phrases.note(instrument, pitch));
					}
				}
				if (!step.isEmpty() && steps.size() < Phrases.MAX_STEPS) {
					steps.add(step);
				}
			}
			return Phrases.key(steps);
		}
	}
}
