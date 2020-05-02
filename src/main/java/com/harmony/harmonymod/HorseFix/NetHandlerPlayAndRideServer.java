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

    public Field lastPosXF = null;
    public Field lastPosYF = null;
    public Field lastPosZF = null;

    public static String lastPosXFName = "lastPasX";
    public static String lastPosYFName = "lastPasY";
    public static String lastPosZFName = "lastPasZ";

    public boolean setupLastPositions = false;

    public NetHandlerPlayAndRideServer(MinecraftServer server, NetworkManager netManager, EntityPlayerMP player) {
        super(server, netManager, player);
        this.server = server;
        boolean failure = false;
        try {
            this.lastPosXF = this.getClass().getSuperclass().getDeclaredField(NetHandlerPlayAndRideServer.lastPosXFName);
            this.lastPosXF.setAccessible(true);
        } catch (Exception e) {
            failure = true;
            NetHandlerPlayAndRideServer.lastPosXFName = null;
        }

        try {
            this.lastPosYF = this.getClass().getSuperclass().getDeclaredField(NetHandlerPlayAndRideServer.lastPosYFName);
            this.lastPosYF.setAccessible(true);
        } catch (Exception e) {
            failure = true;
            NetHandlerPlayAndRideServer.lastPosYFName = null;
        }

        try {
            this.lastPosZF = this.getClass().getSuperclass().getDeclaredField(NetHandlerPlayAndRideServer.lastPosZFName);
            this.lastPosZF.setAccessible(true);

            setupLastPositions = true;

        } catch (Exception e) {
            failure = true;
            NetHandlerPlayAndRideServer.lastPosZFName = null;
        }

        if (failure) {
            System.out.println("HarmonyMod: failed to get lastPosX, lastPosY and lastPosZ");
        } else {
            this.setupLastPositions = true;
            NetHandlerPlayAndRideServer.lastPosZFName = null;
        }
    }

    /*
     * Override process player which would normally only get player
     * location from the client when not ridding an entity.
     * Change it to set entitly location from client too.
     */
    @Override
    public void processPlayer(C03PacketPlayer playerPacket) {
        if (!this.setupLastPositions) {
            this.tryToSetupLastPositions();
        }
        try {
            System.out.println("HarmonyMod: playerx="+this.playerEntity.posX+
                    " lastX="+this.lastPosXF.getDouble(this)+
                    " motionX="+this.playerEntity.motionX+
                    " playerz="+this.playerEntity.posZ+
                    " lastZ="+this.lastPosZF.getDouble(this));
        } catch (Exception e) {
            // TODO this whole print is for debugging, remove
            System.out.println("HarmonyMod: " + e);
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

        if (this.setupLastPositions) {
            try {
                this.lastPosXF.setDouble(this, this.playerEntity.posX);
                this.lastPosYF.setDouble(this, this.playerEntity.posY);
                this.lastPosZF.setDouble(this, this.playerEntity.posZ);
            } catch (Exception e) {
                // pass
                System.out.println("HarmonyMod: " + e);
            }
        }

        //world.updateEntity(this.playerEntity);
        //System.out.println("HarmonyMod: playerx="+this.playerEntity.posX+ " mountX="+this.playerEntity.ridingEntity.posX+" playerz="+this.playerEntity.posZ+" mountZ="+this.playerEntity.ridingEntity.posZ);

        super.processPlayer(playerPacket);
    }

    public void tryToSetupLastPositions() {
        if (Math.abs(this.playerEntity.posX - this.playerEntity.posY) < 1.0 ||
            Math.abs(this.playerEntity.posX - this.playerEntity.posZ) < 1.0 ||
            Math.abs(this.playerEntity.posY - this.playerEntity.posZ) < 1.0) {
            // Positions too close to be safe to set
            System.out.println("HarmonyMod: couldn't set last positions because player x, y or z too similar");
            return;
        }

        Field[] fields = this.getClass().getSuperclass().getDeclaredFields();
        for (Field field : fields) {
            try {
                System.out.println("HarmonyMod: " + field.getName() + "   " + field.getType());
                if (field.getType().toString().equals("double")) {
                    field.setAccessible(true);
                    double value = field.getDouble(this);
                    System.out.println("HarmonyMod: found double");
                    if (Math.abs(value - this.playerEntity.posX) < 1.0) {
                        System.out.println("HarmonyMod: set lastPosX");
                        this.lastPosXF = field;
                        if (NetHandlerPlayAndRideServer.lastPosXFName == null) {
                            NetHandlerPlayAndRideServer.lastPosXFName = field.getName();
                        }
                    } else if (Math.abs(value - this.playerEntity.posY) < 1.0) {
                        System.out.println("HarmonyMod: set lastPosY");
                        this.lastPosYF = field;
                        if (NetHandlerPlayAndRideServer.lastPosYFName == null) {
                            NetHandlerPlayAndRideServer.lastPosYFName = field.getName();
                        }
                    } else if (Math.abs(value - this.playerEntity.posZ) < 1.0) {
                        System.out.println("HarmonyMod: set lastPosZ");
                        this.lastPosZF = field;
                        if (NetHandlerPlayAndRideServer.lastPosZFName == null) {
                            NetHandlerPlayAndRideServer.lastPosZFName = field.getName();
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("HarmonyMod: failed to set NetHandlerPlayAndRideServer last Positions. May result in teleporting back to place of mounting horse.");
            }
        }
        if (this.lastPosXF != null && this.lastPosYF != null && this.lastPosZF != null) {
            this.setupLastPositions = true;
        }
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
