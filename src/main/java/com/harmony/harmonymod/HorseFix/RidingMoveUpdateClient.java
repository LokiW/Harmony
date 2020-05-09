package com.harmony.harmonymod.horsefix;

import net.minecraftforge.common.MinecraftForge;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.client.multiplayer.WorldClient;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.client.FMLClientHandler;
import io.netty.util.concurrent.GenericFutureListener;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class RidingMoveUpdateClient {
    public static void register() {
		RidingMoveUpdateClient handler = new RidingMoveUpdateClient();
        MinecraftForge.EVENT_BUS.register(handler);
		FMLCommonHandler.instance().bus().register(handler);
		System.out.println("HarmonyMod: RidingMoveUpdateClient Registered");
    }

	@SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void sendRideUpdateClient(PlayerTickEvent event) {
        if(event.side == Side.SERVER) {
			return;
        }

		EntityPlayer player = event.player;
		if(player.ridingEntity == null ||
			!MoveEntityHelper.entityRequiresMoveHelper(player.ridingEntity)) {
			return;
		}

		MoveEntityHelper.moveEntityWithHeading((EntityLivingBase)player.ridingEntity,
												player.moveStrafing, player.moveForward);

		NetworkManager netQueue = FMLClientHandler.instance().getClientToServerNetworkManager();
		if (netQueue == null) {
			System.out.println("HarmonyMod: didn't send anything");
			return;
		}

		Packet packet = new C03PacketPlayer.C06PacketPlayerPosLook(player.posX,
				player.boundingBox.minY,
				player.posY,
				player.posZ,
				player.rotationYaw,
				player.rotationPitch,
				player.onGround);
		
		netQueue.scheduleOutboundPacket(packet, new GenericFutureListener[0]);
    }
}
