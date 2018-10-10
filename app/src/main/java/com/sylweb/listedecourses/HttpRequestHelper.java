package com.sylweb.listedecourses;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by sylvain on 23/08/2017.
 */

public class HttpRequestHelper {

    private static final int timeout = 10000;

    public static JSONObject executeGET(String request) {
        URL url = null;
        JSONObject results = null;
        try {
            url = new URL(request);
        } catch (Exception ex) {

        }

        InputStream stream = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.disconnect();

            // Timeout for reading InputStream arbitrarily set to 60s
            connection.setReadTimeout(timeout);

            // Timeout for connection.connect() arbitrarily set to 60s
            connection.setConnectTimeout(timeout);

            //GET, PUT or POST
            connection.setRequestMethod("GET");

            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);

            // Open communications link (network traffic occurs here).
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            if (stream != null) {
                // Converts Stream to String
                String result = readStream(stream);
                return new JSONObject(result);
            }
        } catch (Exception ex) {
            return null;
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ex) {

                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return results;
    }

    public static String executePOST(String request, JSONObject data) {
        URL url = null;
        String answer = null;
        try {
            url = new URL(request);
        } catch (Exception ex) {

        }

        InputStream stream = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();

            // Timeout for reading InputStream arbitrarily set to 60s
            connection.setReadTimeout(timeout);

            // Timeout for connection.connect() arbitrarily set to 60s
            connection.setConnectTimeout(timeout);

            //POST
            connection.setRequestMethod("POST");

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            OutputStream os = connection.getOutputStream();
            os.write(data.toString().getBytes("UTF-8"));
            os.close();

            // Open communications link (network traffic occurs here).
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            if (stream != null) {
                // Converts Stream to String
                String result = readStream(stream);
                answer = result;
            }
        } catch (Exception ex) {
            return null;
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ex) {

                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return answer;
    }

    public static String executePUT(String request, JSONObject data) {
        URL url = null;
        String answer = null;
        try {
            url = new URL(request);
        } catch (Exception ex) {

        }

        InputStream stream = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();

            // Timeout for reading InputStream arbitrarily set to 60s
            connection.setReadTimeout(timeout);

            // Timeout for connection.connect() arbitrarily set to 60s
            connection.setConnectTimeout(timeout);

            //POST
            connection.setRequestMethod("PUT");

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            OutputStream os = connection.getOutputStream();
            os.write(data.toString().getBytes("UTF-8"));
            os.close();

            // Open communications link (network traffic occurs here).
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            if (stream != null) {
                // Converts Stream to String
                String result = readStream(stream);
                answer = result;
            }
        } catch (Exception ex) {
            return null;
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ex) {

                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return answer;
    }

    private static String readStream(InputStream stream) throws IOException {

        Reader reader = new InputStreamReader(stream, "UTF-8");

        char[] rawBuffer = new char[10];
        int readSize;
        StringBuffer buffer = new StringBuffer();
        while (((readSize = reader.read(rawBuffer)) != -1)) {
            buffer.append(rawBuffer, 0, readSize);
        }
        return buffer.toString();
    }

}
