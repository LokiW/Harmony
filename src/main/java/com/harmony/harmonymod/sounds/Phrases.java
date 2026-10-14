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

	// Note names of the 25 note block pitches, F#3 to F#5
	private static final String[] PITCH_NAMES = {
		"F#3", "G3", "G#3", "A3", "A#3", "B3", "C4", "C#4", "D4", "D#4", "E4", "F4",
		"F#4", "G4", "G#4", "A4", "A#4", "B4", "C5", "C#5", "D5", "D#5", "E5", "F5", "F#5",
	};
	// Short instrument names shown before notes
	private static final String[] INSTRUMENT_TAGS = {"Harp ", "Drum ", "Snare ", "Hat ", "Bass "};

	/*
	 * A phrase for players: note names, chords joined with "+", and the instrument named only where it
	 * changes, e.g. "Bass A3 F#4+C#5 Harp A3 A3"
	 */
	public static String display(String key) {
		StringBuilder out = new StringBuilder();
		int lastInstrument = -1;
		for (SortedSet<Integer> step : parse(key)) {
			if (out.length() > 0) {
				out.append(' ');
			}
			boolean first = true;
			for (int note : step) {
				if (!first) {
					out.append('+');
				}
				int instrument = note >> 8;
				if (instrument != lastInstrument) {
					out.append(instrument < INSTRUMENT_TAGS.length ? INSTRUMENT_TAGS[instrument] : "");
					lastInstrument = instrument;
				}
				out.append(PITCH_NAMES[note & 0xFF]);
				first = false;
			}
		}
		return out.length() > 0 ? out.toString() : key;
	}

	/*
	 * display(), cut short with "..." if it's longer than maxChars
	 */
	public static String display(String key, int maxChars) {
		String text = display(key);
		return text.length() <= maxChars ? text : text.substring(0, Math.max(0, maxChars - 3)).trim() + "...";
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
