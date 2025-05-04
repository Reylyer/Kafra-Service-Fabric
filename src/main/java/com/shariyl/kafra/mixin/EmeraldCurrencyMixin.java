package com.shariyl.kafra.mixin;


import net.fabricmc.fabric.mixin.item.ItemAccessor;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Item.class)
public class EmeraldCurrencyMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(Item.Settings settings, CallbackInfo ci) {
//        if ((Object)this == Items.EMERALD) {
//            // Replace the components map with a new one that includes custom MAX_STACK_SIZE
//            var builder = new DataComponentMap.Builder(((Item)(Object)this).getComponents());
//            builder.set(DataComponentTypes.MAX_STACK_SIZE, 9999);
//            ((ItemAccessor)this).setComponents(builder.build());
//        }
    }
}