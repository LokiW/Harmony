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
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
import java.lang.Math;

public class NetHandlerPlayAndRideServer extends NetHandlerPlayServer {
    public MinecraftServer server;

    public Entity lastRiddenEntity;

    public NetHandlerPlayAndRideServer(MinecraftServer server, NetworkManager netManager, EntityPlayerMP player) {
        super(server, netManager, player);
        this.server = server;
        System.out.println("HarmonyMod: updated riding for player " + player);
    }

    /*
     * Override process player which would normally only get player
     * location from the client when not ridding an entity.
     * Change it to set entitly location from client too.
     */
    @Override
    public void processPlayer(C03PacketPlayer playerPacket) {
        if (this.playerEntity.ridingEntity == null) {
            super.processPlayer(playerPacket);
            return;
        }
        this.lastRiddenEntity = this.playerEntity.ridingEntity;

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

        this.lastRiddenEntity.setPosition(packetX, minBoundingBoxY - this.lastRiddenEntity.getMountedYOffset() - this.playerEntity.getYOffset(), packetZ);

        this.playerEntity.updateRidden();

        world.updateEntity(this.playerEntity);
        world.updateEntity(this.lastRiddenEntity);

        super.processPlayer(playerPacket);
    }

    @Override
    public void sendPacket(final Packet packet) {
        if (packet instanceof S1BPacketEntityAttach) {
            S1BPacketEntityAttach attach = (S1BPacketEntityAttach) packet;
            if (attach.func_149403_d() == this.playerEntity.getEntityId() &&
                    attach.func_149402_e() == -1 && this.lastRiddenEntity != null) {
                // Player is dismounting
                // update mounted entity server values 
                // since we've been blocking updates, these values are very out of date
                // teleporting will set them to server side, other movement provides an offset
                super.sendPacket(packet);

                int packetX = this.lastRiddenEntity.myEntitySize.multiplyBy32AndRound(this.lastRiddenEntity.posX);
                int packetY = MathHelper.floor_double(this.lastRiddenEntity.posY * 32.0D);
                int packetZ = this.lastRiddenEntity.myEntitySize.multiplyBy32AndRound(this.lastRiddenEntity.posZ);

                int packetYaw = MathHelper.floor_float(this.lastRiddenEntity.rotationYaw * 256.0F / 360.0F);
                int packetPitch = MathHelper.floor_float(this.lastRiddenEntity.rotationPitch * 256.0F / 360.0F);
                super.sendPacket(new S18PacketEntityTeleport(this.lastRiddenEntity.getEntityId(), packetX, packetY, packetZ, (byte)packetYaw, (byte)packetPitch));
                System.out.println("HarmonyMod: teleported mount to server location after dismounting");
                this.lastRiddenEntity = null;
                return;
            } else {
                super.sendPacket(packet);
                return;
            }
        }
        /*
        if (!(packet instanceof S14PacketEntity) {
            super.sendPacket(packet);
            return;
        }

        if (this.playerEntity.ridingEntity != null) {
            this.lastRiddenEntity = this.playerEntity.ridingEntity;
        }

        if (this.lastRiddenEntity == null) {
            super.sendPacket(packet);
            return;
        }

        Entity entity = packet.func_149065_a(this.playerEntity.worldObj);
        if (entity.getEntityId() != this.lastRiddenEntity.getEntityId()) {
            super.sendPacket(packet);
            return;
        }

        // Trying to update entity player is or recently rode on
        // prevent update if riding, make sure information is up to date if not
        if (this.playerEntity.ridingEntity != null) {
            return;
        }

        int var4 = this.myEntity.myEntitySize.multiplyBy32AndRound(this.lastRiddenEntity.posZ);
        int var5 = MathHelper.floor_float(this.lastRiddenEntity.rotationYaw * 256.0F / 360.0F);
        int var6 = MathHelper.floor_float(this.lastRiddenEntity.rotationPitch * 256.0F / 360.0F);
        int var7 = var2 - this.lastScaledXPosition;
        int var8 = var3 - this.lastScaledYPosition;
        int var9 = var4 - this.lastScaledZPosition;
        MathHelper.floor_double(this.lastRiddenEntity.posX * 32.0D);
                    tracker.lastScaledYPosition = MathHelper.floor_double(this.lastRiddenEntity.posY * 32.0D);
                    tracker.lastScaledZPosition = MathHelper.floor_double(this.lastRiddenEntity.posZ * 32.0D);

        S14PacketEntity newPacket = null;
        if (packet instanceof S14PacketEntity.S17PacketEntityLookMove) {
            newPacket = new S14PacketEntity.S17PacketEntityLookMove(this.myEntity.getEntityId(), (byte)var7, (byte)var8, (byte)var9, (byte)var5, (byte)var6);
        } else if (packet instanceof S14PacketEntity.S16PacketEntityLook) {
            newPacket = new S14PacketEntity.S16PacketEntityLook(this.myEntity.getEntityId(), (byte)var5, (byte)var6);
        } else if (packet instanceof S14PacketEntity.S15PacketEntityRelMove) {
            newPacket = new S14PacketEntity.S15PacketEntityRelMove(this.myEntity.getEntityId(), (byte)var7, (byte)var8, (byte)var9);
        }*/
    }
}
