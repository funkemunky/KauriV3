package dev.brighten.ac.logging;

import lombok.Builder;
import lombok.Getter;
import me.mat1337.loader.utils.json.JSONException;
import me.mat1337.loader.utils.json.JSONObject;

import java.util.UUID;

@Getter
@Builder
public class Log {
    public UUID uuid;
    private float vl;
    private long time;
    private String data;
    private String checkId;
    public String toJson() {
        JSONObject object = new JSONObject();

        try {
            object.put("uuid", uuid.toString());
            object.put("vl", vl);
            object.put("time", time);
            object.put("data", data);
            object.put("checkId", checkId);

            return object.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
