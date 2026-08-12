package btk.staj.WorkFlowProject.audit.repository;

import btk.staj.WorkFlowProject.audit.entity.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional // Test bitince tum degisiklikler geri alinir, veritabani kirlenmez
class AuditLogRepositoryIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void auditLogGercektenVeritabaninaYaziliyorVeOkunuyor() {
        // Hazirlik: gercek bir kullanici ve kayit olusturmamiz lazim,
        // cunku audit_logs tablosu bunlara foreign key ile bagli.
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, first_name, last_name, email, password_hash, role_id) " +
                        "VALUES (?, 'Test', 'Kullanici', ?, 'sifre', 1)",
                userId, "test" + userId + "@test.com"
        );

        UUID recordId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO records (id, title, description, category_id, status, created_by) " +
                        "VALUES (?, 'Test Kayit', 'Test aciklama', 1, 'TASLAK', ?)",
                recordId, userId
        );

        // Test edilen islem: gercek repository ile kaydet
        AuditLog log = AuditLog.builder()
                .recordId(recordId)
                .userId(userId)
                .roleId(1)
                .action("OLUSTURULDU")
                .newStatus("TASLAK")
                .comment("Entegrasyon testi")
                .build();

        auditLogRepository.save(log);

        // Dogrulama: gercekten veritabanindan geri okunabiliyor mu?
        List<AuditLog> gecmis = auditLogRepository.findByRecordIdOrderByCreatedAtAsc(recordId);

        assertThat(gecmis).hasSize(1);
        assertThat(gecmis.get(0).getAction()).isEqualTo("OLUSTURULDU");
        assertThat(gecmis.get(0).getComment()).isEqualTo("Entegrasyon testi");
    }
}