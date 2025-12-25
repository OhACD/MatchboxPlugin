package com.ohacd.matchbox.game.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ohacd.matchbox.Matchbox;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

/**
 * Sends a get request to modrinth API to check the project latest version
 */
public class CheckProjectVersion {
    private final Matchbox plugin;

    /**
     * Creates the version checker for the plugin.
     *
     * @param plugin plugin instance used for scheduling and logging
     */
    public CheckProjectVersion(Matchbox plugin) {
        this.plugin = plugin;
    }

    /**
     * Asynchronously checks the latest project version from the remote API and invokes the callback.
     *
     * @param callback consumer receiving the latest version string when available
     */
    public void checkLatestVersion(Consumer<String> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/umketzP5/version");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonArray array = JsonParser.parseString(response.toString()).getAsJsonArray();
                JsonObject latest = array.get(0).getAsJsonObject();
                String latestVersion = latest.get("version_number").getAsString();
                callback.accept(latestVersion);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
