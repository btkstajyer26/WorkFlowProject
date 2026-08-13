package btk.staj.WorkFlowProject.user.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import btk.staj.WorkFlowProject.user.dto.ChangeRoleRequest;
import btk.staj.WorkFlowProject.user.dto.CreateUserRequest;
import btk.staj.WorkFlowProject.user.dto.UserResponse;
import btk.staj.WorkFlowProject.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Hesap her zaman Calisan rolüyle acilir; baslangic rolu istekte
     * secilemez (sartname: rol yalnizca ayri bir islemle degistirilir).
     */
    @PostMapping("/users")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.createUser(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword()
        ));
    }

    @PatchMapping("/users/{id}/role")
    public UserResponse changeRole(@PathVariable UUID id,
                                   @Valid @RequestBody ChangeRoleRequest request) {
        return UserResponse.from(userService.changeRole(id, request.getRoleName()));
    }
}