package btk.staj.WorkFlowProject.workflow.statemachine;

import java.util.List;
import java.util.Optional;

/**
 * Gecis kurallarinin okundugu kaynak. Durum makinesi kurallari nereden
 * geldigini bilmez; yalnizca bu port uzerinden sorar.
 *
 * <p>Bu arayuz bilerek saf Java'dir: Spring, JPA veya repository bagimliligi
 * yoktur. Production'da {@code workflow_transitions} tablosunu okuyan
 * {@code DbTransitionRuleSource} tarafindan uygulanir. Test agacindaki
 * {@code StaticTransitionRuleSource} yalnizca parity testinin ve veritabanisiz
 * testlerin referansi olarak durur; production artifact'inda bulunmaz (TZ-1).
 *
 * <p>Port yalnizca <em>okuma</em> sozlesmesidir. Cache, reload, kayit veya
 * silme gibi yonetim metotlari bu portun sorumlulugu degildir; onlar
 * implementasyonun kendi ic detayidir.
 */
public interface TransitionRuleSource {

    /**
     * Verilen birlesime karsilik gelen kurali arar.
     *
     * <p>{@code (from, action, actorRoleId)} birlesimi domain seviyesinde tekildir:
     * bir birlesim en fazla bir gecise karsilik gelir.
     *
     * @return kural varsa dolu, tanimli degilse bos {@code Optional}
     */
    Optional<TransitionRule> find(RecordStatus from, WorkflowAction action, RoleId actorRoleId);

    /**
     * Tanimli butun gecis kurallari.
     *
     * @return cagiran tarafindan degistirilemeyecek immutable bir anlik goruntu
     */
    List<TransitionRule> all();
}
