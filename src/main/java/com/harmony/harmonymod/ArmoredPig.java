package com.harmony.harmonymod;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

/*
 * Basics for adding armor to animals
 */
public class ArmoredPig {
   
    /*
     * Event for testing armor value
     * 
    @SubscribeEvent
    public void onEntityGetHurt(LivingHurtEvent e) {
        if(e.entity instanceof EntityPig) {
            EntityPig pig = (EntityPig) e.entity;
            int armored = pig.getTotalArmorValue();
            System.out.println("you hit a pig with armor value" + armored);
        }
    }
    */


    @SubscribeEvent
    public void onEntityInteractEvent(EntityInteractEvent e) {
        Entity e_target = e.target;
        EntityPlayer player = e.entityPlayer;

        if (e_target instanceof EntityPig) {
            EntityPig target = (EntityPig) e_target;
            int current_item_slot = player.inventory.currentItem;
            ItemStack cur_stack = player.inventory.getCurrentItem();
            
            //Return armor to hand if exists 
            if (cur_stack == null) {
                ItemStack target_armor = target.getEquipmentInSlot(0);
                if (target_armor != null) {
                    player.inventory.setInventorySlotContents(current_item_slot, target_armor);
                    target.setCurrentItemOrArmor(0, null);
                }
            
            //Put item on pig if it's valid
            } else if (cur_stack.getItem() instanceof ItemArmor) {
                ItemStack target_armor = target.getEquipmentInSlot(0);
                if (target_armor == null) {
                    target.setCurrentItemOrArmor(0, cur_stack);
                    player.inventory.setInventorySlotContents(current_item_slot, null);
                }
                
            }
        }
    }
}
