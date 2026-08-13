-- Ayri "kayit calisma notu" modelinden vazgecildi.
-- Kullanici aciklamasi artik dogrudan workflow aksiyonunun `comment` alaninda
-- gonderiliyor ve audit kaydinda saklaniyor; ayri bir `record_notes` tablosuna
-- gerek yok. Karar icin bkz. docs/EKSIK_CONTROLLERLAR_VE_KARARLAR.md (§2).
--
-- V2__create_record_notes.sql tarihsel kayit olarak birakilir; bu migration
-- olusturdugu tabloyu ve indekslerini geri alir.
DROP TABLE IF EXISTS record_notes;
