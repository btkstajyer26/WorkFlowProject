package btk.staj.WorkFlowProject.search.specification;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.rbac.visibility.RecordVisibilityScope;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B06 — arama ve kategori filtresi canli kolonlarda calisiyordu,
 * RecordContentView'in gosterdigi (bazen dondurulmus) surumden habersizdi.
 *
 * <p>Gercek PostgreSQL ister. Senaryo: bir kayit DUZENLEME_BEKLIYOR durumuna
 * gecerken anlik goruntusu alinir (V9). Kaydi Baskana ileten devreden, o
 * anki (dondurulmus) icerigi gorur; kaydin guncel sahibi ise canli icerigi
 * gorur (RecordContentView.visibleContent). Arama/kategori filtresi bu
 * ayrimi yansitmazsa devreden goremedigi canli icerikte sonuc bulabilir,
 * ya da guncel sahip gordugu icerikte sonucu kacirabilir.
 */
@SpringBootTest
@DisplayName("B06 - arama ve kategori filtresi gorunen icerik surumune uyar")
class RecordSearchContentParityIntegrationTest {

    private static final String LIVE_ONLY_WORD = "gizlikelimeb06";

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RecordRepository recordRepository;

    private UUID deputyId;
    private UUID assigneeId;
    private Integer liveCategoryId;
    private Integer snapshotCategoryId;
    private UUID recordId;

    @BeforeEach
    void fixture() {
        Integer employeeRole = jdbc.queryForObject(
                "SELECT id FROM roles WHERE system_key = 'CALISAN'", Integer.class);

        deputyId = jdbc.queryForObject("""
                        INSERT INTO users (first_name, last_name, email, password_hash, role_id, is_active)
                        VALUES ('B06', 'Devreden', ?, 'test-only-unused-hash', ?, true)
                        RETURNING id
                        """,
                UUID.class, UUID.randomUUID() + "@b06.invalid", employeeRole);

        assigneeId = jdbc.queryForObject("""
                        INSERT INTO users (first_name, last_name, email, password_hash, role_id, is_active)
                        VALUES ('B06', 'GuncelSahip', ?, 'test-only-unused-hash', ?, true)
                        RETURNING id
                        """,
                UUID.class, UUID.randomUUID() + "@b06.invalid", employeeRole);

        List<Integer> categoryIds = jdbc.queryForList(
                "SELECT id FROM categories ORDER BY id LIMIT 2", Integer.class);
        liveCategoryId = categoryIds.get(0);
        snapshotCategoryId = categoryIds.get(1);

        recordId = jdbc.queryForObject("""
                        INSERT INTO records (
                            title, description, category_id, status, created_by, assigned_to, last_deputy_id,
                            snapshot_title, snapshot_description, snapshot_category_id, snapshot_at
                        )
                        VALUES (
                            'B06 canli baslik ' || ?, 'canli aciklama', ?, 'DUZENLEME_BEKLIYOR', ?, ?, ?,
                            'B06 dondurulmus baslik', 'dondurulmus aciklama', ?, current_timestamp
                        )
                        RETURNING id
                        """,
                UUID.class, LIVE_ONLY_WORD, liveCategoryId, deputyId, assigneeId, deputyId, snapshotCategoryId);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM records WHERE id = ?", recordId);
        jdbc.update("DELETE FROM users WHERE id IN (?, ?)", deputyId, assigneeId);
    }

    @Test
    @DisplayName("devreden sadece canli baslikta gecen kelimeyi bulamaz, guncel sahibi bulur")
    void aramaGorunurIcerigeGoreCalisir() {
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        criteria.setQ(LIVE_ONLY_WORD);

        List<Record> asDeputy = recordRepository.findAll(
                RecordSpecifications.withFilters(criteria, scopeFor(deputyId)));
        List<Record> asAssignee = recordRepository.findAll(
                RecordSpecifications.withFilters(criteria, scopeFor(assigneeId)));

        assertThat(asDeputy)
                .as("Devreden dondurulmus icerigi goruyor; canli kelime onun icin yok sayilmali")
                .isEmpty();
        assertThat(asAssignee)
                .as("Guncel sahip canli icerigi goruyor; kelime onun icin bulunmali")
                .extracting(Record::getId)
                .containsExactly(recordId);
    }

    @Test
    @DisplayName("kategori filtresi de gorunen icerik surumune gore calisir")
    void kategoriFiltresiGorunurIcerigeGoreCalisir() {
        RecordSearchCriteria byLiveCategory = new RecordSearchCriteria();
        byLiveCategory.setCategoryId(liveCategoryId);

        RecordSearchCriteria bySnapshotCategory = new RecordSearchCriteria();
        bySnapshotCategory.setCategoryId(snapshotCategoryId);

        List<Record> deputyByLive = recordRepository.findAll(
                RecordSpecifications.withFilters(byLiveCategory, scopeFor(deputyId)));
        List<Record> deputyBySnapshot = recordRepository.findAll(
                RecordSpecifications.withFilters(bySnapshotCategory, scopeFor(deputyId)));
        List<Record> assigneeByLive = recordRepository.findAll(
                RecordSpecifications.withFilters(byLiveCategory, scopeFor(assigneeId)));
        List<Record> assigneeBySnapshot = recordRepository.findAll(
                RecordSpecifications.withFilters(bySnapshotCategory, scopeFor(assigneeId)));

        assertThat(deputyByLive)
                .as("Devreden canli kategoriyle aranirsa bulunmamali")
                .isEmpty();
        assertThat(deputyBySnapshot)
                .as("Devreden dondurulmus kategoriyle aranirsa bulunmali")
                .extracting(Record::getId)
                .containsExactly(recordId);
        assertThat(assigneeByLive)
                .as("Guncel sahip canli kategoriyle aranirsa bulunmali")
                .extracting(Record::getId)
                .containsExactly(recordId);
        assertThat(assigneeBySnapshot)
                .as("Guncel sahip dondurulmus kategoriyle aranirsa bulunmamali")
                .isEmpty();
    }

    /**
     * Dinamik bir aktorun (yerlesik Bsk. Yrd. ROL ayricaligi OLMADAN) tek
     * iliskisi PREVIOUS_DEPUTY uzerinden kurulur - actorId == lastDeputyId
     * oldugunda eslesir. statuses() bilerek bos birakildi: bu test yalniz
     * "ilettigim kaydi gorurum" iliskisini sinamak istiyor, yerlesik rolun
     * rol-geneli kuyruk ayricaligini degil (bkz. RecordVisibilityScope,
     * RecordSpecifications.seesFrozenContent).
     */
    private RecordVisibilityScope scopeFor(UUID actorId) {
        return new RecordVisibilityScope(
                actorId,
                EnumSet.of(RecordVisibilityScope.Relation.CREATOR,
                        RecordVisibilityScope.Relation.ASSIGNEE,
                        RecordVisibilityScope.Relation.PREVIOUS_DEPUTY),
                Set.of(),
                Set.of());
    }
}