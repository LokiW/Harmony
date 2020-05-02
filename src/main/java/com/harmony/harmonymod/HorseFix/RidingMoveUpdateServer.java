package com.harmony.harmonymod.horsefix;

import net.minecraftforge.common.MinecraftForge;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class RidingMoveUpdateServer {
	public static Map<Integer, NetHandlerPlayServer> serverHandlers = new HashMap<Integer, NetHandlerPlayServer>();

    public static void register() {
		RidingMoveUpdateServer handler = new RidingMoveUpdateServer();
        MinecraftForge.EVENT_BUS.register(handler);
		FMLCommonHandler.instance().bus().register(handler);
    }

    @SubscribeEvent
    public void sendRideUpdateServer(PlayerTickEvent event) {
		EntityPlayer player = event.player;
		if(player.ridingEntity == null ||
			!MoveEntityHelper.entityRequiresMoveHelper(player.ridingEntity)) {
			return;
		}

        if(event.side == Side.SERVER) {
			if (!RidingMoveUpdateServer.serverHandlers.containsKey(player.getEntityId())) {
				RidingMoveUpdateServer.updateServerNetHandler(player);
			}
        }
	}

	public static void updateServerNetHandler(EntityPlayer player) {
		if (player == null || player.worldObj == null) {
			return;
		}

        if (!player.worldObj.isRemote && player.worldObj instanceof WorldServer) {
            MinecraftServer server = ((WorldServer)player.worldObj).func_73046_m();
			// Mutilate the server so that the NetHandlerPlayServer is HarmonyHandlePlayServer
			// so that horses are handled on client side.
			NetworkSystem netSystem = server.func_147137_ag();
			if (netSystem == null) {
				System.out.println("HarmonyMod: server has no NetworkSystem field");
				return;
			}
			try {
				Field[] fields = netSystem.getClass().getDeclaredFields();
				Field managerF = null;
				for (Field field : fields) {
					if (field.getType() == List.class) {
						field.setAccessible(true);
						List potentialList = (List) field.get(netSystem);
						for (Object item : potentialList) {
							if (item instanceof NetworkManager) {
								managerF = field;
							}
						}
					}
				}

				if (managerF == null) {
					System.out.println("HarmonyMod: no network manager found :(");
					return;
				}

				managerF.setAccessible(true);
				List<NetworkManager> netManagers = (List<NetworkManager>) managerF.get(netSystem);
				for (NetworkManager manager : netManagers) {
					if (manager.getNetHandler() instanceof NetHandlerPlayServer &&
							!(manager.getNetHandler() instanceof NetHandlerPlayAndRideServer)) {
						NetHandlerPlayServer oldHandler = (NetHandlerPlayServer) manager.getNetHandler();
						NetHandlerPlayAndRideServer newHandler = new NetHandlerPlayAndRideServer(server, manager, oldHandler.playerEntity);
						manager.setNetHandler(newHandler);
						RidingMoveUpdateServer.serverHandlers.put(player.getEntityId(), newHandler);
						System.out.println("HarmonyMod: Replaced NetHandlerPlayServer");
					}
				}

			} catch(Exception ex) {
				System.out.println("HarmonyMod: Got exception trying to replace network handler " + ex);
			}
		}
	}


}
