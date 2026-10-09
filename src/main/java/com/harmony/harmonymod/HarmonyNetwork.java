package com.harmony.harmonymod;

import com.harmony.harmonymod.horsefix.MountCorrectionMessage;
import com.harmony.harmonymod.horsefix.MountMoveMessage;
import com.harmony.harmonymod.sync.AnimalSyncMessage;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/*
 * The mod's network channel. Every message type gets its own id, add new ones with the next free id.
 */
public class HarmonyNetwork {
	public static SimpleNetworkWrapper channel;

	public static void register() {
		channel = NetworkRegistry.INSTANCE.newSimpleChannel(HarmonyMod.MODID);
		channel.registerMessage(MountMoveMessage.Handler.class, MountMoveMessage.class, 0, Side.SERVER);
		channel.registerMessage(MountCorrectionMessage.Handler.class, MountCorrectionMessage.class, 1, Side.CLIENT);
		channel.registerMessage(AnimalSyncMessage.Handler.class, AnimalSyncMessage.class, 2, Side.CLIENT);
	}
}
