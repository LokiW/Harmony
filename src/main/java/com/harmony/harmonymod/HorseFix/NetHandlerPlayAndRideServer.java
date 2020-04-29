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
import net.minecraft.network.play.client.C0CPacketInput;

public class NetHandlerPlayAndRideServer extends NetHandlerPlayServer {

    public NetHandlerPlayAndRideServer(MinecraftServer server, NetworkManager netManager, EntityPlayerMP player) {
        super(server, netManager, player);
        System.out.println("HarmonyMod: updated riding for player " + player);
    }

    @Override
    public void processInput(C0CPacketInput inputPacket) {
        Entity mount = this.playerEntity.ridingEntity;
        this.playerEntity.ridingEntity = null;

        super.processInput(inputPacket);

        this.playerEntity.ridingEntity = mount;
    }


    /*
     * Override process player which would normally only get player
     * location from the client when not ridding an entity.
     * Change it to set entitly location from client too.
     */
    @Override
    public void processPlayer(C03PacketPlayer playerPacket) {
        Entity mount = this.playerEntity.ridingEntity;
        this.playerEntity.ridingEntity = null;
        double var5 = playerPacket.func_149464_c();
        double var7 = playerPacket.func_149467_d();
        double var9 = playerPacket.func_149472_e();
        double var13 = playerPacket.func_149471_f() - playerPacket.func_149467_d();

        System.out.println("HarmonyMod: player packet: " + var5 +
                            ", " + var7 + "," + var9 + "," + var13);

        super.processPlayer(playerPacket);

        this.playerEntity.ridingEntity = mount;
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
