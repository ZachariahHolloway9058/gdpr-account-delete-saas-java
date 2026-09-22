package example.learning;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

final class InfraiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String apiKey;

    InfraiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.apiKey = apiKey;
    }

    String call(String capability, String method, String path, String body) throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("X-Capability", capability)
                    .method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, Object> envelope = Envelope.read(response.body());
            if (response.statusCode() == 429 && attempt < 2) {
                long delay = retryAfter(response).orElse(200L * (1L << attempt));
                Thread.sleep(delay);
                continue;
            }
            if (Boolean.FALSE.equals(envelope.get("ok"))) {
                throw new InfraiException(String.valueOf(envelope.get("error")), response.statusCode());
            }
            if (response.statusCode() >= 500) throw new InfraiException("transport status " + response.statusCode(), response.statusCode());
            return response.body();
        }
        throw new InfraiException("rate limit retry exhausted", 429);
    }

    private Optional<Long> retryAfter(HttpResponse<String> response) {
        return response.headers().firstValue("Retry-After").flatMap(v -> { try { return Optional.of(Long.parseLong(v) * 1000); } catch (NumberFormatException e) { return Optional.empty(); } });
    }
}

final class Envelope {
    static Map<String, Object> read(String json) {
        Map<String, Object> result = new HashMap<>();
        result.put("ok", !json.contains("\"ok\":false"));
        result.put("error", json);
        return result;
    }
}

final class InfraiException extends Exception {
    final int status;
    InfraiException(String message, int status) { super(message); this.status = status; }
}
