package btk.staj.WorkFlowProject.user.controller;

import btk.staj.WorkFlowProject.user.dto.CreateUserRequest;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public User createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword()
        );
    }

    @PatchMapping("/users/{id}/role")
    public User changeRole(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return userService.changeRole(id, body.get("roleName"));
    }
}