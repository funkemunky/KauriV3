package dev.brighten.ac.utils;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.json.JSONException;
import dev.brighten.ac.utils.json.JSONObject;
import dev.brighten.ac.utils.json.JsonReader;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class MojangAPI {

    public static Optional<String> getUsername(UUID uuid) {
        try {
            JSONObject object = JsonReader.readJsonFromUrl("https://funkemunky.cc/mojang/name?uuid=" + uuid.toString());

            if(object.has("name")) {
                return Optional.of(object.getString("name"));
            } else {
                return Optional.empty();
            }
        } catch(JSONException | IOException e) {
            Anticheat.INSTANCE.getLogger().log(
                    Level.SEVERE,
                    String.format("Could not get the username for %s", uuid),
                    e);
            return Optional.empty();
        }
    }

    public static Optional<UUID> getUUID(String name) {
        try {
            JSONObject object = JsonReader.readJsonFromUrl("https://funkemunky.cc/mojang/uuid?name=" + name);

            if(object.has("uuid")) {
                return Optional.of(UUID.fromString(object.getString("uuid")));
            } else {
                return Optional.empty();
            }
        } catch(JSONException | IOException e) {
            Anticheat.INSTANCE.getLogger().log(
                    Level.SEVERE,
                    String.format("Could not get the uuid for %s", name),
                    e);
            return Optional.empty();
        }
    }
}
