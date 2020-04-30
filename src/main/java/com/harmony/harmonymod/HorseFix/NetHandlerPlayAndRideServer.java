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
import java.lang.Math;

public class NetHandlerPlayAndRideServer extends NetHandlerPlayServer {
    public MinecraftServer server;

    //TEMP
    public double x;
    public double y;
    public double z;

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

        if (mount == null) {
            System.out.println("HarmonyMod: Player Not Mounted");
            super.processPlayer(playerPacket);
            return;
        }

        WorldServer world = this.server.worldServerForDimension(this.playerEntity.dimension);

        double packetX = playerPacket.func_149464_c();
        double minBoundingBoxY = playerPacket.func_149467_d();
        double packetZ = playerPacket.func_149472_e();
        double packetY = playerPacket.func_149471_f();

        boolean hasMoved = playerPacket.func_149466_j();
        if (!hasMoved || packetY == -999.0D || minBoundingBoxY == -999.0D || minBoundingBoxY >= 0.0) {
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

        this.x = packetX; this.y = packetY; this.z = packetZ;
        // this.setPlayerLocation(packetX, minBoundingBoxY, packetZ, packetYaw, packetPitch);
        mount.setPosition(packetX, minBoundingBoxY - mount.getMountedYOffset() - this.playerEntity.getYOffset(), packetZ);

        // world.updateEntity(this.playerEntity);
        //this.playerEntity.updateRidden();
        world.updateEntity(mount);

        super.processPlayer(new C03PacketPlayer.C06PacketPlayerPosLook(packetX,
					minBoundingBoxY,
					packetY,
					packetZ,
					packetYaw,
					packetPitch,
					playerPacket.func_149465_i()));

    }
}
