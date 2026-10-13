package com.harmony.harmonymod.sounds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/*
 * Phrases are what animals learn tricks for: the notes heard until a pause. Each step of a phrase is
 * a chord of the notes played in the same tick, usually just one note.
 *
 * A phrase is keyed by a readable string, e.g. "harp 0, harp 4+harp 7": steps separated by ", ",
 * chord notes by "+". These keys are saved with animals, so instrument names must never change.
 */
public class Phrases {
	// A phrase ends after this many ticks without a note
	public static final int PAUSE = 20;
	// Longest phrase, in steps
	public static final int MAX_STEPS = 8;

	// Note block instruments in NoteBlockEvent.Instrument order
	private static final String[] INSTRUMENTS = {"harp", "bassdrum", "snare", "hat", "bass"};
	// Their sounds, "note." + name
	private static final String[] SOUNDS = {"harp", "bd", "snare", "hat", "bassattack"};
	public static final int HARP = 0;

	public static int note(int instrument, int pitch) {
		return pitch | (instrument << 8);
	}

	public static String soundName(int note) {
		int instrument = note >> 8;
		return "note." + SOUNDS[instrument < SOUNDS.length ? instrument : 0];
	}

	/*
	 * Sound pitch for a note, as vanilla note blocks play it
	 */
	public static float soundPitch(int note) {
		return (float) Math.pow(2.0D, ((note & 0xFF) - 12) / 12.0D);
	}

	/*
	 * Steps of a phrase key, or an empty list if it isn't one
	 */
	public static List<SortedSet<Integer>> parse(String key) {
		List<SortedSet<Integer>> steps = new ArrayList<SortedSet<Integer>>();
		if (key == null || key.isEmpty()) {
			return steps;
		}
		for (String stepText : key.split(", ")) {
			SortedSet<Integer> step = new TreeSet<Integer>();
			for (String noteText : stepText.split("\\+")) {
				String[] parts = noteText.split(" ");
				if (parts.length != 2) {
					return new ArrayList<SortedSet<Integer>>();
				}
				int instrument = Arrays.asList(INSTRUMENTS).indexOf(parts[0]);
				int pitch;
				try {
					pitch = Integer.parseInt(parts[1]);
				} catch (NumberFormatException e) {
					return new ArrayList<SortedSet<Integer>>();
				}
				if (instrument < 0 || pitch < 0 || pitch > 24) {
					return new ArrayList<SortedSet<Integer>>();
				}
				step.add(note(instrument, pitch));
			}
			steps.add(step);
		}
		return steps;
	}

	/*
	 * Note id as SoundDB makes it: pitch in the low byte, instrument above
	 */
	public static String noteName(int note) {
		int instrument = note >> 8;
		String name = instrument < INSTRUMENTS.length ? INSTRUMENTS[instrument] : "instrument" + instrument;
		return name + " " + (note & 0xFF);
	}

	public static String key(List<SortedSet<Integer>> steps) {
		StringBuilder key = new StringBuilder();
		for (SortedSet<Integer> step : steps) {
			if (key.length() > 0) {
				key.append(", ");
			}
			boolean first = true;
			for (int note : step) {
				if (!first) {
					key.append("+");
				}
				key.append(noteName(note));
				first = false;
			}
		}
		return key.toString();
	}

	/*
	 * Whether longer is shorter followed by more steps
	 */
	public static boolean startsWith(String longer, String shorter) {
		return longer.startsWith(shorter + ", ");
	}
}
