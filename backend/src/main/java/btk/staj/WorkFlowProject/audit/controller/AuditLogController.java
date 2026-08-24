package btk.staj.WorkFlowProject.audit.controller;

import btk.staj.WorkFlowProject.audit.dto.AuditLogResponse;
import btk.staj.WorkFlowProject.audit.service.AuditLogService;
import btk.staj.WorkFlowProject.common.exception.ResourceNotFoundException;
import btk.staj.WorkFlowProject.rbac.service.RecordAccessPolicy;
import btk.staj.WorkFlowProject.record.entity.Record;
import btk.staj.WorkFlowProject.record.repository.RecordRepository;
import btk.staj.WorkFlowProject.workflow.model.CurrentActor;
import btk.staj.WorkFlowProject.workflow.port.CurrentActorProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final RecordRepository recordRepository;
    private final RecordAccessPolicy recordAccessPolicy;
    private final CurrentActorProvider currentActorProvider;

    public AuditLogController(AuditLogService auditLogService,
                              RecordRepository recordRepository,
                              RecordAccessPolicy recordAccessPolicy,
                              CurrentActorProvider currentActorProvider) {
        this.auditLogService = Objects.requireNonNull(auditLogService, "auditLogService");
        this.recordRepository = Objects.requireNonNull(recordRepository, "recordRepository");
        this.recordAccessPolicy = Objects.requireNonNull(recordAccessPolicy, "recordAccessPolicy");
        this.currentActorProvider = Objects.requireNonNull(
                currentActorProvider, "currentActorProvider");
    }


    @GetMapping("/record/{recordId}")
    public List<AuditLogResponse> getGecmis(@PathVariable UUID recordId) {
        CurrentActor actor = currentActorProvider.currentActor();

        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Kayıt bulunamadı: " + recordId));

        recordAccessPolicy.assertCanView(
                actor.role(),
                actor.id(),
                record.getCreatedBy(),
                record.getAssignedTo(),
                record.getLastDeputyId(),
                record.getStatus());

        // Kaydi gorebilmek butun gecmisi gormek demek degil. Iki yonlu kirpma
        // var, ikisi de ayni fikrin farkli ucu: kullanici evraki yalnizca
        // kendi masasinda oldugu donem boyunca gorur. Karar
        // RecordAccessPolicy'nin, kirpma AuditLogService'in isi.

        // Evraki elinden cikarmis olan kullanici, kaydin baskasindayken aldigi
        // islemleri gormez.
        if (recordAccessPolicy.seesRecordAsOfHandoff(
                actor.role(),
                actor.id(),
                record.getAssignedTo(),
                record.getStatus())) {
            return auditLogService.getGecmisDevreKadar(recordId);
        }

        // Baskan da evrak kendisine ulasmadan onceki Calisan-Bsk. Yrd.
        // trafigini gormez; gecmis ilk iletimden itibaren baslar.
        if (recordAccessPolicy.seesHistoryFromPresidentHandover(actor.role())) {
            return auditLogService.getGecmisIletimdenItibaren(recordId);
        }

        return auditLogService.getGecmis(recordId);
    }
}
