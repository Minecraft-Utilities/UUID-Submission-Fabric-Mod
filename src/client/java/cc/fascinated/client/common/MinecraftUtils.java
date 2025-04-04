package cc.fascinated.client.common;

import cc.fascinated.client.McUtilsClient;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MinecraftUtils {
    /**
     * Gets the current players, excluding NPCs and fake players.
     *
     * @return the current real players
     */
    public static List<GameProfile> getCurrentPlayers() {
        List<GameProfile> players = new ArrayList<>();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (player != null) {
            for (PlayerListEntry entry : player.networkHandler.getPlayerList()) {
                // Skip if this is an NPC/fake player
                if (isNPC(entry)) {
                    McUtilsClient.LOGGER.debug("Skipping NPC/fake player: {}", entry.getProfile().getName());
                    continue;
                }
                players.add(entry.getProfile());
            }
        }
        return players;
    }

    /**
     * Checks if a player is an NPC or fake player.
     *
     * @param entry The player list entry to check
     * @return true if the player is an NPC/fake player, false otherwise
     */
    private static boolean isNPC(PlayerListEntry entry) {
        GameProfile profile = entry.getProfile();

        // Basic checks
        if (profile == null) return true;
        if (profile.getName() == null || profile.getName().isEmpty()) return true;

        // UUID checks
        try {
            UUID uuid = profile.getId();
            if (uuid == null) return true;

            // Check for "00000000-0000-0000-0000-000000000000" or similar
            if (uuid.equals(new UUID(0, 0))) return true;

            // Check UUID version (most real players use version 4)
            if (uuid.version() != 4) return true;

            // Check for UUIDs generated from names (some NPCs use these)
            if (uuid.variant() != 2) return true; // RFC 4122 variant
        } catch (IllegalArgumentException e) {
            return true;
        }

        // Ping checks (NPCs often have abnormal ping values)
        int ping = entry.getLatency();
        if (ping < 0 || ping > 1000) return true; // Adjusted threshold

        // Check for common NPC name patterns
        String name = profile.getName();
        if (name.matches(".*\\[.*].*")) return true; // Names with brackets
        if (name.matches(".*[0-9]{4,}.*")) return true; // Names with many numbers
        if (name.length() > 16) return true; // Minecraft names can't be longer than 16 chars

        // Check for skin signature (many NPCs don't have proper skin signatures)
        if (profile.getProperties().get("textures").isEmpty()) return true;

        return false;
    }
}