package dev.brighten.ac.logging;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.data.APlayer;
import lombok.extern.slf4j.Slf4j;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.FindOptions;
import org.dizitart.no2.common.SortOrder;
import org.dizitart.no2.common.mapper.JacksonMapperModule;
import org.dizitart.no2.filters.Filter;
import org.dizitart.no2.mvstore.MVStoreModule;
import org.dizitart.no2.repository.ObjectRepository;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.dizitart.no2.filters.FluentFilter.where;


@Slf4j
public class LoggerManager {

    private final File dbDir = new File(Anticheat.INSTANCE.getDataFolder(), "database");
    private final File dbFile = new File(dbDir, "logs.db");
    /*
     * Structure of Log
     * UUID hashcode (INT32),
     */

    private Nitrite database = null;
    private ObjectRepository<Log> logRepo;

    public void init() {
        if(!dbDir.exists()) {
            if(!dbDir.mkdirs())
                throw new RuntimeException("Could not create database directory in plugin folder!");
        }

        MVStoreModule storeModule = MVStoreModule.withConfig()
                .filePath(dbFile)
                .compress(true)
                .autoCommit(true)
                .autoCommitBufferSize(12)
                .cacheSize(64)
                .build();

        database = Nitrite.builder()
                .loadModule(storeModule)
                .loadModule(new JacksonMapperModule())
                .openOrCreate("anticheatuser", "anticheat_dbpass590129*");

        logRepo = database.getRepository(Log.class);
    }

    public void insertLog(APlayer player, CheckData checkData, float vl, long time, String data) {
        var writeResult = logRepo.insert(Log.builder()
                .uuid(player.getUuid())
                .checkId(checkData.checkId())
                .checkName(checkData.name())
                .vl(vl)
                .data(data)
                .time(time)
                .build());

        try {
            Anticheat.INSTANCE.getLogger().info("Inserted log: " + writeResult.getAffectedCount());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void getLogs(UUID uuid, int limit, Consumer<List<Log>> logConsumer) {
        getLogs(uuid, limit, 0, logConsumer);
    }

    public void getLogs(UUID uuid, int limit, int skip, Consumer<List<Log>> logsConsumer) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            var cursor = logRepo.find(where("uuid").eq(uuid), new FindOptions()
                    .limit(limit).skip(skip)
                    .thenOrderBy("time", SortOrder.Descending));

            List<Log> logs = cursor.toList();

            logsConsumer.accept(logs);
        });
    }

    public void getLogs(UUID uuid, String checkId, int limit, int skip, Consumer<List<Log>> logsConsumer) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {
           var cursor = logRepo.find(Filter.and(where("uuid").eq(uuid), where("checkId").eq(checkId)),
                   new FindOptions().skip(skip).limit(limit));

           List<Log> logs = cursor.toList();

           logsConsumer.accept(logs);
        });
    }

    public void shutDown() {
        logRepo = null;
        database.close();
    }
}
