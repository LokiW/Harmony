package com.harmony.harmonymod.horsefix;

import com.harmony.harmonymod.HarmonyNetwork;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

/*
 * Smooth horse riding.
 *
 * In vanilla the server moves a ridden horse and the rider's client only sees it through
 * position packets, so the horse constantly jitters and snaps back. Instead, the rider's
 * client moves the horse (HorseControlClient) and sends the result here every tick. The server
 * applies that move with normal collisions, so other players, fall damage and suffocation
 * still work, and corrects the client if they disagree.
 */
public class HorseControl {
    // Furthest a horse may move in a single update (squared), same limit vanilla uses for players
    private static final double MAX_MOVE_SQ = 100.0D;
    // How far the server's result may be from the client's (squared) before the client is corrected
    private static final double MAX_ERROR_SQ = 0.0625D;
    // Distance to check below the horse when the client says it is standing on the ground
    private static final double GROUND_PROBE = 0.0625D;

    public static void register() {
        FMLCommonHandler.instance().bus().register(new HorseControl());
    }

    /*
     * Whether this mount is moved by its rider's client instead of the server
     */
    public static boolean isRiderControlled(Entity mount) {
        if (!(mount instanceof EntityHorse)) {
            return false;
        }
        EntityHorse horse = (EntityHorse) mount;
        return horse.isTame() && horse.isHorseSaddled() && horse.riddenByEntity instanceof EntityPlayer;
    }

    /*
     * Stop the server from moving rider controlled horses on its own.
     */
    @SubscribeEvent
    public void onWorldTick(WorldTickEvent event) {
        if (event.side != Side.SERVER) {
            return;
        }

        for (Object o : event.world.playerEntities) {
            EntityPlayer player = (EntityPlayer) o;
            Entity mount = player.ridingEntity;
            if (!isRiderControlled(mount)) {
                continue;
            }

            if (event.phase == Phase.START) {
                // The client still sends its movement input, which EntityHorse would use to walk
                // the server's copy of the horse as well.
                player.moveForward = 0.0F;
                player.moveStrafing = 0.0F;
            }

            // Keep the server's copy still between client updates. Clearing it before entity
            // tracking also avoids sending velocity packets that would fight the client's motion.
            mount.motionX = 0.0D;
            mount.motionY = 0.0D;
            mount.motionZ = 0.0D;
        }
    }

    /*
     * Apply a horse position sent by its rider's client
     */
    public static void handleMove(EntityPlayerMP player, MountMoveMessage message) {
        Entity mount = player.ridingEntity;
        if (mount == null || mount.getEntityId() != message.entityId || !isRiderControlled(mount)) {
            // Usually just a mount/dismount that the client hasn't heard about yet
            return;
        }

        double dx = message.x - mount.posX;
        double dy = message.y - mount.posY;
        double dz = message.z - mount.posZ;
        if (dx * dx + dy * dy + dz * dz > MAX_MOVE_SQ) {
            System.out.println("HarmonyMod: " + player.getCommandSenderName() + "'s horse moved too quickly, resetting it");
            sendCorrection(player, mount);
            return;
        }

        mount.moveEntity(dx, dy, dz);
        if (message.onGround && !mount.onGround) {
            // A horse resting on the ground doesn't move vertically, so check below it for the
            // server to register it as landed (and apply fall damage).
            mount.moveEntity(0.0D, -GROUND_PROBE, 0.0D);
        }
        if (mount.onGround) {
            // EntityHorse only clears this during its own movement, which is skipped here.
            // Without it the server's horse would only jump (and play the jump sound) once.
            ((EntityHorse) mount).setHorseJumping(false);
        }

        dx = message.x - mount.posX;
        dy = message.y - mount.posY;
        dz = message.z - mount.posZ;
        if (dx * dx + dy * dy + dz * dz > MAX_ERROR_SQ) {
            sendCorrection(player, mount);
        }
    }

    private static void sendCorrection(EntityPlayerMP player, Entity mount) {
        HarmonyNetwork.channel.sendTo(new MountCorrectionMessage(mount), player);
    }
}
