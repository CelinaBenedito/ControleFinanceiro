package controle.api.back_end.config;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class DesktopApp extends Application {
    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        // Aponta para o seu backend Spring
        webView.getEngine().load(
                getClass().getResource("/static/index.html").toExternalForm()
        );

        Scene scene = new Scene(webView, 1200, 800);
        stage.setTitle("Minha API Financeira");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
