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

    // Class to simplify serialization
    public static class SerializableKafraData {
        public String name;
        public int x, y, z;

        public SerializableKafraData(String name, BlockPos pos) {
            this.name = name;
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }
    }

    public SerializableKafraData toSerializable() {
        return new SerializableKafraData(name, position);
    }

    // in the future will add name for the pylon as identifier
    KafraPersistentData(VillagerEntity villagerEntity, BlockPos position, String name) {
        this.villagerEntity = villagerEntity;
        this.position = position;
        this.name = name;// generateRandomString(5) + "-" + position.toString();
    }

    KafraPersistentData(VillagerEntity villagerEntity, BlockPos position) {
        this(villagerEntity, position, "[Kafra Services]");
    }
}
