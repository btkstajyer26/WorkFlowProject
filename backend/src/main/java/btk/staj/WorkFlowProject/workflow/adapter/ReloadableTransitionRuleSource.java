package btk.staj.WorkFlowProject.workflow.adapter;

import btk.staj.WorkFlowProject.workflow.port.TransitionRuleRecordReader;
import btk.staj.WorkFlowProject.workflow.statemachine.RecordStatus;
import btk.staj.WorkFlowProject.workflow.statemachine.RoleId;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRule;
import btk.staj.WorkFlowProject.workflow.statemachine.TransitionRuleSource;
import btk.staj.WorkFlowProject.workflow.statemachine.WorkflowAction;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Gecis kurallarini yeniden baslatma gerektirmeden tazeleyebilen kaynak (WF-4).
 *
 * <p>{@link DbTransitionRuleSource} kurallari constructor'inda okuyup dondurur; bu
 * dogrulamayi tek yerde toplayan bilincli bir tasarim ve <strong>degistirilmiyor</strong>.
 * Bu sinif onun uzerine yalnizca <em>hangi</em> snapshot'in kullanildigi sorusunu ekler.
 *
 * <h2>Neden takas, neden yerinde guncelleme degil</h2>
 * {@link #reload()} once yeni bir {@code DbTransitionRuleSource} kurmayi dener. Kurulum
 * basarisiz olursa &mdash; bozuk bir satir, bos tablo, tutarsiz hedef metadata'si &mdash;
 * istisna cagirana doner ve <strong>eski snapshot yerinde kalir</strong>. Boylece hatali
 * bir yeniden yukleme calisan uygulamayi kural kaynagi olmadan birakmaz; en kotu ihtimalle
 * kurallar bir sure eski kalir, ki bu yeniden baslatmadan onceki durumdan farksizdir.
 *
 * <p>Delegate {@code volatile}: takas atomiktir ve o sirada devam eden bir istek ya
 * tamamen eski ya tamamen yeni snapshot'i gorur, ikisinin karisimini degil.
 *
 * <h2>Acilistaki davranis korunur</h2>
 * Ilk snapshot constructor'da kurulur. Seed eksikse uygulama yine <strong>acilmaz</strong>;
 * fail-fast tercihi bu sinifla zayiflamaz.
 */
public final class ReloadableTransitionRuleSource implements TransitionRuleSource {

    private final TransitionRuleRecordReader reader;
    private volatile TransitionRuleSource delegate;

    public ReloadableTransitionRuleSource(TransitionRuleRecordReader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
        this.delegate = new DbTransitionRuleSource(reader);
    }

    /**
     * Kurallari veritabanindan yeniden okur.
     *
     * <p>Yalnizca <em>okur</em>: gecis grafigini degistirmez. DB-1 SS13'un yasakladigi sey
     * versiyonlama olmadan aktif grafigi duzenlemektir; tazeleme o yasagin kapsaminda
     * degildir.
     *
     * @return yeni snapshot'taki aktif kural sayisi
     * @throws btk.staj.WorkFlowProject.workflow.exception.TransitionRuleConfigurationException
     *         yeni yapilandirma gecersizse. Bu durumda mevcut snapshot korunur.
     */
    public int reload() {
        TransitionRuleSource refreshed = new DbTransitionRuleSource(reader);
        this.delegate = refreshed;
        return refreshed.all().size();
    }

    /** Su an kullanilan snapshot; tanilama ve test icin. */
    public TransitionRuleSource current() {
        return delegate;
    }

    @Override
    public Optional<TransitionRule> find(RecordStatus from, WorkflowAction action, RoleId actorRoleId) {
        return delegate.find(from, action, actorRoleId);
    }

    @Override
    public List<TransitionRule> all() {
        return delegate.all();
    }
}
