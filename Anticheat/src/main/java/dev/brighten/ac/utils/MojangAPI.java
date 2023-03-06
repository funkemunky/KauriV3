package dev.brighten.ac.utils;

import dev.brighten.ac.utils.json.JSONException;
import dev.brighten.ac.utils.json.JSONObject;
import dev.brighten.ac.utils.json.JsonReader;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class MojangAPI {

    public static Optional<String> getUsername(UUID uuid) {
        try {
            JSONObject object = JsonReader.readJsonFromUrl("https://funkemunky.cc/mojang/name?uuid=" + uuid.toString());

            if(object.getBoolean("success")) {
                return Optional.of(object.getString("name"));
            } else {
                return Optional.empty();
            }
        } catch(JSONException | IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static Optional<UUID> getUUID(String name) {
        try {
            JSONObject object = JsonReader.readJsonFromUrl("https://funkemunky.cc/mojang/uuid?name=" + name);

            if(object.getBoolean("success")) {
                return Optional.of(UUID.fromString(object.getString("uuid")));
            } else {
                return Optional.empty();
            }
        } catch(JSONException | IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static UUID formatFromMojangUUID(String mojangUUID) {
        String uuid = "";
        for(int i = 0; i <= 31; i++) {
            uuid = uuid + mojangUUID.charAt(i);
            if(i == 7 || i == 11 || i == 15 || i == 19) {
                uuid = uuid + "-";
            }
        }

        return UUID.fromString(uuid);
    }
}
