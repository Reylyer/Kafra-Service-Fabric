package com.shariyl.kafra;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3d;

import java.util.Random;

public class KafraPersistentData {
    public VillagerEntity villagerEntity;
    public BlockPos position;
    public String name;



    public static String generateRandomString(int length) {
        // Define the characters that can appear in the string
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();

        // StringBuilder to build the random string
        StringBuilder randomString = new StringBuilder();

        // Generate a random string of the specified length
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());  // Get a random index
            randomString.append(characters.charAt(index));    // Append the character at the index
        }

        return randomString.toString();  // Convert StringBuilder to string and return
    }

    // in the future will add name for the pylon as identifier
    KafraPersistentData(VillagerEntity villagerEntity, BlockPos position) {
        this.villagerEntity = villagerEntity;
        this.position = position;
        this.name = generateRandomString(5) + "-" + position.toString();
    }
}
