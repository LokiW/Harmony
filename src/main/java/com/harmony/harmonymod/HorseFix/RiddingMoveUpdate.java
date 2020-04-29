

public class RiddingMoveUpdate {

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new SendHorseMoveUpdate());
    }

    @SubscribeEvent
    pubic void sendRideUpdate(PlayerTickEvent event) {
		EntityPlayer player = event.player;
		if(!player.ridingEntity) {
			return;
		}

        if(event.side == Side.SERVER) {
			RiddingMoveUpdate.updateServerNetHandler(player);
        } else {
		
		}

    }

	public static void updateServerNetHandler(EntityLiving player) {
		if (HarmonyMod.isRiddingUpdated || player == null || player.worldObj == null) {
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
						//HarmonyMod.isRiddingUpdated = true;
						//need to update per player
					}
				}

			} catch(Exception ex) {
				System.out.println("HarmonyMod: Got exception trying to replace network handler " + ex);
			}
		}
	}


}
