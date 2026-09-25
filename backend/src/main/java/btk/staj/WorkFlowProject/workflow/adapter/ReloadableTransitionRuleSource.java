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
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
 * <p>Delegate {@code volatile}: takas atomiktir. Bir islem birden fazla kural
 * okuyacaksa once {@link #snapshot()} ile tek goruntu yakalamalidir.
 * WF-8 yazmalari ve manuel reload ayni monitor ile siralanir; okuyucular beklemez.
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
    public synchronized int reload() {
        TransitionRuleSource refreshed = new DbTransitionRuleSource(reader);
        this.delegate = refreshed;
        return refreshed.all().size();
    }

    /** Su an kullanilan snapshot; tanilama ve test icin. */
    public TransitionRuleSource current() {
        return delegate;
    }

    @Override
    public TransitionRuleSource snapshot() {
        return delegate;
    }

    /**
     * Run a management change and prepare its complete snapshot in one owned
     * transaction. Publish only after execute has committed successfully, while
     * still holding the same monitor used by reload. No database reads occur
     * after commit. The caller supplies a synchronous TransactionTemplate.
     */
    public synchronized <T> T updateAndReload(TransactionOperations transaction, Supplier<T> change) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalTransactionStateException("Workflow binding changes must own their transaction");
        }
        AtomicBoolean didCommit = new AtomicBoolean();
        PreparedUpdate<T> committed = Objects.requireNonNull(transaction.execute(status -> {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { didCommit.set(true); }
            });
            T result = change.get();
            return new PreparedUpdate<>(result, new DbTransitionRuleSource(reader));
        }), "committed update");
        // TransactionTemplate may return normally after a local rollback-only vote.
        if (!didCommit.get()) throw new UnexpectedRollbackException("Workflow binding change was rolled back");
        delegate = committed.snapshot();
        return committed.result();
    }

    private record PreparedUpdate<T>(T result, TransitionRuleSource snapshot) {}

    @Override
    public Optional<TransitionRule> find(RecordStatus from, WorkflowAction action, RoleId actorRoleId) {
        return delegate.find(from, action, actorRoleId);
    }

    @Override
    public List<TransitionRule> all() {
        return delegate.all();
    }
}
