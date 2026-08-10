package btk.staj.WorkFlowProject.user.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import btk.staj.WorkFlowProject.user.dto.CreateUserRequest;
import btk.staj.WorkFlowProject.user.dto.UserResponse;
import btk.staj.WorkFlowProject.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public UserResponse createUser(@RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.createUser(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword(),
                request.getRoleName()
        ));
    }
}