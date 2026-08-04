package controle.api.back_end;

import controle.api.back_end.config.DesktopApp;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.ServerSocket;

@SpringBootApplication
@EntityScan(basePackages = "controle.api.back_end.model")
@EnableScheduling
public class BackEndApplication {

	/**
	 * Lock de instância única: abre um ServerSocket na porta 13308 (apenas loopback).
	 * Se outra instância já estiver rodando, essa porta já está ocupada e o app encerra.
	 */
	private static ServerSocket instanceLock;

	public static void main(String[] args) {
		if (!acquireSingleInstanceLock()) {
			showAlreadyRunningMessage();
			return;
		}
		new Thread(() -> SpringApplication.run(BackEndApplication.class)).start();
		Application.launch(DesktopApp.class, args);
	}

	private static boolean acquireSingleInstanceLock() {
		try {
			instanceLock = new ServerSocket(13308, 1, InetAddress.getByName("127.0.0.1"));
			instanceLock.setReuseAddress(false);
			// Mantém o socket aberto durante toda a execução
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try { if (instanceLock != null) instanceLock.close(); } catch (Exception ignored) {}
			}));
			return true;
		} catch (Exception e) {
			return false; // Porta já ocupada = outra instância rodando
		}
	}

	private static void showAlreadyRunningMessage() {
		try {
			javax.swing.JOptionPane.showMessageDialog(
				null,
				"O MyFinance já está em execução.\nVerifique a barra de tarefas.",
				"MyFinance",
				javax.swing.JOptionPane.INFORMATION_MESSAGE
			);
		} catch (Exception e) {
			System.out.println("[MyFinance] Já existe uma instância em execução.");
		}
	}
}
