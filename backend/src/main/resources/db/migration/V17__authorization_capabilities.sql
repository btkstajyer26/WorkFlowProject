-- WF-2B: controller capability gaps. Existing migrations remain immutable.
INSERT INTO permissions (code, display_name, description) VALUES
    ('FILE_MANAGE', 'Dosya Yönetimi', 'Düzenlenebilir kendi kaydına dosya ekler veya siler'),
    ('RECORD_DELETE', 'Kayıt Silme', 'Kendi taslak kaydını siler'),
    ('AUDIT_VIEW', 'Denetim Kaydı Görüntüleme', 'Kullanıcı ve sistem denetim kayıtlarını görüntüler');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('CALISAN', 'FILE_MANAGE'),
    ('CALISAN', 'RECORD_DELETE'),
    ('ADMIN', 'AUDIT_VIEW')
) AS seed(system_key, code)
JOIN roles r ON r.system_key = seed.system_key
JOIN permissions p ON p.code = seed.code;
