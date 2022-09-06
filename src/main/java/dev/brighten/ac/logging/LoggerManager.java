package dev.brighten.ac.logging;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.logging.sql.Query;
import dev.brighten.ac.logging.sql.ResultSetIterator;
import dev.brighten.ac.utils.RunUtils;
import dev.brighten.log.socket.OutRequest;
import dev.brighten.log.socket.RequestType;
import dev.brighten.log.utils.EncryptionUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Deque;
import java.util.LinkedList;


public class LoggerManager {

    private final Deque<Log> logList = new LinkedList<>();
    private KeyPair keys;
    private PublicKey encryptKey;

    /*
     * Structure of Log
     * UUID hashcode (INT32),
     */
    public void init() {
        // Starting up H2
        Anticheat.INSTANCE.getLogger().info("Generating RSA...");
        keys = EncryptionUtils.Companion.geneateRsa();
        try {
            Anticheat.INSTANCE.getLogger().info("Sending init request to log server...");
            OutRequest request = new OutRequest(RequestType.INITIALIZE.toString(), "localhost", null);

            String encoded = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
            System.out.println(encoded);
            request.getObjects().writeUTF(encoded);

            request.write();

            Anticheat.INSTANCE.getLogger().info("Sent!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Anticheat.INSTANCE.getSocketManager().onInputReceived(event -> {
            if(event.getRequest().getHeader().equals(RequestType.INITIALIZE.toString())) {
                System.out.println("Received key from server!");
                ObjectInputStream objectStream = event.getRequest().getObjects();
                try {
                    String publicKeyString = objectStream.readUTF();

                    encryptKey = EncryptionUtils.Companion.publicKeyFromString(publicKeyString);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        RunUtils.taskTimerAsync(() -> {
            System.out.println("Running ping!");

            OutRequest request = new OutRequest(RequestType.PING.toString(),
                    "localhost",
                    encryptKey);

            try {
                request.getObjects().writeLong(System.currentTimeMillis());
                request.write();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 80, 40);

        Anticheat.INSTANCE.socketManager.onInputReceived(event -> {
            if(event.getRequest().getHeader().equals(RequestType.PING.toString())) {
                ObjectInputStream objects = event.getRequest().getObjects(keys.getPrivate());

                try {
                    long serverTime = objects.readLong();
                    long extense = objects.readLong();

                    long ping = System.currentTimeMillis() - serverTime + extense;

                    System.out.println("Ping: " + ping + "ms");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
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

    }
}
