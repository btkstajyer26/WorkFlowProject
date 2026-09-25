-- =====================================================================
-- EBYS - expected_target_role_id yalniz ROLE stratejisinin arama anahtari
--
-- Kaynak: docs/decisions/0008-hedef-rol-semantigi-ve-onceki-aktore-donus.md
-- (karar K2 / K6, "Sema karsiligi" bolumu).
--
-- Sorun (B02): kolon uc anlami birden tasiyordu - ROLE icin arama anahtari,
-- cozum sonrasi dogrulama, ve "bu gecis hedef ister" nobetcisi. Ucuncu anlam
-- yuzunden CREATOR / CURRENT_ASSIGNEE / PREVIOUS_ACTOR satirlarinda da dolu
-- tutuluyordu; sonucta kimligi calisma zamaninda belirlenen bir hedefe statik
-- ve yerlesik bir rol dayatiliyor, dinamik rol bu dayatmayi hicbir zaman
-- gecemiyordu.
--
-- V15 duzenlenmiyor (Flyway kurali) - bu SS13.1'e uygun ileri migration.
--
-- DIKKAT: Bu migration tek basina uygulanamaz. Java tarafindaki
-- TransitionRule invariant'i ayni teslimde ters cevrilmelidir; aksi halde
-- kural okunurken hata verir ve uygulama acilista dusmez/duser.
-- =====================================================================

-- Adim 1: Kimligi calisma zamaninda belirlenen stratejilerde kolonu bosalt.
-- Etkilenen uc satir: iki CREATOR (CALISANA_GERI_GONDER x 2) ve bir
-- PREVIOUS_ACTOR (BASKAN_YARDIMCISINA_GERI_GONDER). Gecis sayisi degismez.

UPDATE workflow_transitions
   SET expected_target_role_id = NULL
 WHERE target_strategy IN ('CREATOR', 'CURRENT_ASSIGNEE', 'PREVIOUS_ACTOR');

-- Adim 2: Kisiti daralt. Artik kolon YALNIZ ROLE stratejisinde dolu olabilir;
-- diger butun stratejilerde NULL olmak zorundadir. Boylece kolonun ne oldugu
-- semadan okunabilir hale gelir ve ayni hata yeni bir stratejide tekrarlanamaz.

ALTER TABLE workflow_transitions
    DROP CONSTRAINT chk_transition_target_strategy_role;

ALTER TABLE workflow_transitions
    ADD CONSTRAINT chk_transition_target_strategy_role CHECK (
        (target_strategy =  'ROLE' AND expected_target_role_id IS NOT NULL) OR
        (target_strategy <> 'ROLE' AND expected_target_role_id IS NULL)
    );
