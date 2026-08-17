package btk.staj.WorkFlowProject.user.controller;

import btk.staj.WorkFlowProject.audit.service.UserAuditLogService;
import btk.staj.WorkFlowProject.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminController için MockMvc standalone testleri.
 *
 * <p>Gerçek {@link GlobalExceptionHandler} devreye alınır; böylece
 * repository/Spring Data seviyesinde oluşan hataların da doğru HTTP durumuna
 * eşlendiği doğrulanır, yalnızca controller'ın kendi doğrulaması değil.
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserAuditLogService userAuditLogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminController controller = new AdminController(userService, userAuditLogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("var olmayan bir alana gore siralama istegi 500 yerine 400 doner")
    void gecersizSiralamaAlani400Donmeli() throws Exception {
        // Spring Data, Specification tabanli sorguda var olmayan bir sort
        // alanini bu tipte firlatir (orn. Swagger UI'nin doldurulmamis
        // "string" varsayilanini oldugu gibi gondermek). GlobalExceptionHandler
        // bunu yakalamazsa istemci 500 "Beklenmeyen bir hata olustu" gorur.
        when(userService.searchUsers(any(), any()))
                .thenThrow(new InvalidDataAccessApiUsageException(
                        "Sort expression '[\"string\"]: ASC' must only contain property references"));

        mockMvc.perform(get("/api/admin/users").param("sort", "string"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
    }
}
