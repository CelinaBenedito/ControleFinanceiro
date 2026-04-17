package controle.api.back_end.config;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.scene.image.Image;

public class DesktopApp extends Application {
    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        webView.getEngine().load(
                getClass().getResource("/static/index.html").toExternalForm()
        );

        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/static/assets/glaceonIcon .png")));
        } catch (Exception e) {
            System.out.println("Não foi possível carregar o ícone: " + e.getMessage());
        }

        Scene scene = new Scene(webView, 1200, 800);
        stage.setTitle("Controle Financeiro");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
