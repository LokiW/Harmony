package com.harmony.harmonymod;

import com.harmony.harmonymod.Traits.TRAIT;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * /harmony spawn <animal> [trait] [trait] [trait]
 * Spawns an animal with chosen traits at the player (or at world spawn from the console or a
 * command block), for testing traits without breeding for them.
 */
public class HarmonyCommand extends CommandBase {

	@Override
	public String getCommandName() {
		return "harmony";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "harmony.command.usage";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (args.length < 2 || !"spawn".equals(args[0])) {
			throw new WrongUsageException("harmony.command.usage");
		}
		World world = sender.getEntityWorld();

		Entity entity = EntityList.createEntityByName(args[1], world);
		if (!(entity instanceof EntityLiving) || HarmonyProps.get(entity) == null) {
			throw new CommandException("harmony.command.not_harmony", args[1]);
		}

		TRAIT[] chosen = new TRAIT[args.length - 2];
		for (int i = 2; i < args.length; i++) {
			try {
				chosen[i - 2] = TRAIT.valueOf(args[i].toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new CommandException("harmony.command.unknown_trait", args[i]);
			}
		}

		EntityLiving animal = (EntityLiving) entity;
		if (sender instanceof EntityPlayerMP) {
			EntityPlayerMP player = (EntityPlayerMP) sender;
			animal.setLocationAndAngles(player.posX, player.posY, player.posZ, player.rotationYaw, 0.0F);
		} else {
			ChunkCoordinates spawn = world.getSpawnPoint();
			int y = world.getTopSolidOrLiquidBlock(spawn.posX, spawn.posZ);
			animal.setLocationAndAngles(spawn.posX + 0.5D, y, spawn.posZ + 0.5D, 0.0F, 0.0F);
		}
		animal.onSpawnWithEgg(null);
		HarmonyProps.get(animal).constructProperties(chosen);
		world.spawnEntityInWorld(animal);

		String traits = "";
		for (TRAIT t : HarmonyProps.get(animal).traits.traits) {
			traits += (traits.isEmpty() ? "" : " ") + t;
		}
		func_152373_a(sender, this, "harmony.command.spawned", animal.getCommandSenderName(), traits);
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "spawn");
		}
		if (args.length == 2) {
			return getListOfStringsMatchingLastWord(args, harmonyAnimalNames());
		}
		if (args.length <= 5) {
			String[] names = new String[TRAIT.values().length];
			for (int i = 0; i < names.length; i++) {
				names[i] = TRAIT.values()[i].name();
			}
			return getListOfStringsMatchingLastWord(args, names);
		}
		return null;
	}

	/*
	 * Entity names of the animals Harmony adds data to
	 */
	private static String[] harmonyAnimalNames() {
		List<String> names = new ArrayList<String>();
		for (Object o : EntityList.stringToClassMapping.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			String className = ((Class) entry.getValue()).getSimpleName().toLowerCase();
			if (HarmonyMod.harmonyMobs.contains(className)) {
				names.add((String) entry.getKey());
			}
		}
		return names.toArray(new String[names.size()]);
	}
}
