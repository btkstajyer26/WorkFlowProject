package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.workflow.dto.BindActorRequest;
import btk.staj.WorkFlowProject.workflow.dto.WorkflowActorBindingView;
import btk.staj.WorkFlowProject.workflow.service.WorkflowActorBindingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AP-8: WF-8'in hazır {@link WorkflowActorBindingService} servisine HTTP
 * katmanı. Grafik topolojisini değiştirmez - yalnız sabit bir şablon geçişe
 * dinamik bir rolü aktör olarak bağlar veya bu bağı pasifleştirir.
 *
 * <p>Uç {@code admin/} paketinde değil workflow modülünde:
 * {@link WorkflowRuleAdminController} ile aynı gerekçe - yönettiği şey
 * workflow çekirdeğinin kendi bellek/kural durumu (bkz.
 * {@code WF8_AP8_AKTOR_ROL_BAGLAMA_SOZLESMESI.md}).
 *
 * <p>{@code @PreAuthorize} burada ikinci bir katmandır: servis yetkiyi
 * kendisi de denetler (yalnız frontend kontrolü yeterli sayılmaz), ama HTTP
 * katmanının diğer uçlarla tutarlı, hızlı bir 403 dönmesi için kontrol
 * burada da tutulur.
 */
@RestController
@RequestMapping("/api/workflow/actor-bindings")
@Tag(name = "Workflow Actor Bindings", description = "Sabit geçişlere dinamik rol bağlama (AP-8)")
public class WorkflowActorBindingController {

    private final WorkflowActorBindingService bindings;

    public WorkflowActorBindingController(WorkflowActorBindingService bindings) {
        this.bindings = bindings;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    @Operation(summary = "Aktif ve pasif tüm aktör-rol bağlarını listeler")
    public List<WorkflowActorBindingView> list() {
        return bindings.listTransitions();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    @Operation(summary = "Bir şablon geçişe dinamik bir rolü aktör olarak bağlar")
    public WorkflowActorBindingView bind(@Valid @RequestBody BindActorRequest request) {
        return bindings.bind(request.getTemplateTransitionId(), request.getActorRoleId());
    }

    /**
     * Fiziksel silme değildir: bağ pasifleştirilir. Kullanımda olan bağ
     * ({@code BINDING_IN_USE}, 409) veya sistem rolüne ait bağ
     * ({@code PROTECTED_BINDING}, 409) reddedilir.
     */
    @DeleteMapping("/{bindingId}")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    @Operation(summary = "Bir aktör-rol bağını pasifleştirir")
    public WorkflowActorBindingView unbind(@PathVariable Integer bindingId) {
        return bindings.unbind(bindingId);
    }
}
