package com.keepers.photoorganiser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GooglePhotosPickerApi {
    private static final String SESSIONS_URL =
            "https://photospicker.googleapis.com/v1/sessions";
    private static final Pattern PICKER_URI = Pattern.compile(
            "\\\"pickerUri\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private GooglePhotosPickerApi() {}

    static String createSession(String accessToken) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(SESSIONS_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(15_000);
        connection.setDoOutput(true);
        try (OutputStream output = connection.getOutputStream()) {
            output.write("{}".getBytes(StandardCharsets.UTF_8));
        }
        int status = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                status >= 200 && status < 300 ? connection.getInputStream()
                        : connection.getErrorStream(), StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        for (String line; (line = reader.readLine()) != null;) body.append(line);
        reader.close();
        connection.disconnect();
        if (status < 200 || status >= 300)
            throw new IOException("Google Photos Picker returned HTTP " + status);
        return parsePickerUri(body.toString());
    }

    static String parsePickerUri(String response) {
        Matcher match = PICKER_URI.matcher(response);
        if (!match.find())
            throw new IllegalArgumentException("Picker session did not include a picker URI");
        String pickerUri = match.group(1).replace("\\/", "/");
        if (!pickerUri.startsWith("https://"))
            throw new IllegalArgumentException("Picker session did not include a picker URI");
        return pickerUri;
    }
}
