package com.harmony.harmonymod.respawn;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import java.util.ArrayList;
import java.util.List;

/*
 * Animals waiting to wake up at a respawn point that isn't loaded: {Dim, X, Y, Z, Entity}, where
 * Entity is the animal's saved data. Kept with the world's save data, shared by all dimensions.
 */
public class SpiritStore extends WorldSavedData {
	private static final String NAME = "harmony_spirits";

	public final List<NBTTagCompound> spirits = new ArrayList<NBTTagCompound>();

	public SpiritStore(String name) {
		super(name);
	}

	public static SpiritStore get() {
		World overworld = MinecraftServer.getServer().worldServerForDimension(0);
		SpiritStore store = (SpiritStore) overworld.mapStorage.loadData(SpiritStore.class, NAME);
		if (store == null) {
			store = new SpiritStore(NAME);
			overworld.mapStorage.setData(NAME, store);
		}
		return store;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		spirits.clear();
		NBTTagList list = tag.getTagList("Spirits", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < list.tagCount(); i++) {
			spirits.add(list.getCompoundTagAt(i));
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		NBTTagList list = new NBTTagList();
		for (NBTTagCompound spirit : spirits) {
			list.appendTag(spirit);
		}
		tag.setTag("Spirits", list);
	}
}
