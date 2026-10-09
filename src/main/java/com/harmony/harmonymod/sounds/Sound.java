package com.harmony.harmonymod.sounds;

public class Sound {

	public Object message;
	public long timestamp;
	public double x;
	public double y;
	public double z;

	public Sound(Object message, long timestamp, double x, double y, double z) {
		reset(message, timestamp, x, y, z);
	}

	public void reset(Object message, long timestamp, double x, double y, double z) {
		this.message = message;
		this.timestamp = timestamp;
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
