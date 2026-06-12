package com.bmr.gateway.client;

import com.bmr.gateway.api.CreateOrderInput;
import com.bmr.gateway.api.CreateUserInput;
import com.bmr.gateway.api.UpdateOrderStatusRequest;
import com.bmr.gateway.model.Health;
import com.bmr.gateway.model.Order;
import com.bmr.gateway.model.User;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DownstreamClient {
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;
    private final String usersServiceUrl;
    private final String ordersServiceUrl;

    public DownstreamClient(
            JsonMapper jsonMapper,
            @Value("${app.services.users-url}") String usersServiceUrl,
            @Value("${app.services.orders-url}") String ordersServiceUrl
    ) {
        this.jsonMapper = jsonMapper;
        this.usersServiceUrl = removeTrailingSlash(usersServiceUrl);
        this.ordersServiceUrl = removeTrailingSlash(ordersServiceUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public Health health() {
        return new Health(
                "ok",
                downstreamHealth(usersServiceUrl + "/health"),
                downstreamHealth(ordersServiceUrl + "/health")
        );
    }

    public List<User> users() {
        return get(usersServiceUrl + "/users", new TypeReference<>() {
        });
    }

    public User user(Long id) {
        return get(usersServiceUrl + "/users/" + id, new TypeReference<>() {
        });
    }

    public List<User> usersBatch(List<Long> ids) {
        String idsParam = ids.stream()
                .distinct()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");

        if (idsParam.isBlank()) {
            return List.of();
        }

        return get(usersServiceUrl + "/users/batch?ids=" + encode(idsParam), new TypeReference<>() {
        });
    }

    public User createUser(CreateUserInput input) {
        return post(usersServiceUrl + "/users", input, new TypeReference<>() {
        });
    }

    public List<Order> orders() {
        return get(ordersServiceUrl + "/orders", new TypeReference<>() {
        });
    }

    public Order order(Long id) {
        return get(ordersServiceUrl + "/orders/" + id, new TypeReference<>() {
        });
    }

    public List<Order> ordersByUser(Long userId) {
        return get(ordersServiceUrl + "/orders?userId=" + userId, new TypeReference<>() {
        });
    }

    public Order createOrder(CreateOrderInput input) {
        return post(ordersServiceUrl + "/orders", input, new TypeReference<>() {
        });
    }

    public Order updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        return patch(ordersServiceUrl + "/orders/" + id + "/status", request, new TypeReference<>() {
        });
    }

    private String downstreamHealth(String url) {
        try {
            Map<String, String> response = get(url, new TypeReference<>() {
            });
            return response.getOrDefault("status", "unknown");
        } catch (RuntimeException ex) {
            return "down";
        }
    }

    private <T> T get(String url, TypeReference<T> type) {
        return exchange("GET", url, null, type);
    }

    private <T> T post(String url, Object body, TypeReference<T> type) {
        return exchange("POST", url, body, type);
    }

    private <T> T patch(String url, Object body, TypeReference<T> type) {
        return exchange("PATCH", url, body, type);
    }

    private <T> T exchange(String method, String url, Object body, TypeReference<T> type) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("accept", "application/json")
                    .header("x-request-id", requestId());

            String authorization = authorizationHeader();
            if (authorization != null && !authorization.isBlank()) {
                builder.header("authorization", authorization);
            }

            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("content-type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(body)));
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                return null;
            }

            if (response.statusCode() >= 400) {
                throw new DownstreamServiceException(
                        "Downstream service returned HTTP " + response.statusCode() + ": " + compact(response.body()),
                        response.statusCode(),
                        url
                );
            }

            if (response.body() == null || response.body().isBlank()) {
                return null;
            }

            return jsonMapper.readValue(response.body(), type);
        } catch (IOException ex) {
            throw new DownstreamServiceException("Cannot call downstream service: " + ex.getMessage(), 502, url);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DownstreamServiceException("Interrupted while calling downstream service", 502, url);
        }
    }

    private static String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Service URL cannot be blank");
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String compact(String value) {
        if (value == null) {
            return "";
        }
        String compacted = value.replaceAll("\\s+", " ").trim();
        return compacted.length() <= 250 ? compacted : compacted.substring(0, 250) + "...";
    }

    private static String requestId() {
        HttpServletRequest request = currentRequest();
        String existing = request == null ? null : request.getHeader("x-request-id");
        return existing == null || existing.isBlank() ? UUID.randomUUID().toString() : existing;
    }

    private static String authorizationHeader() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader("authorization");
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
