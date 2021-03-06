package com.harmony.harmonymod.tricks;

/*
 * Encoding of trick names to numbers
 */
public class TrickEnum {

	public static final int STOP = 1 << 0;
	public static final int GO = 1 << 1;
	public static final int LEARNED_LOCATION_1 = 1 << 2;
	public static final int LEARNED_LOCATION_2 = 1 << 3;
	public static final int LEARNED_LOCATION_3 = 1 << 4;
	public static final int ATTACK = 1 << 5;
	public static final int GUARD = 1 << 6;
	public static final int JUMP = 1 << 7;
	public static final int SIT = 1 << 8;
	public static final int RESPAWN_LOCATION = 1 << 9;

	// This is used for action initialization and should be updated
	// for each new trick added to this enum
	public static final int ALL_TRICKS = (1 << 10) - 1;
}
