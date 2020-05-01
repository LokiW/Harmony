package com.harmony.harmonymod.horsefix;

import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;


public class NetHandlerPlayAndRideClient extends NetHandlerPlayClient {
    public Minecraft mc;

    //temp
    public Entity lastRiddenEntity;

    public NetHandlerPlayAndRideClient(Minecraft mc, GuiScreen screen, NetworkManager manager) {
        super(mc, screen, manager);
        this.mc = mc;
    }

    @Override
    public void handlePlayerPosLook(S08PacketPlayerPosLook serverPacket) {
        EntityClientPlayerMP player = this.mc.thePlayer;

        if (player.ridingEntity == null) {
            super.handlePlayerPosLook(serverPacket);
        }
    }

    @Override
    public void handleEntityMovement(S14PacketEntity packetEntity) {
        EntityClientPlayerMP player = this.mc.thePlayer;
        Entity updated = packetEntity.func_149065_a(this.mc.theWorld);
        if (player.ridingEntity == null ||
                (updated.getEntityId() != player.getEntityId() &&
                 updated.getEntityId() != player.ridingEntity.getEntityId())) {
            if (lastRiddenEntity != null && updated.getEntityId() == lastRiddenEntity.getEntityId()) {
                System.out.println("HarmonyMod: updated entity now that it is off player");
            }
            super.handleEntityMovement(packetEntity);
        } else {
            lastRiddenEntity = player.ridingEntity;
            if (updated.getEntityId() == player.ridingEntity.getEntityId()) {
                System.out.println("HarmonyMod: send entity movement to " + player.ridingEntity + " but ignored it.");
            }
        }
    }
}
