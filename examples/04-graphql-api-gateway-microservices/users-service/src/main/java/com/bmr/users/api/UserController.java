package com.bmr.users.api;

import com.bmr.users.model.UserRole;
import com.bmr.users.model.User;
import com.bmr.users.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class UserController {
    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "users-service", "status", "ok");
    }

    @GetMapping("/users")
    public List<User> users() {
        return repository.findAll();
    }

    @GetMapping("/users/batch")
    public List<User> usersBatch(@RequestParam String ids) {
        List<Long> parsedIds = Arrays.stream(ids.split(","))
                .filter(value -> !value.isBlank())
                .map(Long::parseLong)
                .distinct()
                .toList();

        return repository.findByIds(parsedIds);
    }

    @GetMapping("/users/{id}")
    public User user(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@RequestBody CreateUserRequest request) {
        String name = requireText(request.name(), "name");
        String email = requireText(request.email(), "email");
        UserRole role = request.role() == null ? UserRole.CUSTOMER : request.role();

        try {
            return repository.create(name, email, role);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists", ex);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return value.trim();
    }
}
