package com.harmony.harmonymod.horsefix;

import net.minecraftforge.common.MinecraftForge;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
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
	public static Map<Integer, NetHandlerPlayClient> clientHandlers = new HashMap<Integer, NetHandlerPlayClient>();

	//@SideOnly(Side.SERVER)
	public static Map<Integer, NetHandlerPlayServer> serverHandlers = new HashMap<Integer, NetHandlerPlayServer>();

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

		boolean updatedRiddingEntity = false;

		// Check to see if ridingEntity needs to be wrapped for smooth riding
		if (!(player.ridingEntity instanceof RideableEntityWrapper)) {
			if (!RideableEntityWrapper.entityRequiresWrap(player.ridingEntity)) {
				// Don't need to continue processing player tick if riding entity
				// isn't configured for a riding fix
				return;
			}
			System.out.println("HarmonyMod: ridignEntity should be wrapped");
	
			if (event.side == Side.CLIENT) {
				// Skip client ticks until wrapper entity established on both sides
				return;			
			}

			// Wrap entity
			if (!(player.worldObj instanceof WorldServer)) {
				// TODO deal with this??
				return;
			}

			WorldServer world = (WorldServer) player.worldObj;
			EntityLivingBase mount = (EntityLivingBase) player.ridingEntity;

			// Need to wrap entity for better ridding
			RideableEntityWrapper wrapper = new RideableEntityWrapper(world, mount);
			// This sends packet to client so the wrapper exists on both sides
			world.spawnEntityInWorld(wrapper);

			player.ridingEntity = wrapper;
			updatedRiddingEntity = true;
			System.out.println("HarmonyMod: riddingEntity wrapped");
		}

        if(event.side == Side.SERVER) {
			if (!RiddingMoveUpdate.serverHandlers.containsKey(player.getEntityId())) {
				RiddingMoveUpdate.updateServerNetHandler(player);
			}
			if (updatedRiddingEntity && RiddingMoveUpdate.serverHandlers.containsKey(player.getEntityId())) {
				// Need to tell client about wrapper existing if it was updated
				NetHandlerPlayServer netQueue = RiddingMoveUpdate.serverHandlers.get(player.getEntityId());
				netQueue.sendPacket(new S1BPacketEntityAttach(1, player, player.ridingEntity));
			}
        } else {
			if (!RiddingMoveUpdate.clientHandlers.containsKey(player.getEntityId())) {
				//RiddingMoveUpdate.updatePlayerClientNetworkQueue(player);
				RiddingMoveUpdate.setPlayerClientNetworkQueue(player);
			}
			if (!RiddingMoveUpdate.clientHandlers.containsKey(player.getEntityId())) {
				// Wasn't found in updatePlayerClientNetworkQueue
				System.out.println("HarmonyMod: didn't find client network queue for player");
				return;
			}
			NetHandlerPlayClient netQueue = RiddingMoveUpdate.clientHandlers.get(player.getEntityId());
			
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
							RiddingMoveUpdate.clientHandlers.put(player.getEntityId(), netQueue);
						}
					}
				}
			} catch (Exception e) {
				System.out.println("HarmonyMod: Failed to update network queue for client player with exception " + e);
			}
		}
	}

	public static void setPlayerClientNetworkQueue(EntityPlayer player) {
		if (player == null || player.worldObj == null) {
			return;
		}

        if (!player.worldObj.isRemote || !(player.worldObj instanceof WorldClient)) {
			return;
		}
		try {
			WorldClient worldClient = (WorldClient) player.worldObj;
			Field[] fields = worldClient.getClass().getDeclaredFields();
			Field netHandlerField = null;
			Minecraft mc = null;
			for (Field field : fields) {
				if (field.getType() == NetHandlerPlayClient.class) {
					field.setAccessible(true);
					netHandlerField = field;
				} else if (field.getType() == Minecraft.class) {
					field.setAccessible(true);
					mc = (Minecraft) field.get(worldClient);
					System.out.println("HarmonyMod: found Minecraft object in worldClient");
				}
			}

			NetHandlerPlayClient netQueue = (NetHandlerPlayClient) netHandlerField.get(worldClient);
			NetworkManager manager = null;
			NetHandlerPlayAndRideClient newNetHandler = new NetHandlerPlayAndRideClient(mc, mc.currentScreen, manager);

			Field[] newFields = newNetHandler.getClass().getDeclaredFields();
			Field[] queueFields = netQueue.getClass().getDeclaredFields();
			for (Field oldField : queueFields) {
				if (oldField.getType() == NetworkManager.class) {
					oldField.setAccessible(true);
					manager = (NetworkManager) oldField.get(netQueue);
				}
				try {
					oldField.setAccessible(true);
					oldField.set(newNetHandler, oldField.get(netQueue));
					System.out.println("HarmonyMod: set value " + oldField.getName());
				} catch (Exception e) {
					System.out.println("HarmonyMod: DID NOT set value " + oldField.getName());
				}
			}

			if (manager != null && mc != null) {
				manager.setNetHandler(newNetHandler);
				netHandlerField.set(worldClient, newNetHandler);
				netQueue = newNetHandler;
				Field[] playerFields = mc.thePlayer.getClass().getDeclaredFields();
				for (Field field : playerFields) {
					if(field.getType() == NetHandlerPlayClient.class) {
						field.setAccessible(true);
						field.set(mc.thePlayer, newNetHandler);
						break;
					}
				}
				Field[] mcFields = mc.getClass().getDeclaredFields();
				for (Field field : mcFields) {
					if(field.getType() == NetworkManager.class) {
						field.setAccessible(true);
						NetworkManager mcManager = (NetworkManager) field.get(mc);
						if (mcManager != null) {
							mcManager.setNetHandler(newNetHandler);
						} else {
							field.set(mc, manager);
						}
						break;
					}
				}
				System.out.println("HarmonyMod: Replaced NetHandlerPlayClient");
			} else {
				System.out.println("HarmonyMod: DID NOT NetHandlerPlayClient");
			}

			if (netQueue != null) {
				System.out.println("HarmonyMod: updated network queue for client player");
				RiddingMoveUpdate.clientHandlers.put(player.getEntityId(), netQueue);
			}	
		} catch (Exception e) {
			System.out.println("HarmonyMod: Failed to update NetHandlerPlayClient: " + e);
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
						System.out.println("HarmonyMod: Replaced NetHandlerPlayServer");
						RiddingMoveUpdate.serverHandlers.put(player.getEntityId(), newHandler);
					}
				}

			} catch(Exception ex) {
				System.out.println("HarmonyMod: Got exception trying to replace network handler " + ex);
			}
		}
	}


}
