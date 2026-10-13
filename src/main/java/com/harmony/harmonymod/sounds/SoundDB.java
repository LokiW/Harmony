package com.harmony.harmonymod.sounds;

import com.harmony.harmonymod.HarmonyProps;
import com.harmony.harmonymod.tricks.TrickHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import cpw.mods.fml.relauncher.Side;
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
		// Register to receive noteblock events, and ticks to check on listening animals
		MinecraftForge.EVENT_BUS.register(db);
		FMLCommonHandler.instance().bus().register(db);

		return db;
	}

	// Other's can't instantiate
	private SoundDB() {}

	// Animals in the middle of hearing a phrase
	private final Set<TrickHandler> listening = new LinkedHashSet<TrickHandler>();

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
			if(hp != null && hp.tricks != null && !w.isRemote) {
				hp.tricks.hearNote(note);
				listening.add(hp.tricks);
			}
		}
	}

	/*
	 * After all of a tick's notes are in (so chords are complete), let listening animals act on what
	 * they've heard
	 */
	@SubscribeEvent
	public void onWorldTick(WorldTickEvent event) {
		if (event.side != Side.SERVER || event.phase != Phase.END || listening.isEmpty()) {
			return;
		}
		Iterator<TrickHandler> it = listening.iterator();
		while (it.hasNext()) {
			TrickHandler listener = it.next();
			if (listener.pet.worldObj != event.world) {
				continue;
			}
			if (listener.pet.isDead || !listener.listen()) {
				it.remove();
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
