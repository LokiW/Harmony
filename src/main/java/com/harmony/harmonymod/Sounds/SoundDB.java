package com.harmony.harmonymod.sounds;

import com.harmony.harmonymod.HarmonyProps;
import net.minecraftforge.event.world.*;
import net.minecraft.entity.*;
import net.minecraft.world.World;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.common.util.*;
import cpw.mods.fml.common.eventhandler.*;
import java.util.*;

public class SoundDB {

	private static final int NOTE_COUNT = 24;
	
	private static final double MAX_NOTE_DISTANCE = 16.0;

	/*
	 * Proviedes access to SoundDB, and does first time initialization
	 */
	private static SoundDB db;
	public static SoundDB getSoundDB() {
		if(db != null)
			return db;

		db = new SoundDB();
		// Register to receive noteblock events
		MinecraftForge.EVENT_BUS.register(db);

		return db;
	}

	// Other's can't instantiate
	private SoundDB() {}

	/*
	 * Record all noteblock events
	 */
	@SubscribeEvent
	public void NoteBlockEvent(NoteBlockEvent.Play e) {
		World w = e.world;
		long tick = w.getWorldTime();
		int note = getSound(e.instrument, e.getVanillaNoteId());

		// Find all entities that could hear this
		AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
				e.x - MAX_NOTE_DISTANCE,
				e.y - MAX_NOTE_DISTANCE,
				e.z - MAX_NOTE_DISTANCE,
				e.x + MAX_NOTE_DISTANCE,
				e.y + MAX_NOTE_DISTANCE,
				e.z + MAX_NOTE_DISTANCE);

		List<EntityLiving> entities = w.getEntitiesWithinAABB(EntityLiving.class, aabb);
		for(EntityLiving ent : entities) {
			HarmonyProps hp = HarmonyProps.get(ent);
			if(hp != null && hp.tricks != null) {
				hp.tricks.updateCurrentTrick(note);
			}
		}
	}

	/*
	 * API for entities to make animal calls
	 */
	public void registerCall(Entity source, Entity target) {
		long tick = source.worldObj.getWorldTime();
		//TODO implement
	}

	public int getSound(NoteBlockEvent.Instrument instr, int noteId) {
		return noteId | (instr.ordinal() << 8);
	}
}
