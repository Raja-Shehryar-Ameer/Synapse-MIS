package com.synapse;

import com.synapse.ui.SessionManager;
import com.synapse.ui.SplashScreen;
import com.synapse.ui.ViewFactory;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * JavaFX entry point with sidebar navigation and splash screen.
 */
public class App extends Application {

    private BorderPane rootLayout;
    private ViewFactory viewFactory;
    private VBox sidebar;
    private Button activeButton;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        SplashScreen splash = new SplashScreen(stage);
        Thread initThread = new Thread(() -> {
            HibernateUtil.getEntityManagerFactory();
            Platform.runLater(() -> splash.dismiss(this::showMainApp));
        });
        initThread.setDaemon(true);
        initThread.start();
    }

    private void showMainApp() {
        viewFactory = new ViewFactory();
        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("root-pane");

        sidebar = createSidebar();
        rootLayout.setLeft(sidebar);
        sidebar.setVisible(false);
        sidebar.setManaged(false);

        com.synapse.ui.Navigation.setNavigationHandler(() -> {
            String target = com.synapse.ui.Navigation.getTargetViewId();
            if (target != null) {
                showView(target);
                updateSidebarSelection(target);
            }
        });

        showView("account");

        SessionManager.loggedInProperty().addListener((obs, wasIn, isIn) -> {
            sidebar.setVisible(isIn);
            sidebar.setManaged(isIn);
            if (isIn) {
                showView("dashboard");
                updateSidebarSelection("dashboard");
            } else {
                activeButton = null;
                showView("account");
            }
        });

        Scene scene = new Scene(rootLayout, 1000, 650);
        scene.getStylesheets().add(getClass().getResource("/styles/synapse.css").toExternalForm());
        com.synapse.ui.theme.ThemeManager.getInstance().setScene(scene);

        rootLayout.setOpacity(0);
        primaryStage.setTitle("Synapse - Healthcare Management");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.setMaximized(true);
        primaryStage.show();

        com.synapse.ui.ToastNotification.setOwner(primaryStage);

        primaryStage.setOnCloseRequest(e -> {
            com.synapse.service.NotificationScheduler.stopBackgroundTasks();
            System.exit(0);
        });

        FadeTransition fade = new FadeTransition(Duration.millis(600), rootLayout);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private VBox createSidebar() {
        VBox sb = new VBox();
        sb.getStyleClass().add("sidebar");
        sb.setPrefWidth(230);
        sb.setSpacing(0);

        Label logo = new Label("S");
        logo.setStyle(
                "-fx-background-color: #22c55e; -fx-text-fill: #fff;" +
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                "-fx-pref-width: 34; -fx-pref-height: 34;" +
                "-fx-background-radius: 8; -fx-alignment: center;"
        );
        Label brand = new Label("Synapse");
        brand.getStyleClass().add("sidebar-brand");

        HBox brandRow = new HBox(10, logo, brand);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        Label tagline = new Label("Healthcare Ecosystem");
        tagline.getStyleClass().add("sidebar-tagline");

        VBox header = new VBox(6, brandRow, tagline);
        header.setPadding(new Insets(24, 18, 20, 18));

        sb.getChildren().addAll(header, new Separator());

        String[][] navItems = {
                {"dashboard", "\uD83C\uDFE0", "Dashboard"},
                {"account", "\uD83D\uDC64", "My Account"},
                {"vitals", "\u2764\uFE0F", "Log Vitals"},
                {"symptoms", "\uD83D\uDD0D", "Log Symptoms"},
                {"diet", "\uD83E\uDD57", "Diet & Hydration"},
                {"journal", "\uD83D\uDCD3", "Journal"},
                {"medicine", "\uD83D\uDC8A", "Medicine"},
                {"calendar", "\uD83D\uDCC5", "Calendar"},
                {"hospitals", "\uD83C\uDFE5", "Hospitals"},
                {"emergency", "\uD83D\uDEA8", "Emergency"},
                {"records", "\uD83D\uDCC1", "Medical Records"},
                {"analytics", "\uD83D\uDCCA", "Analytics"},
                {"reports", "\uD83D\uDCCB", "Health Reports"},
                {"backup", "\u2601\uFE0F", "Backup & Restore"}
        };

        VBox navBox = new VBox(2);
        navBox.setPadding(new Insets(10));
        for (String[] item : navItems) {
            navBox.getChildren().add(createNavButton(item[0], item[1], item[2]));
        }

        ScrollPane navScroll = new ScrollPane(navBox);
        navScroll.setFitToWidth(true);
        navScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        navScroll.getStyleClass().add("sidebar-scroll");
        VBox.setVgrow(navScroll, Priority.ALWAYS);
        sb.getChildren().add(navScroll);

        Button themeBtn = new Button("\uD83C\uDF13  Toggle Theme");
        themeBtn.getStyleClass().add("nav-button");
        themeBtn.setMaxWidth(Double.MAX_VALUE);
        themeBtn.setAlignment(Pos.CENTER_LEFT);
        themeBtn.setOnAction(e -> {
            com.synapse.ui.theme.ThemeManager.getInstance().toggleTheme();
            themeBtn.setText(com.synapse.ui.theme.ThemeManager.getInstance().isDarkMode() 
                ? "\u2600\uFE0F  Light Mode" 
                : "\uD83C\uDF13  Dark Mode");
        });

        Button exitBtn = new Button("\u23FB  Exit App");
        exitBtn.getStyleClass().add("nav-button");
        exitBtn.setMaxWidth(Double.MAX_VALUE);
        exitBtn.setAlignment(Pos.CENTER_LEFT);
        exitBtn.setOnAction(e -> Platform.exit());
        VBox bottom = new VBox(5, themeBtn, exitBtn);
        bottom.setPadding(new Insets(10));
        sb.getChildren().addAll(new Separator(), bottom);

        return sb;
    }

    private Button createNavButton(String viewId, String icon, String label) {
        Button btn = new Button(icon + "  " + label);
        btn.setUserData(viewId);
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setOnAction(e -> {
            showView(viewId);
            setActiveButton(btn);
        });
        if ("dashboard".equals(viewId)) {
            btn.getStyleClass().add("nav-button-active");
            activeButton = btn;
        }
        return btn;
    }

    private void setActiveButton(Button btn) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }
        btn.getStyleClass().add("nav-button-active");
        activeButton = btn;
    }

    private void updateSidebarSelection(String viewId) {
        ScrollPane scroll = (ScrollPane) sidebar.getChildren().get(2);
        VBox navBox = (VBox) scroll.getContent();
        for (javafx.scene.Node node : navBox.getChildren()) {
            if (node instanceof Button btn && viewId.equals(btn.getUserData())) {
                setActiveButton(btn);
                break;
            }
        }
    }

    private void showView(String viewId) {
        Region view = viewFactory.getView(viewId);
        
        if (view instanceof com.synapse.ui.UpdatableView) {
            ((com.synapse.ui.UpdatableView) view).updateView();
        }
        
        view.setOpacity(0);
        view.setTranslateX(18);
        rootLayout.setCenter(view);

        FadeTransition ft = new FadeTransition(Duration.millis(220), view);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(220), view);
        tt.setFromX(18);
        tt.setToX(0);

        new ParallelTransition(ft, tt).play();
    }

    @Override
    public void stop() {
        HibernateUtil.shutdown();
    }

    public static void main(String[] args) {
        com.ironsoftware.ironpdf.License.setLicenseKey(
                "IRONSUITE.RSACODERZ.GMAIL.COM.30821-87643028E9-KRVZY-BQAGUW3HDCJP-IU5DYVVSNHLG-R7QSROCQO47B-IX3K4C7KNPMB-6P57X37I62TR-LFOE3GQWCOVB-XQDMWN-TC3PUXMUOG2REA-DEPLOYMENT.TRIAL-GEFQBY.TRIAL.EXPIRES.20.MAY.2026"
        );
        launch(args);
    }
}
