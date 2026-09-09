package com.keepers.photoorganiser;

import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    private static final Pattern BASE_URL = Pattern.compile(
            "\\\"baseUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private GooglePhotosPickerApi() {}

    record PickerSession(String id, String pickerUri) {}
    record PickedMedia(String id, String displayUrl) {}

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

    static PickedMedia firstMedia(String accessToken, String sessionId) throws IOException {
        HttpURLConnection connection = authorizedGet(
                "https://photospicker.googleapis.com/v1/mediaItems?sessionId="
                        + Uri.encode(sessionId) + "&pageSize=1", accessToken);
        return parseFirstMedia(readResponse(connection));
    }

    static PickerSession parseSession(String response) {
        Matcher id = ID.matcher(response);
        Matcher uri = PICKER_URI.matcher(response);
        if (!id.find() || !uri.find())
            throw new IllegalArgumentException("Picker session did not include a picker URI");
        String pickerUri = uri.group(1).replace("\\/", "/");
        if (!pickerUri.startsWith("https://"))
            throw new IllegalArgumentException("Picker session did not include a picker URI");
        return new PickerSession(id.group(1), pickerUri.replaceFirst("/+$", "")
                + "/autoclose");
    }

    static PickedMedia parseFirstMedia(String response) {
        Matcher id = ID.matcher(response);
        Matcher baseUrl = BASE_URL.matcher(response);
        if (!id.find() || !baseUrl.find())
            throw new IllegalArgumentException("No selected photo was returned");
        return new PickedMedia(id.group(1), baseUrl.group(1).replace("\\/", "/")
                + "=w1200-h1200");
    }

    static Bitmap downloadDisplayBitmap(String accessToken, PickedMedia media)
            throws IOException {
        HttpURLConnection connection = authorizedGet(media.displayUrl(), accessToken);
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IOException("Google Photos image returned HTTP " + status);
        }
        Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
        connection.disconnect();
        if (bitmap == null) throw new IOException("Google Photos returned an unreadable image");
        return bitmap;
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
