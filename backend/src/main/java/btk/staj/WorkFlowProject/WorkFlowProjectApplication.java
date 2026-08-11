package btk.staj.WorkFlowProject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // <-- Bu import'u ekle

@SpringBootApplication
@EnableAsync // <-- Bu anotasyonu ekle (MailService'teki @Async'in çalışması için şart)
public class WorkFlowProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkFlowProjectApplication.class, args);
    }
}