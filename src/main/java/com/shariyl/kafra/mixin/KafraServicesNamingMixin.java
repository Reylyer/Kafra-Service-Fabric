package com.shariyl.kafra.mixin;


import com.shariyl.kafra.KafraService;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class KafraServicesNamingMixin extends LivingEntity {
    // Required constructor for Mixin (don't worry about it)
    protected KafraServicesNamingMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void interceptNameTag(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = player.getStackInHand(hand);

        KafraService.LOGGER.warn("Interacting with mob with " + stack);

        if (stack.getItem() == Items.NAME_TAG && stack.getCustomName() != null) {
            KafraService.LOGGER.warn("Using custom name tag");

            if ((Object)this instanceof VillagerEntity villager) {
                KafraService.LOGGER.warn("to villager");


                boolean isKafra = KafraService.kafraEntities.stream()
                        .anyMatch(kafra -> kafra.villagerEntity == villager);
                KafraService.LOGGER.warn("is kafra? " + isKafra);
                if (isKafra) {

                    KafraService.LOGGER.warn("NAMING KAFRA ENTITY");
                    // Set custom name manually
                    String originalName = stack.getName().getString();
                    villager.setCustomName(Text.of("[Kafra Services] " + originalName));

                    // Consume Name Tag unless creative
                    if (!player.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }

//                    // Play sound if you want
//                    world.playSound(null, villager.getBlockPos(), SoundEvents.ENTITY_VILLAGER_YES,
//                            SoundCategory.NEUTRAL, 1.0F, 1.0F);

                    // VERY IMPORTANT: Cancel vanilla behavior
                    cir.setReturnValue(ActionResult.SUCCESS);
                }
            }
        }

        cir.setReturnValue(ActionResult.PASS);
    }
}
