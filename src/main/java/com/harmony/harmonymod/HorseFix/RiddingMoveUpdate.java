package com.harmony.harmonymod.horsefix;

import net.minecraftforge.common.MinecraftForge;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.client.multiplayer.WorldClient;
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

public class RiddingMoveUpdate {
	//@SideOnly(Side.CLIENT)
	public static Map<EntityPlayer, NetHandlerPlayClient> playerNetworkQueues = new HashMap<EntityPlayer, NetHandlerPlayClient>();

	//@SideOnly(Side.SERVER)
	public static List<EntityPlayer> updatedServerPlayers = new ArrayList<EntityPlayer>();

    public static void register() {
		RiddingMoveUpdate handler = new RiddingMoveUpdate();
        MinecraftForge.EVENT_BUS.register(handler);
		FMLCommonHandler.instance().bus().register(handler);
    }

    @SubscribeEvent
    public void sendRideUpdate(PlayerTickEvent event) {
		EntityPlayer player = event.player;
		if(player.ridingEntity == null) {
			return;
		}

        if(event.side == Side.SERVER) {
			if (!RiddingMoveUpdate.updatedServerPlayers.contains(player)) {
				RiddingMoveUpdate.updateServerNetHandler(player);
			}
        } else {
			if (!RiddingMoveUpdate.playerNetworkQueues.containsKey(player)) {
				RiddingMoveUpdate.updatePlayerClientNetworkQueue(player);
			}
			if (!RiddingMoveUpdate.playerNetworkQueues.containsKey(player)) {
				// Wasn't found in updatePlayerClientNetworkQueue
				System.out.println("HarmonyMod: didn't find client network queue for player");
				return;
			}
			NetHandlerPlayClient netQueue = RiddingMoveUpdate.playerNetworkQueues.get(player);

			netQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(player.posX,
					player.boundingBox.minY,
					player.posY,
					player.posZ,
					player.rotationYaw,
					player.rotationPitch,
					player.onGround));
		}
    }
	
	public static void updatePlayerClientNetworkQueue(EntityPlayer player) {
		if (player == null || player.worldObj == null) {
			return;
		}

        if (player.worldObj.isRemote && player.worldObj instanceof WorldClient) {
			try {
				WorldClient worldClient = (WorldClient) player.worldObj;
				Field[] fields = worldClient.getClass().getDeclaredFields();
				for (Field field : fields) {
					if (field.getType() == NetHandlerPlayClient.class) {
						field.setAccessible(true);
						NetHandlerPlayClient netQueue = (NetHandlerPlayClient) field.get(worldClient);
						if (netQueue != null) {
							System.out.println("HarmonyMod: updated network queue for client player");
							RiddingMoveUpdate.playerNetworkQueues.put(player, netQueue);
						}
					}
				}
			} catch (Exception e) {
				System.out.println("HarmonyMod: Failed to update network queue for client player with exception " + e);
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
						manager.setNetHandler(new NetHandlerPlayAndRideServer(server, manager, oldHandler.playerEntity));
						System.out.println("HarmonyMod: Replaced NetHandlerPlayServer network handler!!");
						RiddingMoveUpdate.updatedServerPlayers.add(player);
					}
				}

			} catch(Exception ex) {
				System.out.println("HarmonyMod: Got exception trying to replace network handler " + ex);
			}
		}
	}


}
