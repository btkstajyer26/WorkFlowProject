package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;

import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.auth.security.AuthenticatedUser;
import btk.staj.WorkFlowProject.notification.service.MailService;
import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.workflow.adapter.UserPortAdapter;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Onay akisi gecislerinin gercek PostgreSQL semasina yazildigini dogrular.
 *
 * <p>Mevcut {@link WorkflowActionControllerTest} bu ucun kablolamasini
 * dogruluyor ama tum repository'ler mock ve transaction yonetimi kapali
 * oldugu icin <em>yazmanin gerceklestigini</em> kanitlayamiyor: kolonlarin
 * dogru satira gittigi, denetim izinin ayni transaction'da olustugu ve hata
 * halinde ikisinin birlikte geri alindigi orada gorunmez. Buradaki testler
 * yalnizca o bosluga bakar; kural kapsami
 * {@code WorkflowTransitionValidatorTest} ve
 * {@code WorkflowApplicationServiceTest} tarafindan zaten kapsanmistir ve
 * burada tekrarlanmaz.
 *
 * <p>Ayakta bir PostgreSQL ister (bkz. {@code docker compose up -d db}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Onay akisi veritabani entegrasyonu")
class WorkflowTransitionPersistenceIntegrationTest {

    private static final String ACTION_URL = "/api/records/{recordId}/workflow/actions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    /** E-posta commit sonrasi ve {@code @Async} gider; testin konusu degil, SMTP'ye cikilmasin. */
    @MockitoBean
    private MailService mailService;

    /** Varsayilan olarak gercek servise delege eder; yalniz rollback testinde patlatilir. */
    @MockitoSpyBean
    private AuditLogService auditLogService;

    /**
     * Varsayilan olarak gercek adaptore delege eder. Surum catismasi testinde
     * araya girme noktasi olarak kullanilir: hedef cozumleme, kayit okunduktan
     * <em>sonra</em> ve yazma yapilmadan <em>once</em> calisan tek adimdir.
     */
    @MockitoSpyBean
    private UserPortAdapter userPortAdapter;

    // =================================================================
    // Gecisler: kolonlarin dogru degerle yazildigi
    // =================================================================

    @Nested
    @DisplayName("gecis sonrasi kayit ve denetim izi")
    class Transitions {

        @Test
        @Transactional
        @DisplayName("GONDER kaydi tek aktif yardimciya atar, last_deputy_id'ye dokunmaz")
        void gonder() throws Exception {
            UUID calisan = insertUser(RoleName.CALISAN);
            UUID yardimci = insertSingleActiveYardimci();
            UUID record = insertRecord(RecordStatus.TASLAK, calisan, null, null);

            perform(record, actor(calisan, RoleName.CALISAN),
                    "{\"action\":\"GONDER\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newStatus").value("BSK_YRD_INCELEMESINDE"));

            Map<String, Object> row = readRecord(record);
            assertThat(row.get("status")).isEqualTo("BSK_YRD_INCELEMESINDE");
            assertThat(row.get("assigned_to")).isEqualTo(yardimci);
            // Sozlesme: last_deputy_id yalnizca BASKANA_ILET sirasinda yazilir.
            assertThat(row.get("last_deputy_id")).isNull();

            assertAuditRow(record, WorkflowAction.GONDER,
                    RecordStatus.TASLAK, RecordStatus.BSK_YRD_INCELEMESINDE,
                    calisan, RoleName.CALISAN, null);
        }

        @Test
        @Transactional
        @DisplayName("TEKRAR_GONDER duzenleme bekleyen kaydi guncel aktif yardimciya atar")
        void tekrarGonder() throws Exception {
            UUID calisan = insertUser(RoleName.CALISAN);
            // Kaydi daha once ileten yardimci; artik pasif, hedef olarak secilemez.
            UUID ilkYardimci = insertUser(RoleName.BASKAN_YARDIMCISI);
            UUID yeniYardimci = insertSingleActiveYardimci();
            UUID record = insertRecord(
                    RecordStatus.DUZENLEME_BEKLIYOR, calisan, calisan, ilkYardimci);

            perform(record, actor(calisan, RoleName.CALISAN),
                    "{\"action\":\"TEKRAR_GONDER\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newStatus").value("BSK_YRD_INCELEMESINDE"));

            Map<String, Object> row = readRecord(record);
            assertThat(row.get("status")).isEqualTo("BSK_YRD_INCELEMESINDE");
            assertThat(row.get("assigned_to")).isEqualTo(yeniYardimci);
            // TEKRAR_GONDER de bu kolona dokunmaz; onceki degeri korunur.
            assertThat(row.get("last_deputy_id")).isEqualTo(ilkYardimci);

            assertAuditRow(record, WorkflowAction.TEKRAR_GONDER,
                    RecordStatus.DUZENLEME_BEKLIYOR, RecordStatus.BSK_YRD_INCELEMESINDE,
                    calisan, RoleName.CALISAN, null);
        }

        @Test
        @Transactional
        @DisplayName("BASKANA_ILET kaydi tek aktif Baskana atar ve last_deputy_id'yi doldurur")
        void baskanaIlet() throws Exception {
            UUID calisan = insertUser(RoleName.CALISAN);
            UUID yardimci = insertUser(RoleName.BASKAN_YARDIMCISI);
            UUID baskan = insertSingleActiveBaskan();
            UUID record = insertRecord(
                    RecordStatus.BSK_YRD_INCELEMESINDE, calisan, yardimci, null);

            perform(record, actor(yardimci, RoleName.BASKAN_YARDIMCISI),
                    "{\"action\":\"BASKANA_ILET\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newStatus").value("BASKAN_INCELEMESINDE"));

            Map<String, Object> row = readRecord(record);
            assertThat(row.get("status")).isEqualTo("BASKAN_INCELEMESINDE");
            assertThat(row.get("assigned_to")).isEqualTo(baskan);
            // Kritik: Baskanin geri gonderme hedefi bu kolondan bulunur.
            assertThat(row.get("last_deputy_id")).isEqualTo(yardimci);

            assertAuditRow(record, WorkflowAction.BASKANA_ILET,
                    RecordStatus.BSK_YRD_INCELEMESINDE, RecordStatus.BASKAN_INCELEMESINDE,
                    yardimci, RoleName.BASKAN_YARDIMCISI, null);
        }

        @Test
        @Transactional
        @DisplayName("CALISANA_GERI_GONDER (yardimci) kaydi olusturana dondurur")
        void calisanaGeriGonderYardimci() throws Exception {
            UUID calisan = insertUser(RoleName.CALISAN);
            UUID yardimci = insertUser(RoleName.BASKAN_YARDIMCISI);
            UUID record = insertRecord(
                    RecordStatus.BSK_YRD_INCELEMESINDE, calisan, yardimci, null);

            perform(record, actor(yardimci, RoleName.BASKAN_YARDIMCISI),
                    "{\"action\":\"CALISANA_GERI_GONDER\",\"comment\":\"Butce kalemi eksik\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newStatus").value("DUZENLEME_BEKLIYOR"));

            Map<String, Object> row = readRecord(record);
            assertThat(row.get("status")).isEqualTo("DUZENLEME_BEKLIYOR");
            assertThat(row.get("assigned_to")).isEqualTo(calisan);

            assertAuditRow(record, WorkflowAction.CALISANA_GERI_GONDER,
                    RecordStatus.BSK_YRD_INCELEMESINDE, RecordStatus.DUZENLEME_BEKLIYOR,
                    yardimci, RoleName.BASKAN_YARDIMCISI, "Butce kalemi eksik");
        }

        @Test
        @Transactional
        @DisplayName("CALISANA_GERI_GONDER (Baskan) kaydi olusturana dondurur, last_deputy_id korunur")
        void calisanaGeriGonderBaskan() throws Exception {
            UUID calisan = insertUser(RoleName.CALISAN);
            UUID yardimci = insertUser(RoleName.BASKAN_YARDIMCISI);
            UUID baskan = insertUser(RoleName.BASKAN);
            UUID record = insertRecord(
                    RecordStatus.BASKAN_INCELEMESINDE, calisan, baskan, yardimci);

            perform(record, actor(baskan, RoleName.BASKAN),
                    "{\"action\":\"CALISANA_GERI_GONDER\",\"comment\":\"Teknik sartname yetersiz\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newStatus").value("DUZENLEME_BEKLIYOR"));

            Map<String, Object> row = readRecord(record);
            assertThat(row.get("status")).isEqualTo("DUZENLEME_BEKLIYOR");
            assertThat(row.get("assigned_to")).isEqualTo(calisan);
            assertThat(row.get("last_deputy_id")).isEqualTo(yardimci);

            assertAuditRow(record, WorkflowAction.CALISANA_GERI_GONDER,
                    RecordStatus.BASKAN_INCELEMESINDE, RecordStatus.DUZENLEME_BEKLIYOR,
                    baskan, RoleName.BASKAN, "Teknik sartname yetersiz");
        }

        @Test
        @Transactional
        @DisplayName("BASKAN_YARDIMCISINA_GERI_GONDER hedefi last_deputy_id'den bulur")
        void baskanYardimcisinaGeriGonder() throws Exception {
            UUID calisan = insertUser(RoleName.CALISAN);
            UUID ileten = insertUser(RoleName.BASKAN_YARDIMCISI);
            UUID digerYardimci = insertUser(RoleName.BASKAN_YARDIMCISI);
            UUID baskan = insertUser(RoleName.BASKAN);
            UUID record = insertRecord(
                    RecordStatus.BASKAN_INCELEMESINDE, calisan, baskan, ileten);

            perform(record, actor(baskan, RoleName.BASKAN),
                    "{\"action\":\"BASKAN_YARDIMCISINA_GERI_GONDER\",\"comment\":\"Tekrar degerlendirin\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newStatus").value("BSK_YRD_INCELEMESINDE"));

            Map<String, Object> row = readRecord(record);
            assertThat(row.get("status")).isEqualTo("BSK_YRD_INCELEMESINDE");
            // Kayit sisteme baska bir aktif yardimci olsa da ileten kisiye doner.
            assertThat(row.get("assigned_to")).isEqualTo(ileten);
            assertThat(row.get("assigned_to")).isNotEqualTo(digerYardimci);

            assertAuditRow(record, WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER,
                    RecordStatus.BASKAN_INCELEMESINDE, RecordStatus.BSK_YRD_INCELEMESINDE,
                    baskan, RoleName.BASKAN, "Tekrar degerlendirin");
        }

        @Test
        @Transactional
        @DisplayName("ONAYLA kaydi terminal yapar ve assigned_to'yu bosaltir")
        void onayla() throws Exception {
            UUID calisan = insertUser(RoleName.CALISAN);
            UUID yardimci = insertUser(RoleName.BASKAN_YARDIMCISI);
            UUID baskan = insertUser(RoleName.BASKAN);
            UUID record = insertRecord(
                    RecordStatus.BASKAN_INCELEMESINDE, calisan, baskan, yardimci);

            perform(record, actor(baskan, RoleName.BASKAN),
                    "{\"action\":\"ONAYLA\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newStatus").value("ONAYLANDI"));

            Map<String, Object> row = readRecord(record);
            assertThat(row.get("status")).isEqualTo("ONAYLANDI");
            assertThat(row.get("assigned_to")).isNull();
            assertThat(row.get("last_deputy_id")).isEqualTo(yardimci);

            assertAuditRow(record, WorkflowAction.ONAYLA,
                    RecordStatus.BASKAN_INCELEMESINDE, RecordStatus.ONAYLANDI,
                    baskan, RoleName.BASKAN, null);
        }

        @Test
        @Transactional
        @DisplayName("REDDET kaydi terminal yapar ve aciklamayi denetim izine yazar")
        void reddet() throws Exception {
            UUID calisan = insertUser(RoleName.CALISAN);
            UUID yardimci = insertUser(RoleName.BASKAN_YARDIMCISI);
            UUID baskan = insertUser(RoleName.BASKAN);
            UUID record = insertRecord(
                    RecordStatus.BASKAN_INCELEMESINDE, calisan, baskan, yardimci);

            perform(record, actor(baskan, RoleName.BASKAN),
                    "{\"action\":\"REDDET\",\"comment\":\"Butce yetersiz\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.newStatus").value("REDDEDILDI"));

            Map<String, Object> row = readRecord(record);
            assertThat(row.get("status")).isEqualTo("REDDEDILDI");
            assertThat(row.get("assigned_to")).isNull();

            assertAuditRow(record, WorkflowAction.REDDET,
                    RecordStatus.BASKAN_INCELEMESINDE, RecordStatus.REDDEDILDI,
                    baskan, RoleName.BASKAN, "Butce yetersiz");
        }
    }

    // =================================================================
    // Atomiklik: basarisiz gecis hicbir iz birakmaz
    // =================================================================

    @Test
    @Transactional
    @DisplayName("reddedilen gecis ne kaydi degistirir ne denetim izi yazar")
    void aRejectedTransitionLeavesTheDatabaseUntouched() throws Exception {
        UUID calisan = insertUser(RoleName.CALISAN);
        UUID atanmisBaskan = insertUser(RoleName.BASKAN);
        UUID baskaBaskan = insertUser(RoleName.BASKAN);
        UUID record = insertRecord(
                RecordStatus.BASKAN_INCELEMESINDE, calisan, atanmisBaskan, null);

        perform(record, actor(baskaBaskan, RoleName.BASKAN), "{\"action\":\"ONAYLA\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WORKFLOW_FORBIDDEN"));

        Map<String, Object> row = readRecord(record);
        assertThat(row.get("status")).isEqualTo("BASKAN_INCELEMESINDE");
        assertThat(row.get("assigned_to")).isEqualTo(atanmisBaskan);
        assertThat(auditRowCount(record)).isZero();
    }

    /**
     * B9'un test edilmemis kalan maddesi: gecis basarili olsa bile denetim izi
     * yazilamazsa kayit guncellemesi de geri alinmalidir.
     *
     * <p>Bu test bilerek {@code @Transactional} <em>degildir</em>: testin kendi
     * transaction'i olsaydi servisin rollback'i onun icinde kalir ve gorunmezdi.
     * Bu yuzden veriler gercekten commit'lenir ve sonunda elle temizlenir.
     */
    @Test
    @DisplayName("denetim izi yazilamazsa kayit guncellemesi de geri alinir")
    void aFailedAuditWriteRollsBackTheRecordUpdate() throws Exception {
        // Bu test commit'ledigi icin pasiflestirilen yardimcilar sonunda geri alinmali.
        List<UUID> pasiflestirilenler = activeUserIds(RoleName.BASKAN_YARDIMCISI);
        UUID calisan = insertUser(RoleName.CALISAN);
        UUID yardimci = insertSingleActiveYardimci();
        UUID record = insertRecord(RecordStatus.TASLAK, calisan, null, null);

        try {
            doThrow(new IllegalStateException("denetim izi yazilamadi"))
                    .when(auditLogService).record(any());

            // Tek aktif yardimci kurulmasaydi istek hedef cozulemedigi icin 409
            // ile donerdi ve bu test rollback'i degil onu olcerdi.
            perform(record, actor(calisan, RoleName.CALISAN),
                    "{\"action\":\"GONDER\"}")
                    .andExpect(status().isInternalServerError());

            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT status, assigned_to, version FROM records WHERE id = ?", record);
            assertThat(row.get("status")).isEqualTo("TASLAK");
            assertThat(row.get("assigned_to")).isNull();
            assertThat(row.get("version")).isEqualTo(0);

            assertThat(auditRowCount(record)).isZero();
            // Bildirim de ayni transaction'da yazilir; o da geri alinmali.
            assertThat(notificationRowCount(record)).isZero();
        } finally {
            deleteRecordAndUsers(record, List.of(calisan, yardimci));
            reactivate(pasiflestirilenler);
        }
    }

    /**
     * Surum catismasinin uctan uca {@code 409} olarak raporlandigini dogrular.
     *
     * <p>Catisma bilerek <em>elle yazilan</em> surum karsilastirmasiyla degil,
     * Hibernate'in flush anindaki {@code @Version} kontroluyle uretilir: kayit,
     * servis okuduktan sonra ama yazma gerceklesmeden hemen once guncellenir,
     * boylece {@code UPDATE ... WHERE version = <eski>} sifir satir gunceller.
     * Adapterdeki elle kontrol tek transaction icinde persistence context ayni
     * managed entity'yi dondurdugu icin bu yolu yakalayamaz; asil koruma budur.
     *
     * <p>{@link #aFailedAuditWriteRollsBackTheRecordUpdate()} gibi bu test de
     * bilerek {@code @Transactional} <em>degildir</em>: servisin rollback'i
     * testin kendi transaction'i icinde gorunmez olurdu.
     */
    @Test
    @DisplayName("kayit istek sirasinda degistiyse 409 doner ve hicbir iz birakmaz")
    void aConcurrentVersionChangeIsReportedAsConflict() throws Exception {
        List<UUID> pasiflestirilenler = activeUserIds(RoleName.BASKAN_YARDIMCISI);
        UUID calisan = insertUser(RoleName.CALISAN);
        UUID yardimci = insertSingleActiveYardimci();
        UUID record = insertRecord(RecordStatus.TASLAK, calisan, null, null);

        try {
            // Hedef cozumleme, kayit okunduktan sonra ve yazmadan once calisir:
            // baska bir islem kaydi tam bu aralikta guncellemis gibi davran.
            doAnswer(invocation -> {
                jdbc.update("UPDATE records SET version = version + 1 WHERE id = ?", record);
                return invocation.callRealMethod();
            }).when(userPortAdapter).findActiveByRole(RoleName.BASKAN_YARDIMCISI);

            perform(record, actor(calisan, RoleName.CALISAN),
                    "{\"action\":\"GONDER\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("WORKFLOW_VERSION_CONFLICT"));

            // Gecis uygulanmamis olmali: durum, atama ve surum ilk hallerinde.
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT status, assigned_to, version FROM records WHERE id = ?", record);
            assertThat(row.get("status")).isEqualTo("TASLAK");
            assertThat(row.get("assigned_to")).isNull();
            assertThat(row.get("version")).isEqualTo(0);

            // Basarisiz bir gecis gecmiste iz birakmaz.
            assertThat(auditRowCount(record)).isZero();
            assertThat(notificationRowCount(record)).isZero();
        } finally {
            deleteRecordAndUsers(record, List.of(calisan, yardimci));
            reactivate(pasiflestirilenler);
        }
    }

    // =================================================================
    // Yardimcilar
    // =================================================================

    private ResultActions perform(UUID recordId, AuthenticatedUser actor, String body) throws Exception {
        return mockMvc.perform(post(ACTION_URL, recordId)
                .with(user(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /**
     * JPA yazmalarini SQL'e indirir ve persistence context'i bosaltir; boylece
     * asagidaki JDBC okumasi Hibernate onbelleginden degil gercekten
     * veritabanindan gelir.
     */
    private Map<String, Object> readRecord(UUID recordId) {
        flushToDatabase();
        return jdbc.queryForMap(
                "SELECT status, assigned_to, last_deputy_id, version FROM records WHERE id = ?",
                recordId);
    }

    /**
     * Transaction disinda calisan rollback testinde flush edilecek bir sey
     * yoktur (servis kendi transaction'ini coktan geri almistir) ve paylasilan
     * EntityManager zaten flush edemez; bu yuzden yalniz aktif transaction
     * varken calisir.
     */
    private void flushToDatabase() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        entityManager.flush();
        entityManager.clear();
    }

    private void assertAuditRow(UUID recordId,
                                WorkflowAction action,
                                RecordStatus previousStatus,
                                RecordStatus newStatus,
                                UUID actorId,
                                RoleName actorRole,
                                String comment) {
        flushToDatabase();

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT user_id, role_id, action, previous_status, new_status, comment "
                        + "FROM audit_logs WHERE record_id = ?", recordId);

        assertThat(row.get("action")).isEqualTo(action.name());
        assertThat(row.get("previous_status")).isEqualTo(previousStatus.name());
        assertThat(row.get("new_status")).isEqualTo(newStatus.name());
        assertThat(row.get("user_id")).isEqualTo(actorId);
        assertThat(row.get("role_id")).isEqualTo(roleId(actorRole));
        assertThat(row.get("comment")).isEqualTo(comment);
    }

    private int auditRowCount(UUID recordId) {
        flushToDatabase();
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM audit_logs WHERE record_id = ?", Integer.class, recordId);
        return count == null ? 0 : count;
    }

    private int notificationRowCount(UUID recordId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM notifications WHERE record_id = ?", Integer.class, recordId);
        return count == null ? 0 : count;
    }

    private int roleId(RoleName role) {
        Integer id = jdbc.queryForObject(
                "SELECT id FROM roles WHERE name = ?", Integer.class, role.name());
        if (id == null) {
            throw new IllegalStateException("roles tablosunda seed eksik: " + role.name());
        }
        return id;
    }

    private int anyCategoryId() {
        Integer id = jdbc.queryForObject(
                "SELECT id FROM categories ORDER BY id LIMIT 1", Integer.class);
        if (id == null) {
            throw new IllegalStateException("categories tablosunda seed eksik");
        }
        return id;
    }

    private UUID insertUser(RoleName role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, first_name, last_name, email, password_hash, role_id, is_active) "
                        + "VALUES (?, ?, ?, ?, ?, ?, true)",
                id, "Entegrasyon", role.name(), "wf-" + id + "@ornek.test", "x", roleId(role));
        return id;
    }

    /**
     * {@code BASKANA_ILET} sistemde <em>tam olarak bir</em> aktif Baskan
     * bekler. Gelistirici veritabaninda baska Baskanlar olabilecegi icin once
     * hepsi pasiflestirilir; islem transaction icinde oldugundan test bitince
     * geri alinir.
     */
    private UUID insertSingleActiveBaskan() {
        jdbc.update("UPDATE users SET is_active = false WHERE role_id = ?", roleId(RoleName.BASKAN));
        return insertUser(RoleName.BASKAN);
    }

    /**
     * {@code GONDER} / {@code TEKRAR_GONDER} de hedefini tekil aktif Baskan
     * Yardimcisindan cozer; {@link #insertSingleActiveBaskan()} ile ayni sebep.
     *
     * <p>Transaction disinda calisan testte cagrilirsa pasiflestirme
     * commit'lenir: cagiran, once {@link #activeUserIds(RoleName)} ile mevcut
     * aktifleri saklayip sonunda {@link #reactivate(List)} ile geri almalidir.
     */
    private UUID insertSingleActiveYardimci() {
        jdbc.update("UPDATE users SET is_active = false WHERE role_id = ?",
                roleId(RoleName.BASKAN_YARDIMCISI));
        return insertUser(RoleName.BASKAN_YARDIMCISI);
    }

    private List<UUID> activeUserIds(RoleName role) {
        return jdbc.queryForList(
                "SELECT id FROM users WHERE role_id = ? AND is_active = true",
                UUID.class, roleId(role));
    }

    private void reactivate(List<UUID> userIds) {
        userIds.forEach(userId ->
                jdbc.update("UPDATE users SET is_active = true WHERE id = ?", userId));
    }

    private UUID insertRecord(RecordStatus status, UUID createdBy, UUID assignedTo, UUID lastDeputyId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO records "
                        + "(id, title, description, category_id, status, created_by, assigned_to, last_deputy_id, version) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, "Entegrasyon kaydi", "Test aciklamasi", anyCategoryId(),
                status.name(), createdBy, assignedTo, lastDeputyId);
        return id;
    }

    /** Yalniz transaction disinda calisan test icin; digerleri rollback ile temizlenir. */
    private void deleteRecordAndUsers(UUID recordId, List<UUID> userIds) {
        jdbc.update("DELETE FROM notifications WHERE record_id = ?", recordId);
        jdbc.update("DELETE FROM audit_logs WHERE record_id = ?", recordId);
        // HTTP istek filtresi CALISAN/BASKAN_YARDIMCISI isteklerini user_audit_logs'a
        // (ve ADMIN icin record_id'siz audit_logs'a) yazar; kullanici silinmeden once
        // bu satırlar kalkmali (fk_user_audit_target / fk_audit_user RESTRICT).
        userIds.forEach(userId -> {
            jdbc.update("DELETE FROM user_audit_logs WHERE target_user_id = ? OR performed_by = ?",
                    userId, userId);
            jdbc.update("DELETE FROM audit_logs WHERE user_id = ?", userId);
            jdbc.update("DELETE FROM tokens WHERE user_id = ?", userId);
        });
        jdbc.update("DELETE FROM records WHERE id = ?", recordId);
        userIds.forEach(userId -> jdbc.update("DELETE FROM users WHERE id = ?", userId));
    }

    private AuthenticatedUser actor(UUID userId, RoleName roleName) {
        Role role = new Role();
        role.setId(roleId(roleName));
        role.setName(roleName.name());
        role.setActive(true);
        role.setSystemKey(roleName.name());
        role.setWorkflowActor(AuthorizationFixtures.workflowActor(roleName.name()));

        User user = new User();
        user.setId(userId);
        user.setEmail("wf-" + userId + "@ornek.test");
        user.setPasswordHash("x");
        user.setRole(role);
        user.setActive(true);

        return AuthorizationFixtures.authenticated(user);
    }
}
