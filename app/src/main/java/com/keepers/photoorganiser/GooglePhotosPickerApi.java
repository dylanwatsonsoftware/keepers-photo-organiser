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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GooglePhotosPickerApi {
    private static final String SESSIONS_URL =
            "https://photospicker.googleapis.com/v1/sessions";
    private static final Pattern PICKER_URI = Pattern.compile(
            "\\\"pickerUri\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ID = Pattern.compile(
            "\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern MEDIA_ITEM = Pattern.compile(
            "\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"(?:(?!\\\"id\\\").)*?"
                    + "\\\"baseUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"", Pattern.DOTALL);
    private static final Pattern NEXT_PAGE_TOKEN = Pattern.compile(
            "\\\"nextPageToken\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private GooglePhotosPickerApi() {}

    record PickerSession(String id, String pickerUri) {}
    record PickedMedia(String id, String displayUrl) {}
    record MediaPage(List<PickedMedia> items, String nextPageToken) {}

    static PickerSession createSession(String accessToken) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(SESSIONS_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(15_000);
        connection.setDoOutput(true);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(sessionRequestBody()
                    .getBytes(StandardCharsets.UTF_8));
        }
        return parseSession(readResponse(connection));
    }

    static String sessionRequestBody() {
        return "{\"pickingConfig\":{\"maxItemCount\":\"100\"}}";
    }

    static boolean selectionIsComplete(String response) {
        return response.matches("(?s).*\\\"mediaItemsSet\\\"\\s*:\\s*true.*");
    }

    static boolean selectionIsComplete(String accessToken, String sessionId) throws IOException {
        HttpURLConnection connection = authorizedGet(SESSIONS_URL + "/" + Uri.encode(sessionId),
                accessToken);
        return selectionIsComplete(readResponse(connection));
    }

    static List<PickedMedia> allMedia(String accessToken, String sessionId) throws IOException {
        ArrayList<PickedMedia> result = new ArrayList<>();
        String pageToken = null;
        do {
            String url = "https://photospicker.googleapis.com/v1/mediaItems?sessionId="
                    + Uri.encode(sessionId) + "&pageSize=100";
            if (pageToken != null) url += "&pageToken=" + Uri.encode(pageToken);
            MediaPage page = parseMediaPage(readResponse(authorizedGet(url, accessToken)));
            result.addAll(page.items());
            pageToken = page.nextPageToken();
        } while (pageToken != null && !pageToken.isBlank());
        if (result.isEmpty()) throw new IllegalArgumentException("No selected photos were returned");
        return List.copyOf(result);
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
        List<PickedMedia> items = parseMediaPage(response).items();
        if (items.isEmpty())
            throw new IllegalArgumentException("No selected photo was returned");
        return items.get(0);
    }

    static MediaPage parseMediaPage(String response) {
        ArrayList<PickedMedia> items = new ArrayList<>();
        Matcher media = MEDIA_ITEM.matcher(response);
        while (media.find()) items.add(new PickedMedia(media.group(1),
                media.group(2).replace("\\/", "/") + "=w1200-h1200"));
        Matcher next = NEXT_PAGE_TOKEN.matcher(response);
        return new MediaPage(List.copyOf(items), next.find() ? next.group(1) : null);
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
