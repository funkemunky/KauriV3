package dev.brighten.ac.logging;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.data.APlayer;
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
                .build();

        database = Nitrite.builder()
                .loadModule(storeModule)
                .loadModule(new JacksonMapperModule())
                .openOrCreate("anticheatuser", "anticheat_dbpass590129*");

        logRepo = database.getRepository(Log.class);
    }

    public void insertLog(APlayer player, CheckData checkData, float vl, long time, String data) {
        logRepo.insert(Log.builder()
                .uuid(player.getUuid())
                .checkId(checkData.checkId())
                .vl(vl)
                .data(data)
                .time(time)
                .build());
    }

    public void getLogs(UUID uuid, Consumer<List<Log>> logConsumer) {
        getLogs(uuid, 2000, logConsumer);
    }

    public void getLogs(UUID uuid, int limit, Consumer<List<Log>> logConsumer) {
        getLogs(uuid, limit, 0, logConsumer);
    }

    public void getLogs(UUID uuid, int limit, int skip, Consumer<List<Log>> logsConsumer) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            var list = logRepo.find(where("uuid").eq(uuid), new FindOptions().skip(skip).limit(limit)).toList();

            logsConsumer.accept(list);
        });
    }


    public void getLogs(UUID uuid, String checkId, int limit, Consumer<List<Log>> logConsumer) {
        getLogs(uuid, checkId, limit, 0, logConsumer);
    }

    public void getLogs(UUID uuid, String checkId, int limit, int skip, Consumer<List<Log>> logsConsumer) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {
           var list = logRepo.find(Filter.and(where("uuid").eq(uuid), where("checkId").eq(checkId)),
                   new FindOptions().skip(skip).limit(limit)).toList();

           logsConsumer.accept(list);
        });
    }

    public void getLogs(UUID uuid, String checkId, long timeBefore, long timeAfter,
                        Consumer<List<Log>> logsConsumer) {
        getLogs(uuid, checkId, timeBefore, timeAfter, 500, 0, logsConsumer);
    }
    public void getLogs(UUID uuid, String checkId, long timeBefore, long timeAfter, int limit, int skip,
                        Consumer<List<Log>> logsConsumer) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            var list = logRepo.find(Filter
                    .and(where("uuid").eq(uuid),
                            where("checkId").eq(checkId),
                            where("time").lte(timeAfter),
                            where("time").gte(timeBefore)),
                    new FindOptions().skip(skip).limit(limit)).toList();

            logsConsumer.accept(list);
        });
    }

    public void getRecentLogs(int limit, Consumer<List<Log>> logsConsumer) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            var list = logRepo.find(new FindOptions()
                    .thenOrderBy("time", SortOrder.Descending)
                    .limit(limit))
                    .toList();

            logsConsumer.accept(list);
        });
    }

    public void shutDown() {
        logRepo = null;
        database.close();
    }
}
