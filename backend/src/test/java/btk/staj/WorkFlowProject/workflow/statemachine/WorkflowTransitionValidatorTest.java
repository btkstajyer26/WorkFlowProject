package btk.staj.WorkFlowProject.workflow.statemachine;

import btk.staj.WorkFlowProject.support.AuthorizationFixtures;
import btk.staj.WorkFlowProject.support.WorkflowRoleFixtures;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Durum makinesi kural motorunun birim testleri.
 *
 * <p>Negatif testler yalnizca "reddedildi" degil, dondurulen hata kodunu da
 * dogrular; boylece kontrol sirasi degistiginde test kirilir.
 */
@DisplayName("WorkflowTransitionValidator")
class WorkflowTransitionValidatorTest {

    private final WorkflowTransitionValidator validator =
            new WorkflowTransitionValidator(new StaticTransitionRuleSource(WorkflowRoleFixtures.roleIds()));

    @Test
    void equalRoleIdsInDifferentObjectsMatchActorAndTarget() {
        var ids = java.util.Map.of(RoleName.CALISAN, new RoleId(1001),
                RoleName.BASKAN_YARDIMCISI, new RoleId(2002), RoleName.BASKAN, new RoleId(3003));
        var source = new StaticTransitionRuleSource(ids);
        var context = new TransitionContext(RecordStatus.TASLAK, WorkflowAction.GONDER,
                new RoleId(1001), true, false, null, false, new RoleId(2002), true,
                true, java.util.Set.of("RECORD_FORWARD"));

        assertThat(context.actorRoleId()).isNotSameAs(ids.get(RoleName.CALISAN));
        assertThat(context.targetRoleId()).isNotSameAs(ids.get(RoleName.BASKAN_YARDIMCISI));
        assertThat(new WorkflowTransitionValidator(source).validate(context))
                .isEqualTo(TransitionDecision.allowed(RecordStatus.BSK_YRD_INCELEMESINDE));
    }

    // ------------------------------------------------------------------
    // Pozitif gecisler - gecis matrisindeki sekiz satir
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("izinli gecisler")
    class AllowedTransitions {

        @Test
        @DisplayName("Calisan kendi taslagini Baskan Yardimcisina gonderebilir")
        void calisanTaslagiGonderir() {
            TransitionContext context = Ctx.of(RecordStatus.TASLAK, WorkflowAction.GONDER, RoleName.CALISAN)
                    .creator()
                    .resolvedTarget(RoleName.BASKAN_YARDIMCISI)
                    .build();

            assertAllowed(context, RecordStatus.BSK_YRD_INCELEMESINDE);
        }

        @Test
        @DisplayName("Calisan duzenleme bekleyen kendi kaydini yeniden gonderebilir")
        void calisanTekrarGonderir() {
            TransitionContext context = Ctx.of(RecordStatus.DUZENLEME_BEKLIYOR, WorkflowAction.TEKRAR_GONDER, RoleName.CALISAN)
                    .creator()
                    .assignee()
                    .resolvedTarget(RoleName.BASKAN_YARDIMCISI)
                    .build();

            assertAllowed(context, RecordStatus.BSK_YRD_INCELEMESINDE);
        }

        @Test
        @DisplayName("Atanmis Baskan Yardimcisi kaydi Baskana iletebilir")
        void yardimciBaskanaIletir() {
            TransitionContext context = Ctx.of(RecordStatus.BSK_YRD_INCELEMESINDE, WorkflowAction.BASKANA_ILET, RoleName.BASKAN_YARDIMCISI)
                    .assignee()
                    .resolvedTarget(RoleName.BASKAN)
                    .build();

            assertAllowed(context, RecordStatus.BASKAN_INCELEMESINDE);
        }

        @Test
        @DisplayName("Atanmis Baskan Yardimcisi aciklamayla Calisana geri gonderebilir")
        void yardimciCalisanaGeriGonderir() {
            TransitionContext context = Ctx.of(RecordStatus.BSK_YRD_INCELEMESINDE, WorkflowAction.CALISANA_GERI_GONDER, RoleName.BASKAN_YARDIMCISI)
                    .assignee()
                    .comment("Butce kalemini ekleyiniz.")
                    .resolvedTarget(RoleName.CALISAN)
                    .build();

            assertAllowed(context, RecordStatus.DUZENLEME_BEKLIYOR);
        }

        @Test
        @DisplayName("Atanmis Baskan kaydi onaylayabilir")
        void baskanOnaylar() {
            TransitionContext context = Ctx.of(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.ONAYLA, RoleName.BASKAN)
                    .assignee()
                    .build();

            assertAllowed(context, RecordStatus.ONAYLANDI);
        }

        @Test
        @DisplayName("Atanmis Baskan aciklamayla kaydi reddedebilir")
        void baskanReddeder() {
            TransitionContext context = Ctx.of(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.REDDET, RoleName.BASKAN)
                    .assignee()
                    .comment("Talep uygun bulunmamistir.")
                    .build();

            assertAllowed(context, RecordStatus.REDDEDILDI);
        }

        @Test
        @DisplayName("Atanmis Baskan aciklamayla Calisana geri gonderebilir")
        void baskanCalisanaGeriGonderir() {
            TransitionContext context = Ctx.of(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.CALISANA_GERI_GONDER, RoleName.BASKAN)
                    .assignee()
                    .comment("Ek belge gerekmektedir.")
                    .resolvedTarget(RoleName.CALISAN)
                    .build();

            assertAllowed(context, RecordStatus.DUZENLEME_BEKLIYOR);
        }

        @Test
        @DisplayName("Atanmis Baskan aciklamayla kaydi ileten yardimciya geri gonderebilir")
        void baskanYardimciyaGeriGonderir() {
            TransitionContext context = Ctx.of(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.BASKAN_YARDIMCISINA_GERI_GONDER, RoleName.BASKAN)
                    .assignee()
                    .comment("Tekrar inceleyiniz.")
                    .resolvedTarget(RoleName.BASKAN_YARDIMCISI)
                    .build();

            assertAllowed(context, RecordStatus.BSK_YRD_INCELEMESINDE);
        }
    }

    // ------------------------------------------------------------------
    // Kayit sahipligi ve atanmislik
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("kayit iliskisi kontrolleri")
    class RecordRelation {

        @Test
        @DisplayName("Calisan baskasinin kaydini gonderemez")
        void baskasininKaydiGonderilemez() {
            TransitionContext context = Ctx.of(RecordStatus.TASLAK, WorkflowAction.GONDER, RoleName.CALISAN)
                    .targetInRequest(RoleName.BASKAN_YARDIMCISI)
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_FORBIDDEN);
        }

        @Test
        @DisplayName("Duzenleme bekleyen kayit baska bir Calisan tarafindan yeniden gonderilemez")
        void baskasininKaydiTekrarGonderilemez() {
            TransitionContext context = Ctx.of(RecordStatus.DUZENLEME_BEKLIYOR, WorkflowAction.TEKRAR_GONDER, RoleName.CALISAN)
                    .creator() // sahibi ama atanani degil
                    .targetInRequest(RoleName.BASKAN_YARDIMCISI)
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_FORBIDDEN);
        }

        @Test
        @DisplayName("Atanmamis Baskan Yardimcisi kaydi isleyemez")
        void atanmamisYardimciIsleyemez() {
            TransitionContext context = Ctx.of(RecordStatus.BSK_YRD_INCELEMESINDE, WorkflowAction.BASKANA_ILET, RoleName.BASKAN_YARDIMCISI)
                    .resolvedTarget(RoleName.BASKAN)
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_FORBIDDEN);
        }

        @Test
        @DisplayName("Atanmamis Baskan kaydi isleyemez")
        void atanmamisBaskanIsleyemez() {
            TransitionContext context = Ctx.of(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.ONAYLA, RoleName.BASKAN)
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_FORBIDDEN);
        }
    }

    // ------------------------------------------------------------------
    // Tabloda olmayan birlesimler
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("gecersiz durum-aksiyon-rol birlesimleri")
    class InvalidCombinations {

        @Test
        @DisplayName("Baskan taslak kaydi dogrudan onaylayamaz")
        void baskanTaslagiOnaylayamaz() {
            TransitionContext context = Ctx.of(RecordStatus.TASLAK, WorkflowAction.ONAYLA, RoleName.BASKAN)
                    .assignee()
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_INVALID_TRANSITION);
        }

        @Test
        @DisplayName("Baskan Yardimcisi nihai onay veremez")
        void yardimciOnaylayamaz() {
            TransitionContext context = Ctx.of(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.ONAYLA, RoleName.BASKAN_YARDIMCISI)
                    .assignee()
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_INVALID_TRANSITION);
        }

        @Test
        @DisplayName("Baskan Yardimcisi kaydi reddedemez")
        void yardimciReddedemez() {
            TransitionContext context = Ctx.of(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.REDDET, RoleName.BASKAN_YARDIMCISI)
                    .assignee()
                    .comment("Uygun degil.")
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_INVALID_TRANSITION);
        }

        @Test
        @DisplayName("Calisan kaydi dogrudan Baskana iletemez")
        void calisanBaskanaIletemez() {
            TransitionContext context = Ctx.of(RecordStatus.BSK_YRD_INCELEMESINDE, WorkflowAction.BASKANA_ILET, RoleName.CALISAN)
                    .creator()
                    .assignee()
                    .resolvedTarget(RoleName.BASKAN)
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_INVALID_TRANSITION);
        }
    }

    // ------------------------------------------------------------------
    // Aciklama zorunlulugu
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("aciklama zorunlulugu")
    class CommentRules {

        @Test
        @DisplayName("Aciklamasiz geri gonderme reddedilir")
        void aciklamasizGeriGonderme() {
            TransitionContext context = Ctx.of(RecordStatus.BSK_YRD_INCELEMESINDE, WorkflowAction.CALISANA_GERI_GONDER, RoleName.BASKAN_YARDIMCISI)
                    .assignee()
                    .resolvedTarget(RoleName.CALISAN)
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_COMMENT_REQUIRED);
        }

        @Test
        @DisplayName("Aciklamasiz red reddedilir")
        void aciklamasizRed() {
            TransitionContext context = Ctx.of(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.REDDET, RoleName.BASKAN)
                    .assignee()
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_COMMENT_REQUIRED);
        }

        @Test
        @DisplayName("Yalniz bosluktan olusan aciklama gecersizdir")
        void bosluktanIbaretAciklama() {
            TransitionContext context = Ctx.of(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.REDDET, RoleName.BASKAN)
                    .assignee()
                    .comment("   \t  ")
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_COMMENT_REQUIRED);
        }

        @Test
        @DisplayName("Aciklama istege bagli olan aksiyonlarda bos birakilabilir")
        void istegeBagliAciklama() {
            TransitionContext context = Ctx.of(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.ONAYLA, RoleName.BASKAN)
                    .assignee()
                    .build();

            assertAllowed(context, RecordStatus.ONAYLANDI);
        }
    }

    // ------------------------------------------------------------------
    // Hedef kullanici kurallari
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("hedef kullanici kurallari")
    class TargetRules {

        /**
         * Hedefi artik GONDER icin de backend cozuyor; istemci yine de hedef
         * gonderirse istek sessizce yok sayilmaz, reddedilir (Karar 4).
         */
        @Test
        @DisplayName("Gonderme isteginde hedef gonderilirse reddedilir")
        void gondermedeHedefGonderilemez() {
            TransitionContext context = Ctx.of(RecordStatus.TASLAK, WorkflowAction.GONDER, RoleName.CALISAN)
                    .creator()
                    .targetInRequest(RoleName.BASKAN_YARDIMCISI)
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_TARGET_NOT_ALLOWED);
        }

        @Test
        @DisplayName("Hedef gerektirmeyen aksiyonda hedef gonderilirse reddedilir")
        void gereksizHedef() {
            TransitionContext context = Ctx.of(RecordStatus.BASKAN_INCELEMESINDE, WorkflowAction.ONAYLA, RoleName.BASKAN)
                    .assignee()
                    .targetInRequest(RoleName.BASKAN_YARDIMCISI)
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_TARGET_NOT_ALLOWED);
        }

        @Test
        @DisplayName("Baskana iletme isteginde hedef gonderilirse reddedilir")
        void baskanaIletmedeHedefGonderilemez() {
            TransitionContext context = Ctx.of(RecordStatus.BSK_YRD_INCELEMESINDE, WorkflowAction.BASKANA_ILET, RoleName.BASKAN_YARDIMCISI)
                    .assignee()
                    .targetInRequest(RoleName.BASKAN)
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_TARGET_NOT_ALLOWED);
        }

        @Test
        @DisplayName("Yanlis roldeki hedef reddedilir")
        void yanlisRoldeHedef() {
            TransitionContext context = Ctx.of(RecordStatus.TASLAK, WorkflowAction.GONDER, RoleName.CALISAN)
                    .creator()
                    .resolvedTarget(RoleName.BASKAN) // Bskn. Yrd. bekleniyordu
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_TARGET_ROLE_INVALID);
        }

        @Test
        @DisplayName("ADMIN hedef kullanici olarak secilemez")
        void adminHedefOlamaz() {
            TransitionContext context = Ctx.of(RecordStatus.TASLAK, WorkflowAction.GONDER, RoleName.CALISAN)
                    .creator()
                    .resolvedTarget(RoleName.ADMIN)
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_TARGET_ROLE_INVALID);
        }

        @Test
        @DisplayName("Pasif hedef kullanici secilemez")
        void pasifHedefSecilemez() {
            TransitionContext context = Ctx.of(RecordStatus.TASLAK, WorkflowAction.GONDER, RoleName.CALISAN)
                    .creator()
                    .resolvedTarget(RoleName.BASKAN_YARDIMCISI)
                    .targetInactive()
                    .build();

            assertRejected(context, WorkflowErrorCode.WORKFLOW_TARGET_INACTIVE);
        }
    }

    // ------------------------------------------------------------------
    // Kilitleme ve ADMIN siniri
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("kilitleme ve rol siniri")
    class LockingAndRoleBoundary {

        @Test
        @DisplayName("Terminal kayitta hicbir aksiyon uygulanamaz")
        void terminalKayitKilitli() {
            List<String> unexpected = new ArrayList<>();

            for (RecordStatus status : RecordStatus.values()) {
                if (!status.isTerminal()) {
                    continue;
                }
                for (WorkflowAction action : WorkflowAction.values()) {
                    for (RoleName role : RoleName.values()) {
                        if (!role.isWorkflowActor()) {
                            continue;
                        }
                        TransitionContext context = Ctx.of(status, action, role)
                                .creator()
                                .assignee()
                                .comment("aciklama")
                                .resolvedTarget(expectedTargetRoleOf(status, action, role))
                                .build();

                        TransitionDecision decision = validator.validate(context);
                        boolean locked = decision instanceof TransitionDecision.Rejected rejected
                                && rejected.errorCode() == WorkflowErrorCode.WORKFLOW_RECORD_LOCKED;
                        if (!locked) {
                            unexpected.add(status + " / " + action + " / " + role + " -> " + decision);
                        }
                    }
                }
            }

            assertThat(unexpected)
                    .as("terminal durumdaki her birlesim WORKFLOW_RECORD_LOCKED dondurmeli")
                    .isEmpty();
        }

        @Test
        @DisplayName("ADMIN hicbir workflow aksiyonunu yapamaz")
        void adminAktorOlamaz() {
            List<String> unexpected = new ArrayList<>();

            for (RecordStatus status : RecordStatus.values()) {
                for (WorkflowAction action : WorkflowAction.values()) {
                    TransitionContext context = Ctx.of(status, action, RoleName.ADMIN)
                            .creator()
                            .assignee()
                            .comment("aciklama")
                            .resolvedTarget(expectedTargetRoleOf(status, action, RoleName.ADMIN))
                            .build();

                    TransitionDecision decision = validator.validate(context);
                    boolean blocked = decision instanceof TransitionDecision.Rejected rejected
                            && rejected.errorCode() == WorkflowErrorCode.WORKFLOW_ROLE_NOT_ALLOWED;
                    if (!blocked) {
                        unexpected.add(status + " / " + action + " -> " + decision);
                    }
                }
            }

            assertThat(unexpected)
                    .as("ADMIN her birlesimde WORKFLOW_ROLE_NOT_ALLOWED almali")
                    .isEmpty();
        }
    }

    // ------------------------------------------------------------------
    // Yardimcilar
    // ------------------------------------------------------------------

    private void assertAllowed(TransitionContext context, RecordStatus expectedTarget) {
        TransitionDecision decision = validator.validate(context);

        assertThat(decision)
                .isInstanceOf(TransitionDecision.Allowed.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(TransitionDecision.Allowed.class))
                .extracting(TransitionDecision.Allowed::targetStatus)
                .isEqualTo(expectedTarget);
    }

    private void assertRejected(TransitionContext context, WorkflowErrorCode expectedCode) {
        TransitionDecision decision = validator.validate(context);

        assertThat(decision)
                .isInstanceOf(TransitionDecision.Rejected.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(TransitionDecision.Rejected.class))
                .extracting(TransitionDecision.Rejected::errorCode)
                .isEqualTo(expectedCode);
    }

    @Test
    @DisplayName("baglam olusturucusu zorunlu alanlari dogrular")
    void baglamZorunluAlanlar() {
        assertThatCode(() -> new TransitionContext(
                null,
                WorkflowAction.ONAYLA,
                WorkflowRoleFixtures.id(RoleName.BASKAN),
                false,
                false,
                null,
                false,
                null,
                true,
                AuthorizationFixtures.workflowActor(RoleName.BASKAN),
                AuthorizationFixtures.permissions(RoleName.BASKAN)))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * Surum catismasi bir gecis kurali degildir: kayit baskasi tarafindan
     * degistirildiginde istek durum makinesi acisindan hala gecerlidir, yalnizca
     * dayandigi surum eskimistir. Kod bu yuzden yalnizca
     * {@code WorkflowRecordPort} uygulamasindan gelir.
     *
     * <p>Bu testler, {@code WORKFLOW_VERSION_CONFLICT} eklendikten sonra da
     * gecis kurallarinin degismedigini ve kodun kural motoruna sizmadigini
     * korur.
     */
    @Nested
    @DisplayName("surum catismasi durum makinesinin sozlugunde degildir")
    class VersionConflictIsNotATransitionRule {

        private final List<Boolean> bayraklar = List.of(true, false);
        private final List<String> aciklamalar = Arrays.asList(null, "", "   ", "aciklama");
        private final List<RoleName> hedefRolleri = Arrays.asList(
                null, RoleName.CALISAN, RoleName.BASKAN_YARDIMCISI, RoleName.BASKAN, RoleName.ADMIN);

        @Test
        @DisplayName("hicbir girdi birlesimi WORKFLOW_VERSION_CONFLICT uretmez")
        void hicbirBirlesimSurumCatismasiUretmez() {
            List<WorkflowErrorCode> uretilenKodlar = tumBirlesimlerinRetKodlari();

            assertThat(uretilenKodlar)
                    .as("kural motoru en az bir ret uretmeli, aksi halde test bosuna gecer")
                    .isNotEmpty();
            assertThat(uretilenKodlar).doesNotContain(WorkflowErrorCode.WORKFLOW_VERSION_CONFLICT);
        }

        @Test
        @DisplayName("izinli gecis sayisi surum catismasi eklendikten sonra da sekizdir")
        void izinliGecisSayisiDegismedi() {
            long izinliBirlesimSayisi = 0;

            for (RecordStatus status : RecordStatus.values()) {
                for (WorkflowAction action : WorkflowAction.values()) {
                    for (RoleName actorRole : RoleName.values()) {
                        if (WorkflowRoleFixtures.rules().find(status, action, WorkflowRoleFixtures.id(actorRole)).isPresent()) {
                            izinliBirlesimSayisi++;
                        }
                    }
                }
            }

            assertThat(izinliBirlesimSayisi).isEqualTo(8);
        }

        private List<WorkflowErrorCode> tumBirlesimlerinRetKodlari() {
            List<WorkflowErrorCode> kodlar = new ArrayList<>();

            for (RecordStatus status : RecordStatus.values()) {
                for (WorkflowAction action : WorkflowAction.values()) {
                    for (RoleName actorRole : RoleName.values()) {
                        for (boolean olusturan : bayraklar) {
                            for (boolean atanan : bayraklar) {
                                for (String aciklama : aciklamalar) {
                                    for (boolean hedefGonderildi : bayraklar) {
                                        for (RoleName hedefRol : hedefRolleri) {
                                            for (boolean hedefAktif : bayraklar) {
                                                TransitionDecision karar = validator.validate(
                                                        new TransitionContext(
                                                                status,
                                                                action,
                                                                WorkflowRoleFixtures.id(actorRole),
                                                                olusturan,
                                                                atanan,
                                                                aciklama,
                                                                hedefGonderildi,
                                                                WorkflowRoleFixtures.id(hedefRol),
                                                                hedefAktif,
                                                                AuthorizationFixtures.workflowActor(actorRole),
                                                                AuthorizationFixtures.permissions(actorRole)));

                                                if (karar instanceof TransitionDecision.Rejected ret) {
                                                    kodlar.add(ret.errorCode());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return kodlar;
        }
    }

    /** Testlerde okunabilir baglam olusturmak icin kucuk yardimci. */
    /**
     * Beklenen hedef rol artik aksiyonun degil GECISIN ozelligi; testler de onu kuraldan
     * okumali. Tanimsiz birlesimlerde {@code null} doner &mdash; zaten hedef kontrolune
     * gelinmeden once reddedilirler.
     */
    private static RoleId expectedTargetRoleOf(RecordStatus status, WorkflowAction action, RoleName role) {
        return WorkflowRoleFixtures.rules().find(status, action, WorkflowRoleFixtures.id(role))
                .map(TransitionRule::expectedTargetRoleId)
                .orElse(null);
    }

    private static final class Ctx {

        private final RecordStatus status;
        private final WorkflowAction action;
        private final RoleName actorRole;

        private boolean isCreator;
        private boolean isAssignee;
        private String comment;
        private boolean targetProvidedInRequest;
        private RoleId targetRole;
        private boolean targetActive = true;

        private Ctx(RecordStatus status, WorkflowAction action, RoleName actorRole) {
            this.status = status;
            this.action = action;
            this.actorRole = actorRole;
        }

        static Ctx of(RecordStatus status, WorkflowAction action, RoleName actorRole) {
            return new Ctx(status, action, actorRole);
        }

        Ctx creator() {
            this.isCreator = true;
            return this;
        }

        Ctx assignee() {
            this.isAssignee = true;
            return this;
        }

        Ctx comment(String value) {
            this.comment = value;
            return this;
        }

        /** Istemcinin istekte gonderdigi hedef; hicbir aksiyon icin beklenmiyor, reddedilir. */
        Ctx targetInRequest(RoleName role) {
            this.targetProvidedInRequest = true;
            this.targetRole = WorkflowRoleFixtures.id(role);
            return this;
        }

        /** Servisin kendi cozdugu hedef (istekte gonderilmez). */
        Ctx resolvedTarget(RoleName role) {
            this.targetRole = WorkflowRoleFixtures.id(role);
            return this;
        }

        Ctx resolvedTarget(RoleId role) {
            this.targetRole = role;
            return this;
        }

        Ctx targetInactive() {
            this.targetActive = false;
            return this;
        }

        TransitionContext build() {
            return new TransitionContext(
                    status,
                    action,
                    WorkflowRoleFixtures.id(actorRole),
                    isCreator,
                    isAssignee,
                    comment,
                    targetProvidedInRequest,
                    targetRole,
                    targetActive,
                    AuthorizationFixtures.workflowActor(actorRole),
                    AuthorizationFixtures.permissions(actorRole));
        }
    }
}
