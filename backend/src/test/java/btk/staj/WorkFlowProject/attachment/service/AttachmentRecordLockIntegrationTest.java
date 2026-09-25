package btk.staj.WorkFlowProject.attachment.service;

import btk.staj.WorkFlowProject.attachment.storage.FileStorageService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUserFactory;
import btk.staj.WorkFlowProject.auth.security.VisibilityActor;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * B04 / B07 — dosya islemleri kayit satirina kilitlenir, tarihsel gorunum
 * indirmede de gecerlidir.
 *
 * <p>Gercek PostgreSQL ister; yalnizca disk depolamasi ve icerik dogrulayici
 * mock'lanir. Test bilerek {@code @Transactional} degildir: servisin kendi
 * transaction sinirini ve satir kilidini gozlemek gerekiyor.
 *
 * <p>Onceki tasarimda {@code RecordLockValidator} kilit almadan okuma yapiyordu;
 * durum kontrolu ile dosya satirinin yazilmasi arasinda workflow gecisi araya
 * girebiliyordu (inceleme probu: beklenen 0, gerceklesen 1 dosya satiri).
 */
@SpringBootTest
@DisplayName("B04 - dosya islemi kayit kilidi")
class AttachmentRecordLockIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private FileService fileService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticatedUserFactory principals;

    @MockitoBean
    private FileStorageService storage;
    @MockitoBean
    private FileContentValidator contentValidator;

    private UUID creatorId;
    private UUID deputyId;
    private UUID recordId;

    @BeforeEach
    void fixture() {
        creatorId = user("CALISAN");
        deputyId = user("BASKAN_YARDIMCISI");

        Integer categoryId = jdbc.queryForObject("SELECT min(id) FROM categories", Integer.class);
        recordId = jdbc.queryForObject("""
                        INSERT INTO records (title, description, category_id, status, created_by)
                        VALUES ('B04 regresyon', 'B04 regresyon', ?, 'TASLAK', ?)
                        RETURNING id
                        """,
                UUID.class, categoryId, creatorId);

        when(contentValidator.detectAndValidate(any())).thenReturn("application/pdf");
        when(contentValidator.extensionFor(anyString())).thenReturn(".pdf");
        when(storage.loadAsResource(anyString())).thenReturn(new ByteArrayResource(new byte[]{1}));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM files WHERE record_id = ?", recordId);
        jdbc.update("DELETE FROM audit_logs WHERE record_id = ?", recordId);
        jdbc.update("DELETE FROM records WHERE id = ?", recordId);
        jdbc.update("DELETE FROM users WHERE id IN (?, ?)", creatorId, deputyId);
    }

    private UUID user(String systemKey) {
        Integer roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = ?", Integer.class, systemKey);
        return jdbc.queryForObject("""
                        INSERT INTO users (first_name, last_name, email, password_hash, role_id, is_active)
                        VALUES ('B04', 'Regresyon', ?, 'test-only-unused-hash', ?, true)
                        RETURNING id
                        """,
                UUID.class, UUID.randomUUID() + "@b04.invalid", roleId);
    }

    /**
     * Kilit gercekten tutuluyor mu? Yukleme kaydin uzerinde dururken kaydi
     * incelemeye tasimaya calisan yazim beklemek zorunda kalmali. Onceki
     * tasarimda bu yazim aninda gecer, yukleme de eski izinle commit ederdi.
     */
    @Test
    @DisplayName("yukleme devam ederken kaydin durumunu degistiren yazim bekler")
    void yuklemeSirasindaDurumDegisikligiBekler() throws Exception {
        CountDownLatch uploadHoldsLock = new CountDownLatch(1);
        CountDownLatch releaseUpload = new CountDownLatch(1);

        // Dogrulama kilit alindiktan SONRA calisiyor; yuklemeyi burada bekletiyoruz.
        when(contentValidator.detectAndValidate(any())).thenAnswer(call -> {
            uploadHoldsLock.countDown();
            if (!releaseUpload.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test latch zaman asimi");
            }
            return "application/pdf";
        });

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> upload = executor.submit(() -> fileService.uploadFile(
                    new MockMultipartFile("files", "b04.pdf", "application/pdf", new byte[]{1}),
                    recordId, creatorId));

            assertThat(uploadHoldsLock.await(15, TimeUnit.SECONDS)).isTrue();

            Future<Integer> statusChange = executor.submit(() -> jdbc.update(
                    "UPDATE records SET status = 'BSK_YRD_INCELEMESINDE', assigned_to = ?, version = version + 1 WHERE id = ?",
                    deputyId, recordId));

            // Kilit tutuluyorsa bu yazim tamamlanamaz.
            assertThatThrownBy(() -> statusChange.get(2, TimeUnit.SECONDS))
                    .as("Kayit kilitliyken durum degisikligi beklemeli")
                    .isInstanceOf(TimeoutException.class);

            releaseUpload.countDown();
            upload.get(20, TimeUnit.SECONDS);

            // Yukleme commit ettikten sonra bekleyen yazim serbest kalir.
            assertThat(statusChange.get(20, TimeUnit.SECONDS)).isEqualTo(1);
        }

        // Iki islem seri hale geldi: yukleme kendi gordugu durumla commit etti,
        // durum degisikligi ondan sonra uygulandi.
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM files WHERE record_id = ?", Integer.class, recordId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM records WHERE id = ?", String.class, recordId))
                .isEqualTo("BSK_YRD_INCELEMESINDE");
    }

    /**
     * Yarisin diger yonu: durum degisikligi once commit ederse, kilidi sonra
     * alan yukleme taze durumu gorur ve reddedilir.
     */
    @Test
    @DisplayName("durum degistikten sonra gelen yukleme reddedilir")
    void durumDegistiktenSonraYuklemeReddedilir() {
        jdbc.update("UPDATE records SET status = 'BSK_YRD_INCELEMESINDE', assigned_to = ? WHERE id = ?",
                deputyId, recordId);

        assertThatThrownBy(() -> fileService.uploadFile(
                new MockMultipartFile("files", "b04.pdf", "application/pdf", new byte[]{1}),
                recordId, creatorId))
                .hasMessageContaining("TASLAK");

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM files WHERE record_id = ?", Integer.class, recordId))
                .isZero();
    }

    /**
     * B07: devirden sonra silinen ek, dondurulmus goruntude listeleniyorsa
     * indirilebilmeli de. Onceden liste basarili, indirme 404 oluyordu.
     */
    @Test
    @DisplayName("B07 - dondurulmus goruntude listelenen ek indirilebilir")
    void dondurulmusGorunumdeListelenenEkIndirilebilir() {
        jdbc.update("""
                        UPDATE records
                           SET status = 'DUZENLEME_BEKLIYOR',
                               assigned_to = ?,
                               last_deputy_id = ?,
                               snapshot_title = title,
                               snapshot_description = description,
                               snapshot_category_id = category_id,
                               snapshot_at = current_timestamp - interval '10 minutes'
                         WHERE id = ?
                        """,
                creatorId, deputyId, recordId);

        UUID fileId = jdbc.queryForObject("""
                        INSERT INTO files (record_id, original_name, stored_name, mime_type, file_size,
                                           uploaded_by, uploaded_at, deleted_at)
                        VALUES (?, 'devirde-vardi.pdf', ?, 'application/pdf', 1, ?,
                                current_timestamp - interval '20 minutes', current_timestamp)
                        RETURNING id
                        """,
                UUID.class, recordId, UUID.randomUUID() + ".pdf", creatorId);

        VisibilityActor deputy = VisibilityActor.from(
                principals.create(userRepository.findById(deputyId).orElseThrow()));

        assertThat(fileService.listByRecord(recordId, deputy))
                .as("Devir aninda duran ek dondurulmus listede gorunur")
                .anyMatch(file -> file.getId().equals(fileId));

        assertThatCode(() -> fileService.downloadFile(fileId, deputy))
                .as("Listede gorunen ek indirilebilmeli")
                .doesNotThrowAnyException();
    }
}
