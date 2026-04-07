package controle.api.back_end;

import controle.api.back_end.config.DesktopApp;
import javafx.application.Application;
import org.h2.tools.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import java.sql.SQLException;

@SpringBootApplication
public class BackEndApplication {

	public static void main(String[] args) {
		new Thread(() -> SpringApplication.run(BackEndApplication.class)).start();
		Application.launch(DesktopApp.class, args);
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	public Server h2Server() throws SQLException {
		return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
	}

}

