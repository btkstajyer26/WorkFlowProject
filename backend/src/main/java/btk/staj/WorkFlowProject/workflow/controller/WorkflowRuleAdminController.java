package btk.staj.WorkFlowProject.workflow.controller;

import btk.staj.WorkFlowProject.workflow.adapter.ReloadableTransitionRuleSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

/**
 * Gecis kurallarinin bellek icindeki kopyasini tazeleyen yonetim ucu (WF-4).
 *
 * <p>Kurallar acilista bir kez okunur; {@code workflow_transitions} tablosu disaridan
 * degistiginde (migration, TEST ortaminda elle duzeltme, ilerideki yonetim paneli)
 * calisan uygulama bunu kendiliginden gormez. Bu uc, yeniden baslatmaya gerek birakmaz.
 *
 * <p><strong>Bu uc grafigi degistirmez, yalnizca yeniden okur.</strong> DB-1 SS13'un
 * yasakladigi sey versiyonlama olmadan aktif grafigi <em>duzenleyen</em> bir yonetim
 * arayuzudur; tazeleme o yasagin kapsaminda degildir.
 *
 * <p>Yeni yapilandirma gecersizse istek hata doner ve <strong>mevcut kurallar korunur</strong>;
 * uygulama kural kaynagi olmadan kalmaz (bkz. {@link ReloadableTransitionRuleSource}).
 *
 * <p>Uc {@code admin/} paketinde degil workflow modulunde: yonettigi sey workflow
 * cekirdeginin kendi bellek durumu.
 */
@RestController
@RequestMapping("/api/workflow/rules")
@Tag(name = "Workflow Rules", description = "Gecis kurallarinin yonetimi")
public class WorkflowRuleAdminController {

    private final ReloadableTransitionRuleSource ruleSource;

    public WorkflowRuleAdminController(ReloadableTransitionRuleSource ruleSource) {
        this.ruleSource = Objects.requireNonNull(ruleSource, "ruleSource");
    }

    @PostMapping("/reload")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    @Operation(summary = "Gecis kurallarini veritabanindan yeniden okur (grafigi degistirmez)")
    public Map<String, Integer> reload() {
        return Map.of("ruleCount", ruleSource.reload());
    }
}
