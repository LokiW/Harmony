package com.harmony.harmonymod.horsefix;

import com.harmony.harmonymod.HarmonyNetwork;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

/*
 * Client half of HorseControl: moves the horse the local player is riding.
 */
@SideOnly(Side.CLIENT)
public class HorseControlClient {
    // Whether jump was held last tick, to spot the release that makes a horse jump
    private boolean jumpHeld = false;

    public static void register() {
        HorseControlClient handler = new HorseControlClient();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }

    /*
     * The horse the local player is riding and moving, or null
     */
    private static EntityHorse getControlledHorse() {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player == null || !HorseControl.isRiderControlled(player.ridingEntity)) {
            return null;
        }
        return (EntityHorse) player.ridingEntity;
    }

    /*
     * Runs at the start of every entity update, before it moves toward its last position from the server.
     */
    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!event.entityLiving.worldObj.isRemote || event.entityLiving != getControlledHorse()) {
            return;
        }

        // This client is moving the horse, so drop the position the server last sent. Interpolating
        // one step to where it already is also skips the extra motion damping vanilla applies to
        // entities the server moves.
        EntityHorse horse = (EntityHorse) event.entityLiving;
        horse.setPositionAndRotation2(horse.posX, horse.posY, horse.posZ, horse.rotationYaw, horse.rotationPitch, 1);
    }

    /*
     * Runs after the horse and then its rider have updated, so the rider's input for this tick is known.
     */
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (event.side != Side.CLIENT || event.phase != Phase.END || event.player != player) {
            return;
        }

        EntityHorse horse = getControlledHorse();
        if (horse == null) {
            this.jumpHeld = false;
            return;
        }

        // Vanilla sends the charged jump to the server when jump is released. Start the same jump
        // on this client's horse, EntityHorse performs it on its next update.
        boolean jumpNowHeld = player.movementInput.jump;
        if (this.jumpHeld && !jumpNowHeld) {
            horse.setJumpPower((int)(player.getHorseJumpPower() * 100.0F));
        }
        this.jumpHeld = jumpNowHeld;

        MoveEntityHelper.moveRiddenHorse(horse, player);
        HarmonyNetwork.channel.sendToServer(new MountMoveMessage(horse));
    }

    public static void applyCorrection(MountCorrectionMessage message) {
        EntityHorse horse = getControlledHorse();
        if (horse == null || horse.getEntityId() != message.entityId) {
            return;
        }

        horse.setPosition(message.x, message.y, message.z);
        horse.motionX = 0.0D;
        horse.motionY = 0.0D;
        horse.motionZ = 0.0D;
    }
}
