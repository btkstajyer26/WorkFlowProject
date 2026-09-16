-- =====================================================================
-- EBYS - audit_logs atama kolonlari (B12)
--
-- Kaynak: ADR-0009 (workflow audit'inde atama sozlesmesi);
-- APP9_APP10_B11_ISTEMCI_SOZLESMESI.md SS3 ve SS6 "Veri (B12)".
--
-- Amac: Bir gecisin atamayi NEREDEN NEREYE tasidigi kalici gecmiste
-- okunabilsin. Bugun WorkflowTransitionAudit yalniz kisi atamasini
-- tasiyor, AuditLogService onu bile okumuyor; "bu evrak hangi departmana
-- gonderildi" sorusunun cevabi yalniz records tablosunun O ANKI halinde
-- var, gecmiste hic yok. Departman gonderiminde comment istege bagli
-- oldugu icin serbest metne de guvenilemez.
--
-- SEKIL (ADR-0009 K1): dort kolon. Atama TURU (USER/DEPARTMENT/NONE)
-- icin ayri bir kolon ACILMAZ; tur okuma aninda
-- common/dto/AssignmentView.of(UUID, Integer) ile turetilir. Turetimin
-- tek dogruluk kaynagi orasidir ve records tablosu da ayni sekli
-- kullanir (V21). Turu semada da saklamak ikinci bir kaynak yaratirdi.
--
-- Karsilikli dislama her iki yan icin AYRI AYRI zorlanir; kural V21'deki
-- chk_records_assignment_exclusive ile birebir ayni. "Ikisi de NULL"
-- SERBESTTIR ve NONE demektir: TASLAK ve terminal durumlar boyledir.
--
-- KAPSAM (ADR-0009 K2): bu dort kolon yalniz GECIS satirlarinda
-- anlamlidir. Kayit yasam dongusu olaylari (recordLifecycleEvent) ve
-- ADMIN aktorunun HTTP erisim loglari (recordAccess) atama degistirmez,
-- dordu de NULL kalir. Gecis satiri ayrimi previous_status IS NOT NULL
-- ile yapilir.
--
-- GERIYE DONUK (ADR-0009 K3): mevcut butun satirlar dort kolonda da NULL
-- kalir ve "atama bilgisi kaydedilmemis" olarak okunur. Her iki CHECK de
-- NULL tarafindan saglandigi icin veri onarim adimi YOKTUR. Gecmis
-- satirlara kaydin bugunku atamasindan turetilmis deger YAZILMAZ -
-- append-only bir izde kaydedilmemis olguyu kaydedilmis gibi gostermek
-- kabul edilemez.
--
-- SIRA NOTU: Bu migration tek basina uygulanabilir ve zararsizdir -
-- kolonlar nullable, mevcut AuditLog entity'si onlari tanimadigi icin
-- INSERT'te listelemez, PostgreSQL NULL yazar, iki CHECK de saglanir.
-- TERSI GECERLI DEGILDIR: entity kolonlari tanirken migration kosmamissa
-- her audit yazimi "column does not exist" ile duser. Bu yuzden
-- migration Java tarafindan ONCE veya onunla BIRLIKTE gider.
-- (V24'un "tek basina uygulanamaz" uyarisi buraya UYMAZ; oradaki
-- bagimlilik iki yonluydu, bu tek yonlu ve daha zayiftir.)
-- =====================================================================

ALTER TABLE audit_logs
    ADD COLUMN previous_assigned_to            UUID,
    ADD COLUMN previous_assigned_department_id INT,
    ADD COLUMN new_assigned_to                 UUID,
    ADD COLUMN new_assigned_department_id      INT;

-- FK'ler: audit_logs'un mevcut uc kolonunda da (record_id, user_id,
-- role_id) FK var; atama kolonlarini FK'siz birakmak ayni tablo icinde
-- tutarsizlik olurdu. ON DELETE RESTRICT, fk_audit_user / fk_audit_role
-- ve V22'nin butun departman FK'leri ile ayni kuraldir: append-only bir
-- iz, isaret ettigi satirin silinmesini engellemelidir. CASCADE (yalniz
-- fk_audit_record'da var) burada iz kaybina yol acardi.
ALTER TABLE audit_logs
    ADD CONSTRAINT fk_audit_previous_assigned_user
        FOREIGN KEY (previous_assigned_to)
        REFERENCES users (id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_audit_previous_assigned_department
        FOREIGN KEY (previous_assigned_department_id)
        REFERENCES departments (id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_audit_new_assigned_user
        FOREIGN KEY (new_assigned_to)
        REFERENCES users (id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT fk_audit_new_assigned_department
        FOREIGN KEY (new_assigned_department_id)
        REFERENCES departments (id)
        ON DELETE RESTRICT;

ALTER TABLE audit_logs
    ADD CONSTRAINT chk_audit_previous_assignment_exclusive
        CHECK (previous_assigned_to IS NULL OR previous_assigned_department_id IS NULL),
    ADD CONSTRAINT chk_audit_new_assignment_exclusive
        CHECK (new_assigned_to IS NULL OR new_assigned_department_id IS NULL);

-- INDEX ACILMIYOR. Bu dort kolon hicbir okuma yolunun WHERE'inde
-- gecmez: audit_logs sorgulari record_id (idx_audit_record_id), user_id
-- (idx_audit_user_id) ve created_at (idx_audit_created_at) uzerinden
-- gider; atama alanlari yalniz zaten secilmis satirlarin
-- projeksiyonudur. Append-only ve surekli buyuyen bir tabloda
-- kullanilmayan her index salt yazma maliyetidir. Ileride "bir
-- departmanin gecmis is yuku" gibi bir uc acilirsa index o zaman,
-- olcumle birlikte eklenir.
