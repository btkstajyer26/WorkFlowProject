package btk.staj.WorkFlowProject.rbac.config;

import btk.staj.WorkFlowProject.attachment.repository.FileRepository;
import btk.staj.WorkFlowProject.record.repository.CategoryRepository;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.TokenRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Swagger arayuzunun kimlik dogrulama istemeden acilabildigini dogrular.
 * Veritabani gerektirmemesi icin JPA/Flyway autoconfig'leri kapatilir ve
 * repository'ler mock'lanir.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
@DisplayName("Swagger erisimi")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private FileRepository fileRepository;

    @MockitoBean
    private TokenRepository tokenRepository;

    @MockitoBean
    private RecordRepository recordRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("swagger-ui.html giris istemeden yonlendirme doner")
    void swaggerUiKimlikDogrulamaIstemez() throws Exception {
        // Sadece 3xx yeterli degil: kimlik dogrulama acikken de login sayfasina
        // 302 donerdi. Yonlendirmenin hedefi Swagger arayuzu olmali.
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    @DisplayName("OpenAPI semasi giris istemeden 200 doner")
    void apiDocsKimlikDogrulamaIstemez() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("swagger-ui statik dosyalari giris istemeden acilir")
    void swaggerStatikDosyalariAcilir() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
