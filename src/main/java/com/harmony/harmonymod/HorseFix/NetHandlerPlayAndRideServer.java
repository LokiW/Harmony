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
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import java.lang.Math;
import java.lang.Thread;
import java.lang.StackTraceElement;
import java.lang.reflect.Field;

public class NetHandlerPlayAndRideServer extends NetHandlerPlayServer {
    public MinecraftServer server;

    public Field lastPosXF = null;
    public Field lastPosYF = null;
    public Field lastPosZF = null;

    public static String lastPosXFName = "lastPosX";
    public static String lastPosYFName = "lastPosY";
    public static String lastPosZFName = "lastPosZ";

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
            System.out.println("HarmonyMod: Couldn't initially set parent NetHandlerPlayServer's lastPosX/Y/Z because they are private. Will try again later. Until set, dismounting horses has a small chance to teleport player back to where they mounted.");
        } else {
            this.setupLastPositions = true;
            NetHandlerPlayAndRideServer.lastPosZFName = null;
        }
    }

    @Override
    public void processInput(C0CPacketInput packet) {
        // func_149617_f() is a boolean for isSneaking which is input for dismounting horse
        if (packet.func_149617_f() || this.playerEntity.ridingEntity == null ||
                !MoveEntityHelper.entityRequiresMoveHelper(this.playerEntity.ridingEntity)) {
            super.processInput(packet);
        }
    }

    /*
     * Override process player which would normally only get player
     * location from the client when not ridding an entity.
     * Change it to set entitly location from client too.
     */
    @Override
    public void processPlayer(C03PacketPlayer playerPacket) {
        if (playerPacket.func_149466_j()) {
            System.out.println("HarmonyMod: player "+this.playerEntity.getEntityId() +"  "+(playerPacket.func_149471_f()-playerPacket.func_149467_d()));
        }
        if (!this.setupLastPositions) {
            this.tryToSetupLastPositions();
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

        if (Math.abs(packetX - this.playerEntity.posX) > 2.0 ||
            Math.abs(packetY - this.playerEntity.posY) > 2.0 ||
            Math.abs(packetZ - this.playerEntity.posZ) > 2.0) {
            System.out.println("HarmonyMod:");
        }

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

        double mountY = minBoundingBoxY - this.playerEntity.ridingEntity.getMountedYOffset() - this.playerEntity.getYOffset();
        //double mountY = packetY - (this.playerEntity.ridingEntity.getMountedYOffset() + this.playerEntity.getYOffset());

        this.playerEntity.motionX = packetX - this.playerEntity.posX;
        this.playerEntity.motionY = 0.0D;
        this.playerEntity.motionZ = packetZ - this.playerEntity.posZ;

        this.playerEntity.ridingEntity.motionX = packetX - this.playerEntity.ridingEntity.posX;
        this.playerEntity.ridingEntity.motionY = 0.0D;
        this.playerEntity.ridingEntity.motionZ = packetZ - this.playerEntity.ridingEntity.posZ;

        this.playerEntity.ridingEntity.setPosition(packetX, mountY, packetZ);

        this.playerEntity.updateRidden();

        if (this.setupLastPositions) {
            try {
                this.lastPosXF.setDouble(this, this.playerEntity.posX);
                this.lastPosYF.setDouble(this, this.playerEntity.posY);
                this.lastPosZF.setDouble(this, this.playerEntity.posZ);
            } catch (Exception e) {
                // pass as sadly too many things in this world are private :(
            }
        }
        super.processPlayer(playerPacket);
    }

    public void tryToSetupLastPositions() {
        if (Math.abs(this.playerEntity.posX - this.playerEntity.posY) < 1.0 ||
            Math.abs(this.playerEntity.posX - this.playerEntity.posZ) < 1.0 ||
            Math.abs(this.playerEntity.posY - this.playerEntity.posZ) < 1.0) {
            // Positions too close to be safe to set
            return;
        }

        Field[] fields = this.getClass().getSuperclass().getDeclaredFields();
        for (Field field : fields) {
            try {
                if (field.getType().toString().equals("double")) {
                    field.setAccessible(true);
                    double value = field.getDouble(this);
                    if (Math.abs(value - this.playerEntity.posX) < 1.0) {
                        this.lastPosXF = field;
                        if (NetHandlerPlayAndRideServer.lastPosXFName == null) {
                            NetHandlerPlayAndRideServer.lastPosXFName = field.getName();
                        }
                    } else if (Math.abs(value - this.playerEntity.posY) < 1.0) {
                        this.lastPosYF = field;
                        if (NetHandlerPlayAndRideServer.lastPosYFName == null) {
                            NetHandlerPlayAndRideServer.lastPosYFName = field.getName();
                        }
                    } else if (Math.abs(value - this.playerEntity.posZ) < 1.0) {
                        this.lastPosZF = field;
                        if (NetHandlerPlayAndRideServer.lastPosZFName == null) {
                            NetHandlerPlayAndRideServer.lastPosZFName = field.getName();
                        }
                    }
                }
            } catch (Exception e) {
                // Pass
            }
        }
        if (this.lastPosXF != null && this.lastPosYF != null && this.lastPosZF != null) {
            this.setupLastPositions = true;
            System.out.println("HarmonyMod: Set LastPosX/Y/Z of NetHandlerPlayServer. Player will no longer ever teleport when dismounting entities.");
        }
    }

    public void sendPacket(final Packet packet) {
        if (this.playerEntity.ridingEntity == null ||
                !MoveEntityHelper.entityRequiresMoveHelper(this.playerEntity.ridingEntity)) {
            super.sendPacket(packet);
            return;
        }

        if (packet instanceof S08PacketPlayerPosLook) {
            // Client should update server when riding, not visa versa
            return;
        }

        if (packet instanceof S14PacketEntity) {
            Entity updated = ((S14PacketEntity) packet).func_149065_a(this.playerEntity.worldObj);
            if (updated.getEntityId() == this.playerEntity.getEntityId() ||
                 updated.getEntityId() == this.playerEntity.ridingEntity.getEntityId()) {
                return;
            }

        }

        if (packet instanceof S18PacketEntityTeleport) {
            if (((S18PacketEntityTeleport) packet).func_149451_c() == this.playerEntity.getEntityId() ||
                     ((S18PacketEntityTeleport)packet).func_149451_c() == this.playerEntity.ridingEntity.getEntityId()) {
                 return;
            }
        }

        if (packet instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) packet).func_149412_c() == this.playerEntity.getEntityId() ||
                    ((S12PacketEntityVelocity) packet).func_149412_c() == this.playerEntity.ridingEntity.getEntityId()) {
                return;
            }
        }

        super.sendPacket(packet);
    }
}
