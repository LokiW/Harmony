package com.harmony.harmonymod.horsefix;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.network.play.client.C03PacketPlayer;

public class NetHandlerPlayAndRideServer extends NetHandlerPlayServer {

    public NetHandlerPlayAndRideServer(MinecraftServer server, NetworkManager netManager, EntityPlayerMP player) {
        super(server, netManager, player);
    }

    /*
     * Override process player which would normally only get player
     * location from the client when not ridding an entity.
     * Change it to set entitly location from client too.
     */
    @Override
    public void processPlayer(C03PacketPlayer playerPacket) {
        double playerX = this.playerEntity.posX;
        double playerY = this.playerEntity.posY;
        double playerZ = this.playerEntity.posZ;

        if (this.playerEntity.ridingEntity != null)
        {
            float playerYaw = this.playerEntity.rotationYaw;
            float playerPitch = this.playerEntity.rotationPitch;

            Entity mount = this.playerEntity.ridingEntity;
            double yOffset = 0.0F;
            double yawOffset = 0.1F;
            if (mount instanceof EntityHorse && ((EntityHorse)mount).isRearing()) {
                yOffset = 0.15F * (double)((EntityHorse)mount).getRearingAmount(0.0F);
                yawOffset = 0.7F * (double)((EntityHorse)mount).getRearingAmount(0.0F);

            }

            double yawX = (double)MathHelper.sin(playerYaw * (float)Math.PI / 180.0F);
            double yawZ = (double)MathHelper.cos(playerYaw * (float)Math.PI / 180.0F);
            double mountY = playerY - (mount.getMountedYOffset() + this.playerEntity.getYOffset() + yOffset);
            mount.setPosition(playerX + (yawOffset * yawX),
                              mountY,
                              playerZ - (yawOffset * yawZ));

            if (mount instanceof EntityLivingBase) {
                ((EntityLivingBase)mount).renderYawOffset = playerYaw;
            }
        }

        super.processPlayer(playerPacket);
    }
}
