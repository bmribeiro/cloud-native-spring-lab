package com.bmr.gateway.api;

import com.bmr.gateway.client.DownstreamClient;
import com.bmr.gateway.model.Health;
import com.bmr.gateway.model.Order;
import com.bmr.gateway.model.OrderStatus;
import com.bmr.gateway.model.User;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class GraphqlController {
    private final DownstreamClient downstreamClient;

    public GraphqlController(DownstreamClient downstreamClient) {
        this.downstreamClient = downstreamClient;
    }

    @QueryMapping
    public Health health() {
        return downstreamClient.health();
    }

    @QueryMapping
    public List<User> users() {
        return downstreamClient.users();
    }

    @QueryMapping
    public User user(@Argument Long id) {
        return downstreamClient.user(id);
    }

    @QueryMapping
    public List<Order> orders() {
        return downstreamClient.orders();
    }

    @QueryMapping
    public Order order(@Argument Long id) {
        return downstreamClient.order(id);
    }

    @QueryMapping
    public List<Order> ordersByUser(@Argument Long userId) {
        return downstreamClient.ordersByUser(userId);
    }

    @MutationMapping
    public User createUser(@Argument CreateUserInput input) {
        return downstreamClient.createUser(input);
    }

    @MutationMapping
    public Order createOrder(@Argument CreateOrderInput input) {
        User user = downstreamClient.user(input.userId());
        if (user == null) {
            throw new IllegalArgumentException("Cannot create order for a non-existing user");
        }
        return downstreamClient.createOrder(input);
    }

    @MutationMapping
    public Order updateOrderStatus(@Argument Long id, @Argument OrderStatus status) {
        return downstreamClient.updateOrderStatus(id, new UpdateOrderStatusRequest(status));
    }

    @SchemaMapping(typeName = "User", field = "orders")
    public List<Order> orders(User user) {
        return downstreamClient.ordersByUser(user.id());
    }

    @BatchMapping(typeName = "Order", field = "user")
    public Map<Order, User> user(List<Order> orders) {
        List<Long> userIds = orders.stream()
                .map(Order::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> usersById = downstreamClient.usersBatch(userIds)
                .stream()
                .collect(Collectors.toMap(User::id, Function.identity()));

        Map<Order, User> result = new LinkedHashMap<>();
        for (Order order : orders) {
            result.put(order, usersById.get(order.userId()));
        }
        return result;
    }
}
