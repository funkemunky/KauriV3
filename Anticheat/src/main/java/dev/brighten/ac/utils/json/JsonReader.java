//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.brighten.ac.utils.json;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class JsonReader {
    public JsonReader() {
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();

        int cp;
        while((cp = rd.read()) != -1) {
            sb.append((char)cp);
        }

        return sb.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        URLConnection connection = (new URL(url)).openConnection();
        InputStream is = connection.getInputStream();

        JSONObject var6;
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            var6 = json;
        } finally {
            is.close();
        }

        return var6;
    }
}
