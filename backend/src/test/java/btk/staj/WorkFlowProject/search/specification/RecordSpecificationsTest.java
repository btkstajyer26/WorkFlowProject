package btk.staj.WorkFlowProject.search.specification;

import static btk.staj.WorkFlowProject.support.AuthorizationFixtures.visibility;

import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.search.dto.RecordSearchCriteria;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleName;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kapsam kurali sartnamenin §2 "Kayit Gorunurlugu Kapsami" satiri; yanlis
 * uygulanirsa kullanici baskasinin evragini gorur. Bu yuzden hangi kolona
 * bakildigi burada dogrulanir.
 */
@DisplayName("Arama gorunurluk kapsami")
class RecordSpecificationsTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000060");

    private final Root<Record> root = mock(Root.class, RETURNS_DEEP_STUBS);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

    @Test
    @DisplayName("Calisan yalnizca kendi olusturduklarini gorur")
    void anEmployeeIsScopedToTheirOwnRecords() {
        Path<Object> createdBy = pathFor("createdBy");

        build(RoleName.CALISAN);

        verify(cb).equal(createdBy, USER_ID);
    }

    @Test
    @DisplayName("Bsk. Yrd. kendisine atananlari, duzeltmedekileri ve elinden gecenleri gorur")
    void aDeputyIsScopedToTheirAssignments() {
        Path<Object> assignedTo = pathFor("assignedTo");
        Path<Object> status = pathFor("status");
        Path<Object> lastDeputyId = pathFor("lastDeputyId");

        build(RoleName.BASKAN_YARDIMCISI);

        verify(cb).equal(assignedTo, USER_ID);
        verify(cb).equal(status, RecordStatus.DUZENLEME_BEKLIYOR);
        verify(cb).equal(lastDeputyId, USER_ID);
    }

    /**
     * Kapsam kurali iki yerde duruyor: burada sorgu kosulu, RecordAccessPolicy'de
     * tek kayda bakan boolean. Bu kol yalnizca policy'ye eklenmis, sorguya
     * eklenmemisti; detay ucu kaydi aciyor ama liste onu hic dondurmuyordu.
     */
    @Test
    @DisplayName("Bsk. Yrd. Baskana ilettigi kaydi listede kaybetmez")
    void aDeputyKeepsSeeingRecordsTheyForwarded() {
        Path<Object> lastDeputyId = pathFor("lastDeputyId");

        build(RoleName.BASKAN_YARDIMCISI);

        verify(cb).equal(lastDeputyId, USER_ID);
    }

    @Test
    @DisplayName("Baskan onay asamasindakileri ve kendisine atananlari gorur")
    void aPresidentSeesRecordsAwaitingApprovalOrAssignedToThem() {
        Path<Object> status = pathFor("status");
        Path<Object> assignedTo = pathFor("assignedTo");

        build(RoleName.BASKAN);

        verify(cb).equal(status, RecordStatus.BASKAN_INCELEMESINDE);
        verify(cb).equal(assignedTo, USER_ID);
    }

    /**
     * ONAYLA/REDDET assignedTo'yu bosaltir. Sonuclanan iki durum kapsama
     * acikca yazilmazsa Baskan kendi verdigi karardan sonra kaydi kaybeder ve
     * "Onaylananlar"/"Reddedilenler" sekmeleri kalici olarak bos gorunur.
     */
    @Test
    @DisplayName("Baskan sonuclandirdigi kayitlari kendi karardan sonra da gorur")
    void aPresidentKeepsSeeingRecordsTheyDecided() {
        Path<Object> status = pathFor("status");

        build(RoleName.BASKAN);

        verify(cb).equal(status, RecordStatus.ONAYLANDI);
        verify(cb).equal(status, RecordStatus.REDDEDILDI);
    }

    @Test
    @DisplayName("Admin evrak goremez")
    void anAdminSeesNothing() {
        build(RoleName.ADMIN);

        verify(cb).disjunction();
        verify(cb, never()).equal(any(Path.class), any(UUID.class));
    }

    @Test
    @DisplayName("silinmis kayitlar hicbir rolde donmez")
    void softDeletedRecordsAreAlwaysExcluded() {
        Path<Object> deletedAt = pathFor("deletedAt");

        build(RoleName.CALISAN);

        verify(cb).isNull(deletedAt);
    }

    @Test
    @DisplayName("olusturan filtresi verilmezse alt sorgu kurulmaz")
    void noSubqueryWithoutTheCreatorFilter() {
        build(RoleName.CALISAN);

        verify(query, never()).subquery(UUID.class);
    }

    @Test
    @DisplayName("olusturan filtresi kapsami gevsetmez, uzerine AND'lenir")
    void theCreatorFilterOnlyNarrowsTheScope() {
        Path<Object> createdBy = pathFor("createdBy");
        givenSubquery();
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        criteria.setCreator("ahmet");

        RecordSpecifications.withFilters(criteria, new btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy(actor -> java.util.Set.of()).scopeFor(visibility(RoleName.CALISAN, USER_ID)))
                .toPredicate(root, query, cb);

        // Kapsam kosulu hala kuruluyor: filtre onun yerine gecmiyor, yanina ekleniyor.
        verify(cb).equal(createdBy, USER_ID);
        verify(query).subquery(UUID.class);
        verify(cb).exists(any());
    }

    @Test
    @DisplayName("bos olusturan filtresi yok sayilir")
    void aBlankCreatorFilterIsIgnored() {
        RecordSearchCriteria criteria = new RecordSearchCriteria();
        criteria.setCreator("   ");

        RecordSpecifications.withFilters(criteria, new btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy(actor -> java.util.Set.of()).scopeFor(visibility(RoleName.CALISAN, USER_ID)))
                .toPredicate(root, query, cb);

        verify(query, never()).subquery(UUID.class);
    }

    @Test
    @DisplayName("kullanici veya rol verilmeden olusturulamaz")
    void refusesToBuildWithoutAnActor() {
        RecordSearchCriteria criteria = new RecordSearchCriteria();

        assertThatNullPointerException().isThrownBy(
                () -> RecordSpecifications.withFilters(criteria, null));
        assertThatNullPointerException().isThrownBy(
                () -> RecordSpecifications.withFilters(null, new btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy(actor -> java.util.Set.of()).scopeFor(visibility(RoleName.CALISAN, USER_ID))));
    }

    /** Olusturan filtresi korele bir EXISTS alt sorgusu kurar. */
    @SuppressWarnings("unchecked")
    private void givenSubquery() {
        Subquery<UUID> subquery = mock(Subquery.class, RETURNS_DEEP_STUBS);
        when(query.subquery(UUID.class)).thenReturn(subquery);
    }

    @SuppressWarnings("unchecked")
    private Path<Object> pathFor(String attribute) {
        Path<Object> path = mock(Path.class);
        when(root.get(attribute)).thenReturn(path);
        return path;
    }

    private void build(RoleName role) {
        Specification<Record> specification =
                RecordSpecifications.withFilters(new RecordSearchCriteria(), new btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy(actor -> java.util.Set.of()).scopeFor(visibility(role, USER_ID)));
        specification.toPredicate(root, query, cb);
    }
}
