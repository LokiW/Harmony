package com.harmony.harmonymod.horsefix;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
import java.lang.Math;
import java.lang.Thread;
import java.lang.StackTraceElement;
import java.lang.reflect.Field;

public class NetHandlerPlayAndRideServer extends NetHandlerPlayServer {
    public MinecraftServer server;

    public Field lastPosXF;
    public Field lastPosYF;
    public Field lastPosZF;

    public NetHandlerPlayAndRideServer(MinecraftServer server, NetworkManager netManager, EntityPlayerMP player) {
        super(server, netManager, player);
        this.server = server;
        try {
            this.lastPosXF = this.getClass().getSuperclass().getDeclaredField("lastPosX");
            this.lastPosXF.setAccessible(true);

            this.lastPosYF = this.getClass().getSuperclass().getDeclaredField("lastPosZ");
            this.lastPosYF.setAccessible(true);

            this.lastPosZF = this.getClass().getSuperclass().getDeclaredField("lastPosZ");
            this.lastPosZF.setAccessible(true);

        } catch (Exception e) {
            System.out.println("HarmonyMod: failed to get lastPosX, lastPosY and lastPosZ");
        }
    }

    /*
     * Override process player which would normally only get player
     * location from the client when not ridding an entity.
     * Change it to set entitly location from client too.
     */
    @Override
    public void processPlayer(C03PacketPlayer playerPacket) {
        try {
            System.out.println("HarmonyMod: playerx="+this.playerEntity.posX+
                    " lastX="+this.lastPosXF.getDouble(this)+
                    " motionX="+this.playerEntity.motionX+
                    " playerz="+this.playerEntity.posZ+
                    " lastZ="+this.lastPosZF.getDouble(this));
        } catch (Exception e) {
        }

        if (playerPacket.func_149464_c() > 0 && (Math.abs(playerPacket.func_149464_c()-this.playerEntity.posX) > 4 ||
                Math.abs(playerPacket.func_149472_e() - this.playerEntity.posZ) > 4)) {
            System.out.println("HarmonyMod: GOT BAD C03PacketPlayer Packet: playerx=" + this.playerEntity.posX +
                            " packetX="+ playerPacket.func_149464_c() +
                            " playerz="+ this.playerEntity.posZ +
                            " packetz="+ playerPacket.func_149472_e());
        }
        if (this.playerEntity.ridingEntity == null ||
                !MoveEntityHelper.entityRequiresMoveHelper(this.playerEntity.ridingEntity)) {
            super.processPlayer(playerPacket);
            return;
        }
        WorldServer world = this.server.worldServerForDimension(this.playerEntity.dimension);

        double packetX = playerPacket.func_149464_c();
        double minBoundingBoxY = playerPacket.func_149467_d();
        double packetZ = playerPacket.func_149472_e();
        double packetY = playerPacket.func_149471_f();

        boolean hasMoved = playerPacket.func_149466_j();
        if (!hasMoved || packetY == -999.0D || minBoundingBoxY == -999.0D) {
            return;
        }
        minBoundingBoxY = Math.abs(minBoundingBoxY);
        packetY = Math.abs(packetY);

        float packetYaw = this.playerEntity.rotationYaw;
        float packetPitch = this.playerEntity.rotationPitch;

        if (playerPacket.func_149463_k()) {
            packetYaw = playerPacket.func_149462_g();
            packetPitch = playerPacket.func_149470_h();
        }

        this.playerEntity.ridingEntity.setPosition(packetX, minBoundingBoxY - this.playerEntity.ridingEntity.getMountedYOffset() - this.playerEntity.getYOffset(), packetZ);

        this.playerEntity.updateRidden();
        try {
            this.lastPosXF.setDouble(this, this.playerEntity.posX);
            this.lastPosYF.setDouble(this, this.playerEntity.posY);
            this.lastPosZF.setDouble(this, this.playerEntity.posZ);
        } catch (Exception e) { 
            System.out.println("HarmonyMod: failed to set lastX, lastY, lastZ. May result in teleporting back to place of mounting horse");
        }

        //world.updateEntity(this.playerEntity);
        //System.out.println("HarmonyMod: playerx="+this.playerEntity.posX+ " mountX="+this.playerEntity.ridingEntity.posX+" playerz="+this.playerEntity.posZ+" mountZ="+this.playerEntity.ridingEntity.posZ);

        super.processPlayer(playerPacket);
    }


    @Override
    public void sendPacket(final Packet packet) {
        if (packet instanceof S08PacketPlayerPosLook) {
            System.out.println("HarmonyMod: Teleporting player, playerx="+this.playerEntity.posX+" packetX="+((S08PacketPlayerPosLook)packet).func_148932_c()+" playerz="+this.playerEntity.posZ+" packetZ="+((S08PacketPlayerPosLook)packet).func_148933_e());
        }

        super.sendPacket(packet);
    }

    @Override
    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch) {
        if (Math.abs(x - this.playerEntity.posX) > 4 || Math.abs(z - this.playerEntity.posZ) > 4 ) {
            System.out.println("HarmonyMod: stack trace: ");
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            for (int i = 1; i < elements.length; i++) {
                StackTraceElement s = elements[i];
                System.out.println("\tat " + s.getClassName() + "." + s.getMethodName()
                + "(" + s.getFileName() + ":" + s.getLineNumber() + ")");
            }
        }
        super.setPlayerLocation(x,y,z,yaw,pitch);
    }

}
