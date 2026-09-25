package btk.staj.WorkFlowProject.common.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OPS-4 — surumlenmis {@code docs/openapi.json} ile calisan uygulamanin
 * sozlesmesi arasindaki kaymayi CI'da yakalar.
 *
 * <p>Neden bayt karsilastirmasi degil: {@code docs/openapi.json} elle bakimlidir
 * (bkz. kok README). {@code servers.url} calisan ortama gore degisir, alan
 * sirasi ve bicimlendirme uretim aracina gore kayar. Bunlar uzerinden diff
 * almak her turda gurultu uretir ve gate kisa surede kapatilir.
 *
 * <p>Bunun yerine <b>anlamli kume</b> karsilastirilir: hangi yollar var, her
 * yolda hangi HTTP metotlari var, hangi DTO semalari var ve her semanin alan
 * kumesi ne. Aciklamalar, ornekler, siralama ve sunucu adresi kapsam disidir.
 *
 * <p>Mevcut {@code backend / verify} isinde kostugu icin CI'a ayri bir job
 * eklemeye gerek yoktur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("OPS-4 - OpenAPI snapshot drift")
class OpenApiSnapshotDriftTest {

    /** Testler {@code backend/} dizininden kosar; snapshot depo kokundedir. */
    private static final Path SNAPSHOT = Path.of("..", "docs", "openapi.json");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("yollar, metotlar ve sema alanlari snapshot ile ayni")
    void snapshotCalisanSozlesmeyleAyni() throws Exception {
        JsonNode live = MAPPER.readTree(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(SNAPSHOT)
                .as("Surumlenmis OpenAPI snapshot'i bulunamadi: %s", SNAPSHOT.toAbsolutePath())
                .exists();
        JsonNode snapshot = MAPPER.readTree(Files.readString(SNAPSHOT, StandardCharsets.UTF_8));

        assertThat(operations(live))
                .as("""
                        Calisan uygulama ile docs/openapi.json arasinda yol/metot farki var. \
                        Uc eklendiyse snapshot'i ilgili bolumu koruyarak elle guncelleyin; \
                        dosyayi toptan yeniden uretmeyin.""")
                .isEqualTo(operations(snapshot));

        assertThat(schemaFields(live))
                .as("""
                        Calisan uygulama ile docs/openapi.json arasinda DTO sema/alan farki var. \
                        Yanit veya istek sozlesmesi degistiyse snapshot ve uretilmis istemci \
                        ayni PR'da guncellenmelidir.""")
                .isEqualTo(schemaFields(snapshot));
    }

    /** Yol -> o yolda tanimli HTTP metotlari. */
    private static Map<String, Set<String>> operations(JsonNode document) {
        Map<String, Set<String>> operations = new TreeMap<>();
        JsonNode paths = document.path("paths");

        for (Iterator<String> paths_ = paths.fieldNames(); paths_.hasNext(); ) {
            String path = paths_.next();
            Set<String> methods = new TreeSet<>();
            for (Iterator<String> keys = paths.path(path).fieldNames(); keys.hasNext(); ) {
                String key = keys.next();
                if (isHttpMethod(key)) {
                    methods.add(key);
                }
            }
            operations.put(path, methods);
        }

        return operations;
    }

    /** Sema adi -> alan adlari. Aciklama, ornek ve tip detaylari kapsam disi. */
    private static Map<String, Set<String>> schemaFields(JsonNode document) {
        Map<String, Set<String>> schemas = new TreeMap<>();
        JsonNode components = document.path("components").path("schemas");

        for (Iterator<String> names = components.fieldNames(); names.hasNext(); ) {
            String schema = names.next();
            Set<String> fields = new TreeSet<>();
            JsonNode properties = components.path(schema).path("properties");
            for (Iterator<String> fieldNames = properties.fieldNames(); fieldNames.hasNext(); ) {
                fields.add(fieldNames.next());
            }
            schemas.put(schema, fields);
        }

        return schemas;
    }

    private static boolean isHttpMethod(String key) {
        return List.of("get", "put", "post", "delete", "options", "head", "patch", "trace").contains(key);
    }
}
