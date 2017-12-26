package com.harmony.harmonymod.tricks;

import net.minecraft.entity.*;

public interface Trick {
	/*
	 * Call to have entity perform trick.
	 * Returns True if should be called again,
	 *         False if action is over.
	 */
	public boolean act();

	/*
	 * Should be called immediately after construction, before act()
	 */
	public void setupTrick(EntityCreature entity, Object firstSound, Object secondSound);
}
