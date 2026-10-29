package com.privychat.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.privychat.backend.exception.BadRequestException;
import com.privychat.backend.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);
    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucketName;
    private final int signedUrlExpiration;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SupabaseStorageService(
            @Value("${privychat.storage.supabase.url:}") String supabaseUrl,
            @Value("${privychat.storage.supabase.service-role-key:}") String serviceRoleKey,
            @Value("${privychat.storage.supabase.bucket-name:privychat-attachments}") String bucketName,
            @Value("${privychat.storage.supabase.signed-url-expiration:300}") int signedUrlExpiration) {
        this.supabaseUrl = cleanUrl(supabaseUrl);
        this.serviceRoleKey = serviceRoleKey != null ? serviceRoleKey.trim() : "";
        this.bucketName = bucketName != null && !bucketName.isBlank() ? bucketName.trim() : "privychat-attachments";
        this.signedUrlExpiration = signedUrlExpiration > 0 ? signedUrlExpiration : 300;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    private String cleanUrl(String url) {
        if (url == null || url.isBlank()) return "";
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    public boolean isConfigured() {
        return !supabaseUrl.isBlank() && !serviceRoleKey.isBlank();
    }

    public String getBucketName() {
        return bucketName;
    }

    public int getSignedUrlExpiration() {
        return signedUrlExpiration;
    }

    /**
     * Ensures the private Supabase Storage bucket exists.
     * Creates the bucket if not found with public=false.
     */
    public void ensureBucketExists() {
        if (!isConfigured()) {
            log.warn("Supabase Storage is not configured. Skipping bucket check.");
            return;
        }

        try {
            URI checkUri = URI.create(supabaseUrl + "/storage/v1/bucket/" + bucketName);
            HttpRequest checkRequest = HttpRequest.newBuilder()
                    .uri(checkUri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .GET()
                    .build();

            HttpResponse<String> checkResponse = httpClient.send(checkRequest, HttpResponse.BodyHandlers.ofString());

            if (checkResponse.statusCode() == 200) {
                log.info("Supabase private bucket '{}' is verified and accessible.", bucketName);
                return;
            }

            if (checkResponse.statusCode() == 404) {
                log.info("Bucket '{}' does not exist. Creating private bucket in Supabase...", bucketName);
                URI createUri = URI.create(supabaseUrl + "/storage/v1/bucket");
                String payload = String.format("{\"id\":\"%s\",\"name\":\"%s\",\"public\":false,\"file_size_limit\":52428800}",
                        bucketName, bucketName);

                HttpRequest createRequest = HttpRequest.newBuilder()
                        .uri(createUri)
                        .timeout(Duration.ofSeconds(10))
                        .header("Authorization", "Bearer " + serviceRoleKey)
                        .header("apikey", serviceRoleKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
                if (createResponse.statusCode() == 200 || createResponse.statusCode() == 201) {
                    log.info("Successfully created private Supabase bucket: {}", bucketName);
                } else {
                    log.error("Failed to create bucket: status={} body={}", createResponse.statusCode(), createResponse.body());
                }
            } else {
                log.warn("Unexpected status checking bucket '{}': status={} body={}",
                        bucketName, checkResponse.statusCode(), checkResponse.body());
            }
        } catch (Exception e) {
            log.error("Error communicating with Supabase Storage: {}", e.getMessage(), e);
        }
    }

    /**
     * Uploads encrypted ciphertext bytes to the private bucket in Supabase Storage.
     */
    public void uploadCiphertext(String objectPath, byte[] ciphertext, String mimeType) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IllegalStateException("Supabase Storage credentials are not configured");
        }

        URI uploadUri = URI.create(supabaseUrl + "/storage/v1/object/" + bucketName + "/" + objectPath);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uploadUri)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .header("Content-Type", mimeType != null ? mimeType : "application/octet-stream")
                .header("x-upsert", "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(ciphertext))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            log.error("Supabase Storage upload failed: status={} body={}", response.statusCode(), response.body());
            throw new BadRequestException("Storage upload rejected: " + response.body());
        }

        log.debug("Successfully uploaded {} bytes to Supabase Storage at: {}", ciphertext.length, objectPath);
    }

    /**
     * Streams encrypted ciphertext bytes from the private bucket in Supabase Storage.
     */
    public byte[] downloadCiphertext(String objectPath) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IllegalStateException("Supabase Storage credentials are not configured");
        }

        URI downloadUri = URI.create(supabaseUrl + "/storage/v1/object/authenticated/" + bucketName + "/" + objectPath);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(downloadUri)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 404) {
            throw new ResourceNotFoundException("Attachment ciphertext not found in Supabase Storage: " + objectPath);
        }

        if (response.statusCode() != 200) {
            log.error("Supabase Storage download failed: status={}", response.statusCode());
            throw new RuntimeException("Failed to download from Supabase Storage: HTTP " + response.statusCode());
        }

        return response.body();
    }

    /**
     * Generates a short-lived temporary signed URL for downloading from the private bucket.
     */
    public String createSignedDownloadUrl(String objectPath, int expiresInSeconds) {
        if (!isConfigured()) {
            return null;
        }

        try {
            int expiry = expiresInSeconds > 0 ? expiresInSeconds : signedUrlExpiration;
            URI signUri = URI.create(supabaseUrl + "/storage/v1/object/sign/" + bucketName + "/" + objectPath);
            String payload = String.format("{\"expiresIn\":%d}", expiry);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(signUri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                if (json.has("signedURL")) {
                    String relativeSigned = json.get("signedURL").asText();
                    return supabaseUrl + "/storage/v1" + relativeSigned;
                }
            } else {
                log.warn("Failed to generate signed URL: status={} body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Error creating signed URL for {}: {}", objectPath, e.getMessage());
        }

        return null;
    }

    /**
     * Deletes an object from the private Supabase Storage bucket.
     */
    public void deleteObject(String objectPath) {
        if (!isConfigured()) return;

        try {
            URI deleteUri = URI.create(supabaseUrl + "/storage/v1/object/" + bucketName);
            String payload = String.format("{\"prefixes\":[\"%s\"]}", objectPath);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(deleteUri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Failed to delete object from Supabase Storage: status={} body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Error deleting object from Supabase Storage: {}", e.getMessage());
        }
    }
}

