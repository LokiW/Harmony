package com.harmony.harmonymod.sounds;

import com.harmony.harmonymod.HarmonyProps;
import net.minecraft.entity.player.EntityPlayer;
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
		hear(e.world, e.x + 0.5D, e.y + 0.5D, e.z + 0.5D, getSound(e.instrument, e.getVanillaNoteId()), null);
	}

	/*
	 * A note played at a point, by a noteblock (player null) or a player's instrument. Animals within
	 * range hear it. Bonded animals ignore other players' instruments; noteblocks work for anyone.
	 */
	public void hear(World w, double x, double y, double z, int note, EntityPlayer player) {
		if (w.isRemote) {
			return;
		}
		AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(
				x - MAX_NOTE_DISTANCE,
				y - MAX_NOTE_DISTANCE,
				z - MAX_NOTE_DISTANCE,
				x + MAX_NOTE_DISTANCE,
				y + MAX_NOTE_DISTANCE,
				z + MAX_NOTE_DISTANCE);

		List<EntityLiving> entities = w.getEntitiesWithinAABB(EntityLiving.class, aabb);
		for(EntityLiving ent : entities) {
			HarmonyProps hp = HarmonyProps.get(ent);
			if(hp != null && hp.tricks != null) {
				if (player != null && hp.isBonded() && !hp.isOwner(player)) {
					continue;
				}
				hp.tricks.hearNote(note, player);
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
