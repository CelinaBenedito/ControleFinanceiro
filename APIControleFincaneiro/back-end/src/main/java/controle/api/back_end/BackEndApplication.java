package controle.api.back_end;

import controle.api.back_end.config.DesktopApp;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "controle.api.back_end.model")
public class BackEndApplication {

	public static void main(String[] args) {
		new Thread(() -> SpringApplication.run(BackEndApplication.class)).start();
		Application.launch(DesktopApp.class, args);
	}

}
