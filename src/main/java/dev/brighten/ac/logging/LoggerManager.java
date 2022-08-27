package dev.brighten.ac.logging;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.logging.sql.ExecutableStatement;
import dev.brighten.ac.logging.sql.MySQL;
import dev.brighten.ac.logging.sql.Query;
import dev.brighten.ac.logging.sql.ResultSetIterator;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class LoggerManager {

    private final Deque<Log> logList = new LinkedList<>();

    /*
     * Structure of Log
     * UUID hashcode (INT32),
     */
    public void init() {
        // Starting up H2
        MySQL.initH2();

        Query.prepare("CREATE TABLE IF NOT EXISTS `logs` (" +
                "`id` INT NOT NULL AUTO_INCREMENT," +
                "`uuid` INT NOT NULL," +
                "`check` VARCHAR(32) NOT NULL," +
                "`vl` FLOAT NOT NULL," +
                "`data` MEDIUMTEXT NOT NULL," +
                "`time` TIMESTAMP NOT NULL," +
                "PRIMARY KEY (`id`)" +
                ");").execute();

        Anticheat.INSTANCE.getScheduler().scheduleAtFixedRate(() -> {
            if(logList.size() > 0) {
                synchronized (logList) {
                    final StringBuilder values = new StringBuilder();

                    List<Object> objectsToInsert = new ArrayList<>();
                    Log log = null;
                    int amount = 0;
                    while((log = logList.poll()) != null) {
                        objectsToInsert.add(log.getUuid().hashCode());
                        objectsToInsert.add(log.getCheckId());
                        objectsToInsert.add(log.getVl());
                        objectsToInsert.add(log.getData());
                        objectsToInsert.add(new Timestamp(log.getTime()));

                        if(++amount >= 150) break;
                    }

                    for (int i = 0; i < amount; i++) {
                        values.append(i > 0 ? "," : "").append("(?, ?, ?, ?, ?)");
                    }

                    ExecutableStatement statement = Query.prepare("INSERT INTO `logs` " +
                                    "(`uuid`,`check`,`vl`,`data`,`time`) VALUES" + values.toString())
                            .append(objectsToInsert.toArray());

                    statement.execute();

                    objectsToInsert.clear();
                }
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    public void insertLog(APlayer player, CheckData checkData, float vl, long time, String data) {
        logList.add(Log.builder()
                .uuid(player.getUuid())
                .checkId(checkData.checkId())
                .vl(vl)
                .data(data)
                .time(time)
                .build());
    }

    public void runQuery(String query, ResultSetIterator iterator) {
        Query.prepare(query).execute(iterator);
    }

    public void shutDown() {
        MySQL.shutdown();
    }
}
