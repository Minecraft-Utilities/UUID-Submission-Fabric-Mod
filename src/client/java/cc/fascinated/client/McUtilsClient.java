package cc.fascinated.client;

import cc.fascinated.client.common.MinecraftUtils;
import cc.fascinated.client.common.UUIDSubmission;
import com.mojang.authlib.GameProfile;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Environment(EnvType.CLIENT)
public class McUtilsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mcutils");

    private static final long DELAY_MS = 5_000; // 5 seconds delay for first run
    private static final long PERIOD_MS = 30_000; // 30 seconds period

    private static final Lock LOCK = new ReentrantLock();
    private static final List<UUID> toSendUUIDs = new ArrayList<>();
    private static final List<UUID> alreadySentUUIDs = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        // Finds the current players then adds their UUIDs to the list of UUIDs to send.
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().world == null || MinecraftClient.getInstance().player == null) {
                        return;
                    }

                    List<GameProfile> players = MinecraftUtils.getCurrentPlayers();
                    for (GameProfile player : players) {
                        UUID uuid = player.getId();
                        // Check all possible duplicate conditions
                        if (!uuid.toString().startsWith("00000000") && // Ignore bedrock players
                                !alreadySentUUIDs.contains(uuid) &&       // Not already sent
                                !toSendUUIDs.contains(uuid)) {            // Not already queued
                            toSendUUIDs.add(uuid);
                        }
                    }
                });
            }
        }, DELAY_MS, 2_000);

        // Periodically sends the UUIDs to the server.
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().world == null || MinecraftClient.getInstance().player == null) {
                        return;
                    }

                    if (LOCK.tryLock()) {
                        try {
                            if (!toSendUUIDs.isEmpty()) {
                                UUIDSubmission submission = new UUIDSubmission(
                                        MinecraftClient.getInstance().player.getUuid(),
                                        toSendUUIDs.toArray(new UUID[0])
                                );
                                submitUUIDs(submission);
                                toSendUUIDs.clear();
                            }
                        } finally {
                            LOCK.unlock();
                        }
                    }
                });
            }
        }, DELAY_MS, PERIOD_MS);
    }

    /**
     * Submits the UUIDs to the server.
     *
     * @param submission the submission object
     */
    public static void submitUUIDs(UUIDSubmission submission) {
        Thread thread = new Thread(() -> {
//            HttpResponse<JsonNode> response = Unirest.post("http://localhost:45000/player/submit-uuids") // The endpoint to send the UUIDs to
            HttpResponse<JsonNode> response = Unirest.post("https://mc.fascinated.cc/api/player/submit-uuids") // The endpoint to send the UUIDs to
                    .header("Content-Type", "application/json")
                    .body(submission)
                    .asJson();

            // Handle response
            if (response.isSuccess()) {
                LOGGER.info("Successfully sent {} UUIDs to McUtils", response.getBody().getObject().getString("added"));

                // Add the UUIDs to the alreadySentUUIDs list
                alreadySentUUIDs.addAll(Arrays.asList(submission.getUuids()));
            } else {
                LOGGER.warn("Error: {}", response.getStatusText());
            }
        });
        thread.start();
    }
}