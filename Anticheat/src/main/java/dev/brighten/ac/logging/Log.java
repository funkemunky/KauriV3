package dev.brighten.ac.logging;

import lombok.*;
import org.dizitart.no2.collection.NitriteId;
import org.dizitart.no2.index.IndexType;
import org.dizitart.no2.repository.annotations.Entity;
import org.dizitart.no2.repository.annotations.Id;
import org.dizitart.no2.repository.annotations.Index;

import java.io.Serializable;
import java.util.UUID;

@Entity(value = "logs",
indices = {
        @Index(fields = {"uuid"}, type = IndexType.NON_UNIQUE),
        @Index(fields = {"uuid", "checkId"}, type = IndexType.NON_UNIQUE),
        @Index(fields = {"time"}, type = IndexType.NON_UNIQUE),
        @Index(fields = {"uuid", "checkId", "time"}, type = IndexType.NON_UNIQUE)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Log implements Serializable {
    @Id
    private NitriteId id;
    public UUID uuid;
    private float vl;
    private long time;
    private String data;
    private String checkId;
    private String checkName;
}
