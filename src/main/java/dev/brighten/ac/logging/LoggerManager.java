package dev.brighten.ac.logging;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.data.APlayer;
import dev.brighten.ac.utils.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;


public class LoggerManager {

    private final Deque<Log> logList = new LinkedList<>();
    private String license;

    /*
     * Structure of Log
     * UUID hashcode (INT32),
     */
    public void init() {
        // Starting up H2
        license = Anticheat.INSTANCE.getPluginInstance().getConfig().getString("license");

        AtomicLong lastWrite = new AtomicLong();
        Anticheat.INSTANCE.getScheduler().scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            if(logList.size() > 0 && (now - lastWrite.get() > 10000L || logList.size() > 600)) {
                try {
                    WebSocket socket =  new WebSocketFactory().createSocket("ws://port.funkemunky.cc/chat").connect();

                    System.out.println("Writing logs");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);

                    oos.writeUTF("LOG_WRITE");
                    oos.writeUTF(license());

                    int i = 0;
                    while(logList.size() > 0 && i++ < 400) {
                        Log log = logList.poll();

                        if(log != null) {
                            oos.writeUTF(log.toJson());
                        }
                    }

                    if(i == 0) {
                        logList.clear();
                    }

                    System.out.println("Wrote " + i + " logs;" + logList.size());
                    lastWrite.set(now);
                    oos.close();
                    socket.sendBinary(baos.toByteArray());
                    baos.close();

                    socket.disconnect();
                } catch (IOException | WebSocketException e) {
                    e.printStackTrace();
                }
            }
        }, 200, 200, TimeUnit.MILLISECONDS);
    }

    private String license() {
        return license;
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


    public void getLogs(UUID uuid, Consumer<List<Log>> logConsumer) {
        getLogs(uuid, 500, 0, logConsumer);
    }
    public void getLogs(UUID uuid, int limit, int skip, Consumer<List<Log>> logsConsumer) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            try {
                WebSocket socket = createSocket(logsConsumer).connect();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeUTF("LOG_REQ_UUID");
                oos.writeUTF(license());

                oos.writeUTF(uuid.toString());
                oos.writeInt(skip);
                oos.writeInt(limit);

                oos.close();
                socket.sendBinary(baos.toByteArray());
            } catch(WebSocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void getLogs(UUID uuid, String checkId, Consumer<List<Log>> logConsumer) {
        getLogs(uuid, checkId, 500, 0, logConsumer);
    }

    public void getLogs(UUID uuid, String checkId, int limit, int skip, Consumer<List<Log>> logsConsumer) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            try {
                WebSocket socket = createSocket(logsConsumer).connect();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeUTF("LOG_REQ_UUID_CHECK");
                oos.writeUTF(license());
                oos.writeUTF(uuid.toString());
                oos.writeUTF(checkId);
                oos.writeInt(skip);
                oos.writeInt(limit);


                oos.close();
                socket.sendBinary(baos.toByteArray());
            } catch(WebSocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void getLogs(UUID uuid, String checkId, long timeBefore, long timeAfter,
                        Consumer<List<Log>> logsConsumer) {
        getLogs(uuid, checkId, timeBefore, timeAfter, 500, 0, logsConsumer);
    }
    public void getLogs(UUID uuid, String checkId, long timeBefore, long timeAfter, int limit, int skip,
                        Consumer<List<Log>> logsConsumer) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            try {
                WebSocket socket = createSocket(logsConsumer).connect();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeUTF("LOG_REQ_UUID_CHECK_TIME");
                oos.writeUTF(license());
                oos.writeUTF(checkId);
                oos.writeLong(timeBefore);
                oos.writeLong(timeAfter);
                oos.writeInt(skip);
                oos.writeInt(limit);


                oos.writeUTF(uuid.toString());
                oos.close();
                socket.sendBinary(baos.toByteArray());
            } catch(WebSocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void getRecentLogs(int limit, Consumer<List<Log>> logsConsumer) {
        Anticheat.INSTANCE.getScheduler().execute(() -> {
            try {
                WebSocket socket = createSocket(logsConsumer).connect();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeUTF("LOG_REQ_UUID_CHECK");
                oos.writeUTF(license());
                oos.writeInt(limit);

                oos.close();
                socket.sendBinary(baos.toByteArray());
            } catch (WebSocketException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private WebSocket createSocket(Consumer<List<Log>> logsConsumer) throws IOException {
        return new WebSocketFactory().createSocket("ws://port.funkemunky.cc/chat")
                .addListener(new WebSocketAdapter() {

                    @Override
                    public void onBinaryMessage(WebSocket websocket, byte[] data) throws Exception {
                        ByteArrayInputStream bais = new ByteArrayInputStream(data);
                        ObjectInputStream ois = new ObjectInputStream(bais);

                        System.out.println("Received!: " + ois.available());
                        List<Log> logs = new ArrayList<>();
                        while(ois.available() > 0) {
                            String logString = ois.readUTF();
                            JSONObject logObject = new JSONObject(logString);

                            logs.add(Log.builder()
                                    .vl((float)logObject.getDouble("vl"))
                                    .checkId(logObject.getString("checkId"))
                                    .data(logObject.getString("data"))
                                    .time(logObject.getLong("time"))
                                    .uuid(UUID.fromString(logObject.getString("uuid"))).build());
                        }

                        logsConsumer.accept(logs);
                        websocket.disconnect();
                    }
                });
    }
    public void shutDown() {

    }
}
