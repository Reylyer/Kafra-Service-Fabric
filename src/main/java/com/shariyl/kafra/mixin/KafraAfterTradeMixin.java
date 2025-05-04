package com.shariyl.kafra.mixin;


import com.shariyl.kafra.KafraPersistentData;
import com.shariyl.kafra.KafraService;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.village.TradeOffer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Mixin(MerchantEntity.class)
public class KafraAfterTradeMixin {

    @Inject(method = "onSellingItem",
            at = @At("RETURN"))
    private void onTrade(ItemStack stack, CallbackInfo ci) {
        MerchantEntity merchant = (MerchantEntity) (Object) this;
        VillagerEntity villager = (VillagerEntity) merchant;
        PlayerEntity player = merchant.getCustomer();

        KafraPersistentData kafraPersistentData = KafraService.getKafraPersistentData(villager);


        // Getting a copy
        @Nullable var data = stack.get(DataComponentTypes.CUSTOM_DATA);

        if (data != null) {
            NbtCompound value = data.copyNbt();

            Optional<Double> teleportTargetX = value.getDouble("x");
            Optional<Double> teleportTargetY = value.getDouble("y");
            Optional<Double> teleportTargetZ = value.getDouble("z");


            List<TradeOffer> offers = villager.getOffers();

            for (TradeOffer offer : offers) {
                // Check if the stack in the trade matches the given stack (either buying or selling item)
                if (isItemInTrade(offer, stack)) {
                    // Now you can proceed with your logic for handling the trade
                    // For example, check if the player has enough emeralds, etc.

                    // In this case, the logic can continue with the trade or cancel it depending on other conditions

                    assert player != null;
                    revertTradeInputs(player);


                    var playerEmeraldCount = playerEmeraldCount(player);

                    // this is just kafra style that WILL ONLY USE EMERALD CURRENCY
                    var priceTradeInEmerald =  offer.getFirstBuyItem().count();

                    if (playerEmeraldCount >= priceTradeInEmerald) {
                        deductEmeralds(player, priceTradeInEmerald);

                        player.setPos(teleportTargetX.get(), teleportTargetY.get(), teleportTargetZ.get());
                    }
                }
            }
        }
    }

    // Helper method to check if the ItemStack is involved in the trade offer
    @Unique
    private boolean isItemInTrade(TradeOffer offer, ItemStack stack) {
        // Check if the selling item matches the stack
        var itemSelling = offer.getSellItem();

        var sellingLabel = Objects.requireNonNull(itemSelling.getCustomName()).getString();
        var buyingLabel = Objects.requireNonNull(stack.getCustomName()).getString();

        KafraService.LOGGER.warn(sellingLabel);
        KafraService.LOGGER.warn(buyingLabel);

        return Objects.equals(sellingLabel, buyingLabel);
//        return ItemStack.areItemsEqual(stack, itemSelling);// Return false if no match is found
    }

    private int playerEmeraldCount(PlayerEntity player) {
        int totalEmeralds = 0;

        // Iterate through the entire inventory and sum up emeralds
        for (ItemStack stack : player.getInventory()) {
            if (stack.getItem() == Items.EMERALD) {
                totalEmeralds += stack.getCount(); // Add the count of emeralds in this stack
            }
        }

        // Check if the player has enough emeralds
        return totalEmeralds;
    }

    // Helper method to deduct emeralds from the player's inventory
    private void deductEmeralds(PlayerEntity player, int emeraldCount) {
        int remainingEmeralds = emeraldCount;

        // Iterate through the inventory and remove emeralds until the required amount is deducted
        for (ItemStack stack : player.getInventory()) {
            if (stack.getItem() == Items.EMERALD) {
                int emeraldStackCount = stack.getCount();

                if (emeraldStackCount <= remainingEmeralds) {
                    remainingEmeralds -= emeraldStackCount;
                    stack.setCount(0); // Remove all emeralds in this stack
                } else {
                    stack.decrement(remainingEmeralds); // Remove the required amount
                    remainingEmeralds = 0;
                    break; // Stop once we've deducted the required amount
                }
            }

            if (remainingEmeralds == 0) {
                break; // Stop iterating once we've deducted the required emeralds
            }
        }
    }

    private void revertTradeInputs(PlayerEntity player) {
        ScreenHandler screenHandler = player.currentScreenHandler;

        if (screenHandler instanceof MerchantScreenHandler merchantScreenHandler) {
            // Slot 0 (first buy item)
            moveSlotItemBackToInventory(player, merchantScreenHandler, 0);
            // Slot 1 (second buy item)
            moveSlotItemBackToInventory(player, merchantScreenHandler, 1);
        }
    }

    private void moveSlotItemBackToInventory(PlayerEntity player, MerchantScreenHandler merchantScreenHandler, int slotIndex) {
        ItemStack stack = merchantScreenHandler.getSlot(slotIndex).getStack();

        if (!stack.isEmpty()) {
            // Try to insert into player's inventory
            boolean inserted = player.getInventory().insertStack(stack.copy()); // copy() is important

            if (inserted) {
                // If successful, clear the slot
                merchantScreenHandler.getSlot(slotIndex).setStack(ItemStack.EMPTY);
                merchantScreenHandler.sendContentUpdates(); // Refresh the screen
            } else {
                System.out.println("Failed to return item from trade slot " + slotIndex + " to inventory.");
                // If the inventory is full, you could drop it instead, if you want
                player.dropItem(stack, false);
                merchantScreenHandler.getSlot(slotIndex).setStack(ItemStack.EMPTY);
                merchantScreenHandler.sendContentUpdates();
            }
        }
    }
}
