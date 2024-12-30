package dev.brighten.ac.logging;

import dev.brighten.ac.utils.json.JSONException;
import dev.brighten.ac.utils.json.JSONObject;
import lombok.*;
import org.dizitart.no2.repository.annotations.Entity;
import org.dizitart.no2.repository.annotations.Id;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Log {
    @Id
    private long id;
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
