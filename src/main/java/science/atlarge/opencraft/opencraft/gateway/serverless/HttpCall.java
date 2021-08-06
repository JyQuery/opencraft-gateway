package science.atlarge.opencraft.opencraft.gateway.serverless;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpCall {
    protected URL url;

    public HttpCall(String urlStr) {
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            System.out.println("Invalid Azure Function URL: " + urlStr);
        }
    }

    public String requestWithPayload(String payload) {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setDoInput(true);
            con.connect();

            OutputStream os = con.getOutputStream();
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
            pw.write(payload);
            pw.close();

            return getResult(con);
        } catch (Exception e) {
            System.out.println("HttpCall exception = " + e);
        }
        return null;
    }

    protected String getResult(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        StringBuffer sb = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }
}