package controle.api.back_end.config;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class DesktopApp extends Application {
    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.load(
                getClass().getResource("/static/index.html").toExternalForm()
        );

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("desktopBridge", new DesktopBridge(stage));
                System.out.println("[DesktopApp] desktopBridge injetada com sucesso.");
            }
        });

        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/static/assets/glaceonIcon .png")));
        } catch (Exception e) {
            System.out.println("Não foi possível carregar o ícone: " + e.getMessage());
        }

        Scene scene = new Scene(webView, 1200, 800);
        stage.setTitle("MyFinance");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static class DesktopBridge {
        private final Stage stage;

        public DesktopBridge(Stage stage) {
            this.stage = stage;
        }

        public boolean saveBase64File(String fileName, String base64Content) {
            if (base64Content == null || base64Content.isBlank()) {
                System.out.println("[DesktopApp] Exportação abortada: conteúdo vazio.");
                return false;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Salvar PDF exportado");
            fileChooser.setInitialFileName(
                    fileName == null || fileName.isBlank() ? "registros.pdf" : fileName
            );
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Arquivos PDF", "*.pdf")
            );

            File targetFile = fileChooser.showSaveDialog(stage);
            if (targetFile == null) {
                System.out.println("[DesktopApp] Exportação cancelada pelo usuário (sem arquivo selecionado).");
                return false;
            }

            try {
                byte[] bytes = Base64.getDecoder().decode(base64Content);
                Files.write(targetFile.toPath(), bytes);
                System.out.println("[DesktopApp] PDF salvo em: " + targetFile.getAbsolutePath());
                return true;
            } catch (IllegalArgumentException | IOException e) {
                throw new RuntimeException("Falha ao salvar arquivo exportado.", e);
            }
        }
    }
}
