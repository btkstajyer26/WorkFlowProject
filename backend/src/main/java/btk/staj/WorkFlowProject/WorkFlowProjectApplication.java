package btk.staj.WorkFlowProject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
// Bildirim e-postalari @Async ile gonderilir (sartname §6.2); onsuz gonderim
// istegi bloklar ve onay akisi yavaslar.
@EnableScheduling
@SpringBootApplication
@EnableAsync
public class WorkFlowProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkFlowProjectApplication.class, args);
	}

}
