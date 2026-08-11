package btk.staj.WorkFlowProject;

import btk.staj.WorkFlowProject.service.MailService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class TestRunner implements CommandLineRunner {

    private final MailService mailService;

    public TestRunner(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public void run(String... args) throws Exception {
        Thread.sleep(5000); // Sistem tamamen ayağa kalksın diye 5 saniye bekletiyoruz
        
        System.out.println("\n==================================================");
        System.out.println("🚀 EBYS MODÜLÜ: CANLI TEST MAİLİ TETİKLENİYOR...");
        System.out.println("==================================================\n");

        // ⚠️ BURAYI DEĞİŞTİR: Test mailinin geleceği kendi kişisel e-posta adresini yaz!
        String aliciEmail = "btkebys@outlook.com"; 
        
        mailService.sendStatusChangeMail(
            aliciEmail,
            "Melih",
            UUID.randomUUID(),
            "2026 Yılı Ağustos Ayı Personel Bütçe Raporu",
            "Başkan İncelemesinde",
            "Melih Kocaman tarafından yazılan asenkron e-posta motoru başarıyla test edildi. Şartname kriterlerine uygundur."
        );

        System.out.println("\n==================================================");
        System.out.println("📬 TEST SİNYALİ GÖNDERİLDİ! ARKA PLANDA ÇALIŞIYOR.");
        System.out.println("==================================================\n");
    }
}