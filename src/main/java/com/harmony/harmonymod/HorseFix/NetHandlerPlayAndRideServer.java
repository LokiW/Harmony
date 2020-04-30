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
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0CPacketInput;

public class NetHandlerPlayAndRideServer extends NetHandlerPlayServer {
    public MinecraftServer server;

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
        Entity mount = this.playerEntity.ridingEntity;
        boolean hasMoved = playerPacket.func_149466_j();

        if (mount == null || !hasMoved) {
            super.processPlayer(playerPacket);
            return;
        }

        WorldServer world = this.server.worldServerForDimension(this.playerEntity.dimension);

        double packetX = playerPacket.func_149464_c();
        double packetY = playerPacket.func_149467_d();
        double packetZ = playerPacket.func_149472_e();
        float packetYaw = this.playerEntity.rotationYaw;
        float packetPitch = this.playerEntity.rotationPitch;

        if (playerPacket.func_149463_k()) {
            packetYaw = playerPacket.func_149462_g();
            packetPitch = playerPacket.func_149470_h();
        }

        this.playerEntity.setPositionAndRotation(packetX, packetY, packetZ, packetYaw, packetPitch);
        this.playerEntity.updateRidden();

        super.processPlayer(playerPacket);

        /*
        double mountY = packetY - (mount.getMountedYOffset() + this.playerEntity.getYOffset());

        mount.setPosition(packetX, mountY, packetZ);
        super.processPlayer(playerPacket);*/

        /*
        mount.updateRiderPosition();

        System.out.println("HarmonyMod: player packet: " + packetX +
                            ", " + packetY + "," + packetZ + " has moved: " + hasMoved);

        this.playerEntity.setPositionAndRotation(packetX, packetY, packetZ, packetYaw, packetPitch);

        // Set mount position too
        double yawOffset = 0.0F;
        double yOffset = 0.0F;
        if (mount instanceof EntityHorse && ((EntityHorse)mount).isRearing()) {
            yawOffset = 0.7F * (double)((EntityHorse)mount).getRearingAmount(0.0F);
            yOffset = 0.15F * (double)((EntityHorse)mount).getRearingAmount(0.0F);
        }

        double yawX = (double)MathHelper.sin(packetYaw * (float)Math.PI / 180.0F);
        double yawZ = (double)MathHelper.cos(packetYaw * (float)Math.PI / 180.0F);
        double mountY = this.playerEntity.posY - (mount.getMountedYOffset() + this.playerEntity.getYOffset() + yOffset);
        double mountX = this.playerEntity.posX - (yawOffset * yawX);
        double mountZ = this.playerEntity.posZ + (yawOffset * yawZ);
        mount.setPosition(mountX, mountY, mountZ);

        if (mount instanceof EntityLivingBase) {
            ((EntityLivingBase)mount).renderYawOffset = packetYaw;
        }

        world.updateEntity(this.playerEntity);
        world.updateEntity(mount);

        super.processPlayer(playerPacket);
        */

        //System.out.println("HarmonyMod: player post packet: "+ this.playerEntity.posX +
        //                    ", " + this.playerEntity.posY + "," + this.playerEntity.posZ);
        /*
        if (mount != null)
        {
            this.playerEntity.ridingEntity = mount;

            float playerYaw = this.playerEntity.rotationYaw;
            float playerPitch = this.playerEntity.rotationPitch;

            double yawOffset = 0.0F;
            double yOffset = 0.0F;
            if (mount instanceof EntityHorse && ((EntityHorse)mount).isRearing()) {
                yawOffset = 0.7F * (double)((EntityHorse)mount).getRearingAmount(0.0F);
                yOffset = 0.15F * (double)((EntityHorse)mount).getRearingAmount(0.0F);
            }

            double yawX = (double)MathHelper.sin(playerYaw * (float)Math.PI / 180.0F);
            double yawZ = (double)MathHelper.cos(playerYaw * (float)Math.PI / 180.0F);
            double mountY = this.playerEntity.posY - (mount.getMountedYOffset() + this.playerEntity.getYOffset() + yOffset);
            double mountX = this.playerEntity.posX - (yawOffset * yawX);
            double mountZ = this.playerEntity.posZ + (yawOffset * yawZ);
            System.out.println("HarmonyMod: setting horse position to " + mountX +
                                ", " + mountY + ", " + mountZ + " based on player");
            mount.setPosition(this.playerEntity.posX - (yawOffset * yawX),
                              mountY,
                              this.playerEntity.posZ + (yawOffset * yawZ));

            if (mount instanceof EntityLivingBase) {
                ((EntityLivingBase)mount).renderYawOffset = playerYaw;
            }
        }*/

    }
}
