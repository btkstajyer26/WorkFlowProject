package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.support.AbstractTransitionRuleInvariants;
import btk.staj.WorkFlowProject.workflow.entity.WorkflowActionEntity;
import btk.staj.WorkFlowProject.workflow.entity.WorkflowStatusEntity;
import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowActionRepository;
import btk.staj.WorkFlowProject.workflow.repository.WorkflowStatusRepository;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import btk.staj.WorkFlowProject.workflow.statemachine.StaticTransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SM-9 &mdash; statik gecis tablosu ile veritabani kaynagi ayni sekiz kurali
 * uretmelidir.
 *
 * <p>Bu test refactor'un dogru yapildiginin tek kaniti. DB-1 SS17: "Eksik,
 * fazla veya farkli tek bir DB gecisi parity testini dusurur." Ayrisma
 * durumunda hangi satirin farkli oldugu gorunmelidir; AssertJ koleksiyon
 * karsilastirmalari eksik/fazla elemani {@link TransitionRule} record'u
 * olarak yazar.
 *
 * <p>Kaynak burada <strong>acikca</strong> kurulur, {@code TransitionRuleSource}
 * bean'inden alinmaz: DB-1 SS13.2 madde 8 geregi parity, production bean'i
 * cevirmenin on kosuludur; bean'e bagimli olsaydi kendi on kosulunu
 * dogrulayamazdi.
 *
 * <p>TZ-1 ile ayrica {@link AbstractTransitionRuleInvariants} miras alinir: statik tablo
 * uzerinde kosan kaynak-agnostik kontroller ayni sekilde <em>gercek veritabani</em>
 * kaynagina da uygulanir. Bu miras parity karsilastirmasinin yerine gecmez, onu tamamlar.
 *
 * <p>Gercek PostgreSQL gerektirir (projede Testcontainers yok):
 * {@code docker compose up -d db} ve {@code DB_PORT} degeri {@code .env} ile
 * ayni olmalidir.
 */
@SpringBootTest
@Transactional // Pasiflestirme senaryosu veriyi degistirir; test sonunda geri alinir.
@DisplayName("Static-DB gecis kurali paritesi")
class TransitionRuleSourceParityTest extends AbstractTransitionRuleInvariants {

    private static final int EXPECTED_RULE_COUNT = 8;

    @Autowired
    private TransitionRuleRecordReader reader;

    @Autowired
    private TransitionRuleSource injectedRuleSource;

    @Autowired
    private WorkflowStatusRepository statusRepository;

    @Autowired
    private WorkflowActionRepository actionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TransitionRuleSource staticSource;
    private Map<RoleName, RoleId> referenceRoleIds;

    @BeforeEach
    void resolveReferenceIdentitiesFromDatabase() {
        Map<RoleName, RoleId> ids = new EnumMap<>(RoleName.class);
        jdbcTemplate.query("SELECT system_key, id FROM roles WHERE system_key IS NOT NULL", rs -> {
            ids.put(RoleName.valueOf(rs.getString("system_key")), new RoleId(rs.getInt("id")));
        });
        assertThat(ids).containsKeys(RoleName.values());
        staticSource = new StaticTransitionRuleSource(ids);
        referenceRoleIds = Map.copyOf(ids);
    }

    @Test
    @DisplayName("veritabani sekiz aktif kural uretir")
    void databaseProducesEightRules() {
        assertThat(databaseSource().all())
                .as("workflow_transitions icindeki aktif kural sayisi")
                .hasSize(EXPECTED_RULE_COUNT);
    }

    @Test
    @DisplayName("DB ve statik kural kumeleri birebir aynidir")
    void databaseAndStaticRuleSetsAreIdentical() {
        List<TransitionRule> databaseRules = databaseSource().all();
        List<TransitionRule> staticRules = staticSource.all();

        assertThat(databaseRules)
                .as("statik tabloda olup DB'de olmayan veya DB'de fazladan bulunan kural")
                .containsExactlyInAnyOrderElementsOf(staticRules);
        assertThat(staticRules)
                .as("DB'de olup statik tabloda olmayan kural")
                .containsExactlyInAnyOrderElementsOf(databaseRules);
    }

    /**
     * Kabul kriterinin cekirdegi. Sadece iki kumeyi karsilastirmak, iki kaynagin
     * ayni <em>sorguya</em> ayni cevabi verdigini kanitlamaz; bu test butun
     * durum-aksiyon-rol uzayini (6 x 7 x 4) tarar ve negatif cevaplari da
     * karsilastirir. Fazladan bir DB satiri burada da yakalanir.
     */
    @Test
    @DisplayName("her durum-aksiyon-rol birlesimi icin find ayni sonucu verir")
    void findAgreesForEveryCombination() {
        TransitionRuleSource databaseSource = databaseSource();

        for (RecordStatus from : RecordStatus.values()) {
            for (WorkflowAction action : WorkflowAction.values()) {
                for (RoleName actorRole : RoleName.values()) {
                    Optional<TransitionRule> fromDatabase = databaseSource.find(from, action, referenceRoleIds.get(actorRole));
                    Optional<TransitionRule> fromStatic = staticSource.find(from, action, referenceRoleIds.get(actorRole));

                    assertThat(fromDatabase)
                            .as("kural araması: %s + %s + %s", from, action, actorRole)
                            .isEqualTo(fromStatic);
                }
            }
        }
    }

    @Test
    @DisplayName("durum katalogu RecordStatus enum'u ile uyumludur")
    void statusCatalogMatchesEnum() {
        List<WorkflowStatusEntity> statuses = statusRepository.findAll();

        assertThat(statuses).extracting(WorkflowStatusEntity::getName)
                .as("workflow_statuses.name kumesi")
                .containsExactlyInAnyOrder(names(RecordStatus.values()));

        // DB-1 SS6.4: bayraklar DB'ye tasindi ama kod hala enum'dan okuyor.
        // Parity, cevirme zamani geldiginde ikisinin ayrismis olmasini onler.
        for (WorkflowStatusEntity status : statuses) {
            RecordStatus expected = RecordStatus.valueOf(status.getName());

            assertThat(status.isTerminal())
                    .as("%s.is_terminal", status.getName())
                    .isEqualTo(expected.isTerminal());
            assertThat(status.isEditableByCreator())
                    .as("%s.is_editable_by_creator", status.getName())
                    .isEqualTo(expected.isEditableByCreator());
        }
    }

    @Test
    @DisplayName("aksiyon katalogu WorkflowAction enum'u ile uyumludur")
    void actionCatalogMatchesEnum() {
        List<WorkflowActionEntity> actions = actionRepository.findAll();

        assertThat(actions).extracting(WorkflowActionEntity::getName)
                .as("workflow_actions.name kumesi")
                .containsExactlyInAnyOrder(names(WorkflowAction.values()));

        for (WorkflowActionEntity action : actions) {
            WorkflowAction expected = WorkflowAction.valueOf(action.getName());

            assertThat(action.isCommentRequired())
                    .as("%s.comment_required", action.getName())
                    .isEqualTo(expected.isCommentRequired());
        }
    }

    /**
     * Testin gercekten ayrisma yakaladiginin kaniti. Bir gecis pasiflestirildiginde
     * kaynak yedi kural uretmeli ve parity dusmelidir; aksi halde yukaridaki
     * yesil sonuclar bir sey kanitlamazdi.
     */
    @Test
    @DisplayName("pasiflestirilen gecis kural kumesinden duser ve parity bozulur")
    void deactivatedTransitionBreaksParity() {
        int updated = jdbcTemplate.update("""
                UPDATE workflow_transitions
                SET is_active = FALSE
                WHERE id = (SELECT MIN(id) FROM workflow_transitions WHERE is_active)
                """);
        assertThat(updated).as("pasiflestirilen satir sayisi").isEqualTo(1);

        List<TransitionRule> databaseRules = databaseSource().all();

        assertThat(databaseRules).hasSize(EXPECTED_RULE_COUNT - 1);
        assertThat(staticSource.all())
                .as("pasiflestirilen kural artik DB kumesinde bulunmamali")
                .anyMatch(rule -> !databaseRules.contains(rule));
    }

    @Test
    @DisplayName("production bean gecis kurallarini veritabanindan okur")
    void productionBeanUsesDatabaseSource() {
        // WF-4 ile bean artik tazelenebilir sarmalayici; sardigi snapshot yine DB kaynagi.
        assertThat(injectedRuleSource)
                .as("WorkflowConfiguration#transitionRuleSource bean'i")
                .isInstanceOf(ReloadableTransitionRuleSource.class);
        assertThat(((ReloadableTransitionRuleSource) injectedRuleSource).current())
                .as("sarmalayicinin kullandigi snapshot")
                .isInstanceOf(DbTransitionRuleSource.class);

        assertThat(injectedRuleSource.all())
                .as("production bean'in urettigi kural kumesi")
                .containsExactlyInAnyOrderElementsOf(staticSource.all());
    }

    /**
     * WF-4: tazeleme kural kumesini degistirmemeli. Veri ayni kaldigi surece reload
     * oncesi ve sonrasi kume birebir aynidir; parity reload'dan sonra da gecerlidir.
     */
    @Test
    @DisplayName("reload kural kumesini degistirmez")
    void reloadKeepsTheSameRuleSet() {
        ReloadableTransitionRuleSource reloadable = (ReloadableTransitionRuleSource) injectedRuleSource;
        List<TransitionRule> before = List.copyOf(reloadable.all());

        int count = reloadable.reload();

        assertThat(count).isEqualTo(EXPECTED_RULE_COUNT);
        assertThat(reloadable.all())
                .as("reload sonrasi kural kumesi")
                .containsExactlyInAnyOrderElementsOf(before);
    }

    /**
     * Hedef metadata'si {@code TransitionRule}'un parcasi oldugu icin yukaridaki kume
     * karsilastirmalari onu zaten kapsiyor. Bu test ayrismanin <em>hangi alanda</em>
     * oldugunu okunur kilar: strateji ya da beklenen rol kaydigi anda mesaj dogrudan o
     * gecisi ve o alani gosterir.
     */
    @Test
    @DisplayName("hedef stratejisi ve beklenen hedef rol iki kaynakta aynidir")
    void targetMetadataMatchesBetweenSources() {
        TransitionRuleSource databaseSource = databaseSource();

        for (TransitionRule staticRule : staticSource.all()) {
            Optional<TransitionRule> fromDatabase = databaseSource.find(staticRule.from(), staticRule.action(), staticRule.actorRoleId());

            assertThat(fromDatabase)
                    .as("kural: %s + %s + %s", staticRule.from(), staticRule.action(), staticRule.actorRoleId())
                    .isPresent();
            assertThat(fromDatabase.get().targetStrategy())
                    .as("%s + %s + %s hedef stratejisi", staticRule.from(), staticRule.action(), staticRule.actorRoleId())
                    .isEqualTo(staticRule.targetStrategy());
            assertThat(fromDatabase.get().expectedTargetRoleId())
                    .as("%s + %s + %s beklenen hedef rolu", staticRule.from(), staticRule.action(), staticRule.actorRoleId())
                    .isEqualTo(staticRule.expectedTargetRoleId());
        }
    }

    /**
     * Her testte yeniden kurulur: {@link DbTransitionRuleSource} veriyi
     * constructor'da okuyup dondurur, dolayisiyla ornek olusturmak "su anki
     * veritabani durumunu oku" demektir.
     */
    private TransitionRuleSource databaseSource() {
        return new DbTransitionRuleSource(reader);
    }

    /**
     * Miras alinan invariantlar gercek veritabani kaynagini denetler. Her cagrida taze
     * kurulur; aksi halde ayni test icinde yapilan veri degisikligi gorunmez olurdu.
     */
    @Override
    protected TransitionRuleSource ruleSource() {
        return databaseSource();
    }

    @Override
    protected RoleId nonActorRoleId() {
        return referenceRoleIds.get(RoleName.ADMIN);
    }

    private static String[] names(Enum<?>[] values) {
        String[] names = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            names[index] = values[index].name();
        }
        return names;
    }
}
