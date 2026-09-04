package btk.staj.WorkFlowProject.search;

import static btk.staj.WorkFlowProject.support.AuthorizationFixtures.visibility;

import btk.staj.WorkFlowProject.rbac.Role;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.search.specification.RecordSpecifications;
import btk.staj.WorkFlowProject.user.entity.User;
import btk.staj.WorkFlowProject.user.repository.RoleRepository;
import btk.staj.WorkFlowProject.user.repository.UserRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Proje Postgres'e ozgu ozellikler (Flyway migration, citext, jsonb, custom
// index'ler) kullaniyor. @DataJpaTest varsayilan olarak DataSource'u embedded
// bir DB ile degistirmeye calisiyor; classpath'te H2/HSQLDB olmadigi icin
// "Failed to replace DataSource with an embedded database" hatasi veriyordu.
// Replace.NONE ile application.properties'teki gercek Postgres kullaniliyor;
// bu, projedeki diger integration testleriyle (@SpringBootTest) tutarlidir.
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class RecordRepositorySortingTest {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldSortRecordsByTitleAscending() {

        // Arrange
        // records.created_by, users(id) FK'sini tasiyor; rastgele bir UUID
        // kullanilirsa insert "fk_record_created_by" kisitini ihlal eder.
        // Bu yuzden once gercek bir kullanici satiri olusturulur.
        UUID userId = createUser();
        String testPrefix = "SORT_TEST_" + UUID.randomUUID();

        Record record1 = Record.builder()
                .title(testPrefix + "_B")
                .description("Sorting test B")
                .categoryId(5)
                .status(RecordStatus.TASLAK)
                .createdBy(userId)
                .build();

        Record record2 = Record.builder()
                .title(testPrefix + "_A")
                .description("Sorting test A")
                .categoryId(5)
                .status(RecordStatus.TASLAK)
                .createdBy(userId)
                .build();

        recordRepository.save(record1);
        recordRepository.save(record2);
        recordRepository.flush();

        RecordSearchCriteria criteria =
                new RecordSearchCriteria();

        criteria.setQ(testPrefix);

        PageRequest pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.ASC,
                        "title"));

        // Act
        // Kapsam olarak CALISAN kullaniliyor: iki kayit da userId tarafindan
        // olusturuldugu icin ikisi de gorunur. ADMIN kullanilamaz — sartname
        // geregi Admin evrak goremez, kapsam kosulu bos sonuc dondurur.
        Page<Record> result = recordRepository.findAll(
                RecordSpecifications.withFilters(
                        criteria,
                        new btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy(actor -> java.util.Set.of()).scopeFor(visibility(RoleName.CALISAN, userId))),
                pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        assertEquals(
                testPrefix + "_A",
                result.getContent().get(0).getTitle());
        assertEquals(
                testPrefix + "_B",
                result.getContent().get(1).getTitle());

        // Cleanup
        recordRepository.deleteAll(result.getContent());
        recordRepository.flush();
    }

    /** Kayitlarin baglanacagi gercek bir kullanici satiri uretir. */
    private UUID createUser() {
        // roles tablosu Flyway V1 ile dolduruluyor; CALISAN her ortamda var.
        Role calisan = roleRepository.findByName("CALISAN")
                .orElseThrow(() -> new IllegalStateException("CALISAN rolu bulunamadi"));

        User user = new User();
        user.setFirstName("Sirala");
        user.setLastName("Test");
        user.setEmail("sirala-test-" + UUID.randomUUID() + "@ornek.local");
        user.setPasswordHash("test-parola-ozeti");
        user.setRole(calisan);
        user.setActive(true);
        // users.created_at NOT NULL ve kolonun varsayilani yok; entity de
        // otomatik doldurmuyor (bkz. User#createdAt), bu yuzden elle verilir.
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.saveAndFlush(user).getId();
    }
}
