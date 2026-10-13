package com.harmony.harmonymod.sounds;

import java.util.List;
import java.util.SortedSet;

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
