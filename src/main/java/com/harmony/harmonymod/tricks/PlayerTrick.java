package com.harmony.harmonymod.tricks;

import com.harmony.harmonymod.HarmonyProps;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import java.util.UUID;

/*
 * A trick about a player, e.g. following them. The player is whoever played the phrase on an
 * instrument, or for noteblocks the animal's bonded owner if they're nearby, otherwise the nearest player.
 */
public abstract class PlayerTrick extends Trick {
	// How far away the animal notices a player for the trick
	protected static final double PLAYER_RANGE = 32.0D;

	private UUID playerId;
	private EntityPlayer player;

	public void setupTrick(EntityLiving pet, Trick currentTrick) {
		this.pet = pet;
		EntityPlayer chosen = choosePlayer(pet);
		if (chosen != null) {
			this.player = chosen;
			this.playerId = chosen.getUniqueID();
		}
	}

	private static EntityPlayer choosePlayer(EntityLiving pet) {
		HarmonyProps hp = HarmonyProps.get(pet);
		if (hp != null && hp.tricks != null && hp.tricks.commander != null) {
			return hp.tricks.commander;
		}
		if (hp != null && hp.isBonded()) {
			EntityPlayer owner = pet.worldObj.func_152378_a(hp.ownerId);
			if (owner != null && owner.getDistanceSqToEntity(pet) <= PLAYER_RANGE * PLAYER_RANGE) {
				return owner;
			}
		}
		return pet.worldObj.getClosestPlayerToEntity(pet, PLAYER_RANGE);
	}

	/*
	 * The player, or null if they've logged off or are in another world
	 */
	protected EntityPlayer getPlayer() {
		if (this.playerId == null) {
			return null;
		}
		if (this.player == null || this.player.isDead || this.player.worldObj != this.pet.worldObj) {
			// Logged back in, respawned or changed dimension: find them again
			this.player = this.pet.worldObj.func_152378_a(this.playerId);
		}
		return this.player;
	}

	/*
	 * Name of the player, for showing what the animal is doing, or "" if they aren't around
	 */
	public String getPlayerName() {
		if (this.playerId == null || this.pet == null) {
			return "";
		}
		EntityPlayer player = this.pet.worldObj.func_152378_a(this.playerId);
		return player != null ? player.getCommandSenderName() : "";
	}

	public boolean isPlayer(EntityPlayer player) {
		return this.playerId != null && this.playerId.equals(player.getUniqueID());
	}

	protected boolean hasPlayer() {
		return this.playerId != null;
	}

	public boolean isInstant() {
		return false;
	}

	@Override
	protected void writeToNBT(NBTTagCompound tag) {
		if (this.playerId != null) {
			tag.setLong("PlayerMost", this.playerId.getMostSignificantBits());
			tag.setLong("PlayerLeast", this.playerId.getLeastSignificantBits());
		}
	}

	@Override
	protected void readFromNBT(NBTTagCompound tag) {
		if (tag.hasKey("PlayerMost")) {
			this.playerId = new UUID(tag.getLong("PlayerMost"), tag.getLong("PlayerLeast"));
		}
	}
}
