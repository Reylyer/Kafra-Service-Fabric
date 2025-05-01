package com.shariyl.kafra;

import com.shariyl.kafra.mixin.BeaconMixin;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;


public class KafraService implements ModInitializer {
	public static final String MOD_ID = "kafra-service";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ArrayList<KafraPersistentData> kafraEntities = new ArrayList<>();

	public static KafraPersistentData getKafraPersistentData(VillagerEntity villagerEntity) {
		Optional<KafraPersistentData> matchingEntity = kafraEntities.stream()
				.filter(x -> x.villagerEntity == villagerEntity) // Filter by condition
				.findFirst(); // Get the first match or empty Optional if none found

        return matchingEntity.orElse(null);
	}

	public static KafraPersistentData getKafraPersistentData(BlockPos pos) {
		Optional<KafraPersistentData> matchingEntity = kafraEntities.stream()
				.filter(x -> x.position.isWithinDistance(pos, 1)) // Filter by condition
				.findFirst(); // Get the first match or empty Optional if none found

		return matchingEntity.orElse(null);
	}


	public static ArrayList<KafraPersistentData> newlyCreatedKafraPersistentData = new ArrayList<>();


	public boolean dirtyLock = false;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.


		ServerEntityEvents.ENTITY_LOAD.register((entity, server) -> {
			if (entity instanceof VillagerEntity villager) {
				if (villager.getCustomName() != null && villager.getCustomName().getString().startsWith("[Kafra Services]")) {
					var pos = new BlockPos((int) villager.getX(), (int) villager.getY(), (int)villager.getZ());

					var kafraIntersect = getKafraPersistentData(pos);

					if (kafraIntersect == null) {
						KafraPersistentData kafraPersistentData = new KafraPersistentData(
								villager,
								pos
						);
						kafraEntities.add(
								kafraPersistentData
						);

						LOGGER.warn("LOADED KAFRA+ " + pos);
						LOGGER.warn("KAFRA SIZE CHECKING DUPES " + kafraEntities.size());
					} else {
						LOGGER.warn("KAFRA EXIST, OVERRIDING IT " + pos);
						kafraIntersect.villagerEntity = villager;
					}
				}
			}

		});


		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClient() && player.isSneaking()) {  // Check if the player is crouching
				BlockPos pos = hitResult.getBlockPos();
				BlockState state = world.getBlockState(pos);

				if (state.getBlock() == Blocks.BEACON) {  // Check if the block is a beacon
					BlockEntity be = world.getBlockEntity(pos);
					if (be instanceof BeaconBlockEntity beacon) {

						int level = ((BeaconMixin) beacon).getBeaconLevel();
						System.out.println("Beacon level: " + level);

						if (level > 0) {
							// Beacon is active
							boolean hasKafraNearby = !world.getEntitiesByClass(
									VillagerEntity.class,
									new Box(pos).expand(5), // Check a 5-block radius
									villager -> villager.isAlive() && villager.getCustomName() != null
											&& villager.getCustomName().getString().startsWith("[Kafra Services]")
							).isEmpty();

							if (!hasKafraNearby) {
								// spawn Kafra
								spawnKafra(world, pos);
								return ActionResult.SUCCESS;  // Block the event to prevent further processing
							}
						}
					}
				}
			}



			return ActionResult.PASS;  // Allow normal behavior for other interactions
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!world.isClient()) {
				if (dirtyLock) {
					dirtyLock = false;
					return ActionResult.PASS;
				}

				ItemStack stack = player.getStackInHand(hand);

				var contains = kafraEntities.stream().anyMatch(x -> x.villagerEntity == entity);
				if (contains) {
					var thisKafraEntity = (VillagerEntity) entity;

					if (stack.getItem() == Items.NAME_TAG && stack.getCustomName() != null) {

						boolean isKafra = KafraService.kafraEntities.stream()
								.anyMatch(kafra -> kafra.villagerEntity == thisKafraEntity);
						if (isKafra) {
							// Set custom name manually
							String originalName = stack.getName().getString();
							String newName = "[Kafra Services] " + originalName;

							thisKafraEntity.setCustomName(Text.of(newName));

							var kafraPersistentData = getKafraPersistentData(thisKafraEntity);

							kafraPersistentData.name = newName;

							// Consume Name Tag unless creative
							if (!player.getAbilities().creativeMode) {
								stack.decrement(1);
							}
						}
						dirtyLock = true;
						return ActionResult.PASS;
					}

					TradeOfferList tradeOffers = new TradeOfferList();

					var thisKafraPos = thisKafraEntity.getPos();

					// Create the trade offer
					for (KafraPersistentData otherKafra : kafraEntities) {
						// Don't trade teleportation to yourself
						if (otherKafra.villagerEntity == thisKafraEntity) continue;

						// Calculate distance
						double dx = thisKafraPos.x - otherKafra.position.getX();
						double dz = thisKafraPos.z - otherKafra.position.getZ();
						double distance = Math.sqrt(dx * dx + dz * dz);

						// Convert to chunk distance
						int chunkDistance = (int) (distance / 16.0);

						// Price: chunkDistance / 15, ceiled
						int price = (int) Math.ceil(chunkDistance / 15.0);
						if (price < 1) price = 1;  // Minimum 1 emerald maybe?

						// Create the trade offer
						TradedItem payment = new TradedItem(Items.EMERALD, price);
						ItemStack teleportTicket = new ItemStack(Items.PAPER);
						teleportTicket.set(DataComponentTypes.CUSTOM_NAME, Text.literal(otherKafra.villagerEntity.getCustomName().getString() + ": (" + otherKafra.position.getX() + ", " + otherKafra.position.getY() + ", " + otherKafra.position.getZ() + ")" ));

						NbtCompound customData = new NbtCompound();
						customData.putDouble("x", otherKafra.position.getX());
						customData.putDouble("y", otherKafra.position.getY());
						customData.putDouble("z", otherKafra.position.getZ());

						NbtComponent component = NbtComponent.of(customData);

						teleportTicket.set(DataComponentTypes.CUSTOM_DATA, component);

						TradeOffer tradeOffer = new TradeOffer(payment, teleportTicket, 99999, 5, 0.05F);
						tradeOffers.add(tradeOffer);

					}

					// Set the villager's trades
					thisKafraEntity.setOffers(tradeOffers);

					thisKafraEntity.setCustomer(player);
					thisKafraEntity.sendOffers(player, thisKafraEntity.getDisplayName(), thisKafraEntity.getVillagerData().level());
				}
			}

			dirtyLock = true;
			return ActionResult.PASS;  // Allow normal behavior for other interactions
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			kafraEntities.forEach(data -> {
				data.villagerEntity.setCustomName(Text.literal(data.name));
			});
		});

	}

	private void spawnKafra(World world, BlockPos beaconPos) {
		// Spawn the villager (Kafra) near the beacon
		if (world instanceof ServerWorld) {
			ServerWorld serverWorld = (ServerWorld) world;
			VillagerEntity kafra = new VillagerEntity(EntityType.VILLAGER, serverWorld);

			kafra.setInvulnerable(true);

			kafra.setAiDisabled(true);

			kafra.setCustomName(Text.literal("[Kafra Services]"));

			var kafraPosition = new Vector3d(beaconPos.getX() + 1.0, beaconPos.getY() + 1.0, beaconPos.getZ() + 1.0);

			// Place the Kafra near the beacon
			//kafra.setPersistent();
			kafra.refreshPositionAndAngles(kafraPosition.x, kafraPosition.y, kafraPosition.z, 0.0f, 0.0f);

			// Spawn the entity in the world
			serverWorld.spawnEntity(kafra);

			// Optional: Send a broadcast message
			serverWorld.getServer().getPlayerManager().broadcast(Text.literal("Kafra has arrived at beacon: " + kafraPosition.toString()), false);
		}
	}
}