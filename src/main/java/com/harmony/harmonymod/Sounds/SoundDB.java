package com.harmony.harmonymod.sounds;

import net.minecraftforge.event.world.*;
import net.minecraft.entity.*;
import net.minecraftforge.common.*;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.common.util.*;
import cpw.mods.fml.common.eventhandler.*;
import java.util.*;

public class SoundDB {

	private static final int NOTE_COUNT = 24;
	
	private static final int MAX_TICK_DELAY = 1;
	private static final int MAX_NOTES = 1;

	private static final double MAX_NOTE_DISTANCE = 16.0;

	private InstrumentSound[][] soundInterns;

	//circular buffer of recently heard sounds
	private Sound[] data;
	private int cur;

	/*
	 * Proviedes access to SoundDB, and does first time initialization
	 */
	private static SoundDB db;
	public static SoundDB getSoundDB() {
		if(db != null)
			return db;

		// Initialize circular buffer
		db = new SoundDB();
		db.data = new Sound[32];
		for(int i = 0; i < db.data.length; i++) {
			db.data[i] = new Sound(null, 0, 0.0, 0.0, 0.0);
		}
		db.cur = 0;

		// Initialize all the sound interns
		int instrumentCount = NoteBlockEvent.Instrument.values().length;
		db.soundInterns = new InstrumentSound[instrumentCount][];
		for(int i = 0; i < instrumentCount; i++) {
			db.soundInterns[i] = new InstrumentSound[NOTE_COUNT];
			for(int j = 0; j < NOTE_COUNT; j++) {
				db.soundInterns[i][j] = new InstrumentSound();
			}
		}

		// Register to receive noteblock events
		MinecraftForge.EVENT_BUS.register(db);

		return db;
	}

	// Other's can't instantiate
	private SoundDB() {}

	/*
	 * Get the last thing e would have heard
	 */
	public Object getLastHeard(Entity e) {
		// For each note in db reverse chronological order
		long tick = e.worldObj.getWorldTime();
		int i = cur - 1;
		i = i < 0? i = data.length - 1 : i;
		while(i != cur) {
			Sound s = data[i];
			// Have hit notes that are too old
			if (s.timestamp < tick - MAX_NOTES*MAX_TICK_DELAY)
				return null;

			double distSq = e.getDistanceSq(s.x, s.y, s.z);

			// If close enough to be heard
			if(distSq < MAX_NOTE_DISTANCE*MAX_NOTE_DISTANCE) {
				return data[i].message;
			}

			i--;
			i = i < 0 ? data.length - 1 : i;
		}

		return null;
	}

	/*
	 * Record all noteblock events
	 */
	@SubscribeEvent
	public void NoteBlockEvent(NoteBlockEvent.Play e) {
		long tick = e.world.getWorldTime();
		checkBackingArray(tick);

		Object message = getSound(e.instrument, e.getNote()); 
		data[cur].reset(message, tick, (double)e.x, (double)e.y, (double)e.z);
		cur++;
		cur %= data.length;
	}

	public InstrumentSound getSound(NoteBlockEvent.Instrument instrument, NoteBlockEvent.Note note) {
		return soundInterns[instrument.ordinal()][note.ordinal()];
	}

	/*
	 * API for entities to make animal calls
	 */
	public void registerCall(Entity source, Entity target) {
		long tick = source.worldObj.getWorldTime();
		checkBackingArray(tick);

		Object message = target;
		data[cur].reset(message, tick, source.posX, source.posY, source.posZ);
		cur++;
		cur %= data.length;
	}

	/*
	 * Grow the backing array if we run out of space
	 */
	private void checkBackingArray(long tick) {
		if(data[cur].timestamp < tick - MAX_NOTES*MAX_TICK_DELAY) {
			return;
		}

		System.out.println("HarmonyMod: Growing SoundDB backing array. data[cur].timestamp " + data[cur].timestamp + " tick " + tick + " MAX_NOTES " + MAX_NOTES + " MAX_TICK_DELAY " + MAX_TICK_DELAY);
		int newSize = data.length * 2;
		Sound[] newData = new Sound[newSize];

		for(int i = 0; i < cur; i++) {
			newData[i] = data[i];
		}

		for(int i = cur; i < data.length + cur; i++) {
			newData[i] = new Sound(null, 0, 0.0, 0.0, 0.0);
		}

		for(int i = cur; i < data.length; i++) {
			newData[data.length + i] = data[i];
		}

		//cur remains the same
		data = newData;
	}

	/*
	 * Class that represents a noteblock sound
	 */
	public static class InstrumentSound {}
}
