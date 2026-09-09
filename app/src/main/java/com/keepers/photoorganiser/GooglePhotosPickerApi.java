package com.keepers.photoorganiser;

import android.net.Uri;
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
    private static final Pattern ID = Pattern.compile(
            "\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private GooglePhotosPickerApi() {}

    record PickerSession(String id, String pickerUri) {}

    static PickerSession createSession(String accessToken) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(SESSIONS_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(15_000);
        connection.setDoOutput(true);
        try (OutputStream output = connection.getOutputStream()) {
            output.write("{\"pickingConfig\":{\"maxItemCount\":\"1\"}}"
                    .getBytes(StandardCharsets.UTF_8));
        }
        return parseSession(readResponse(connection));
    }

    static boolean selectionIsComplete(String response) {
        return response.matches("(?s).*\\\"mediaItemsSet\\\"\\s*:\\s*true.*");
    }

    static boolean selectionIsComplete(String accessToken, String sessionId) throws IOException {
        HttpURLConnection connection = authorizedGet(SESSIONS_URL + "/" + Uri.encode(sessionId),
                accessToken);
        return selectionIsComplete(readResponse(connection));
    }

    static String firstMediaId(String accessToken, String sessionId) throws IOException {
        HttpURLConnection connection = authorizedGet(
                "https://photospicker.googleapis.com/v1/mediaItems?sessionId="
                        + Uri.encode(sessionId) + "&pageSize=1", accessToken);
        return parseFirstMediaId(readResponse(connection));
    }

    static PickerSession parseSession(String response) {
        Matcher id = ID.matcher(response);
        Matcher uri = PICKER_URI.matcher(response);
        if (!id.find() || !uri.find())
            throw new IllegalArgumentException("Picker session did not include a picker URI");
        String pickerUri = uri.group(1).replace("\\/", "/");
        if (!pickerUri.startsWith("https://"))
            throw new IllegalArgumentException("Picker session did not include a picker URI");
        return new PickerSession(id.group(1), pickerUri);
    }

    static String parseFirstMediaId(String response) {
        Matcher id = ID.matcher(response);
        if (!id.find()) throw new IllegalArgumentException("No selected media item was returned");
        return id.group(1);
    }

    static Uri originalPhotoUri(String mediaId) {
        return Uri.parse("https://photos.google.com/lr/photo").buildUpon()
                .appendPath(mediaId).build();
    }

    private static HttpURLConnection authorizedGet(String url, String accessToken)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(15_000);
        return connection;
    }

    private static String readResponse(HttpURLConnection connection) throws IOException {
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
        return body.toString();
    }
}
