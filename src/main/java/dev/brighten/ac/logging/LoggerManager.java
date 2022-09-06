package dev.brighten.ac.logging;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import dev.brighten.ac.Anticheat;
import dev.brighten.ac.check.CheckData;
import dev.brighten.ac.data.APlayer;
import me.mat1337.loader.utils.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

        Anticheat.INSTANCE.getScheduler().scheduleAtFixedRate(() -> {
            if(logList.size() > 0) {
                try {
                    WebSocket socket =  new WebSocketFactory().createSocket("ws://port.funkemunky.cc/chat").connect();

                    System.out.println("Writing logs");
                    Log log;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);

                    oos.writeUTF("LOG_WRITE");
                    oos.writeUTF(license());

                    int i = 0;
                    while((log = logList.poll()) != null && i++ < 100) {
                        oos.writeUTF(log.toJson());
                    }

                    System.out.println("Wrote " + i + " logs");
                    oos.close();
                    socket.sendBinary(baos.toByteArray());
                    baos.close();

                    socket.disconnect();
                } catch (IOException | WebSocketException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
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

    public void getLogs(UUID uuid, Consumer<List<Log>> logsConsumer) {
        try {
            WebSocket socket = new WebSocketFactory().createSocket("ws://port.funkemunky.cc/chat").connect();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            oos.writeUTF("LOG_REQ_UUID");
            oos.writeUTF(license());

            oos.writeUTF(uuid.toString());
            oos.close();
            System.out.println("Sending binary");
            socket.sendBinary(baos.toByteArray()).addListener(new WebSocketAdapter() {

                @Override
                public void onBinaryMessage(WebSocket websocket, byte[] data) throws Exception {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    ObjectInputStream ois = new ObjectInputStream(bais);

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

            if(socket.isOpen()) System.out.println("Open");
            else System.out.println("Not open!");
        } catch(WebSocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void shutDown() {

    }
}
