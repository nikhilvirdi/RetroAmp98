package io.github.nikhilvirdi.retroamp98;

import io.github.nikhilvirdi.retroamp98.controller.PlayerController;
import io.github.nikhilvirdi.retroamp98.engine.JavaFxMediaEngine;
import io.github.nikhilvirdi.retroamp98.playlist.PlaylistManager;
import io.github.nikhilvirdi.retroamp98.util.SettingsManager;
import io.github.nikhilvirdi.retroamp98.ui.PlayerViewController;
import io.github.nikhilvirdi.retroamp98.ui.WindowPlacement;
import io.github.nikhilvirdi.retroamp98.controller.ControllerListener;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import javafx.application.Platform;
import io.github.nikhilvirdi.retroamp98.library.LibraryService;
import io.github.nikhilvirdi.retroamp98.library.ListeningHistory;

public class RetroAmpApp extends Application {

    private static final int MIN_WIDTH = 420;
    private static final int MIN_HEIGHT = 570;
    private PlayerController controller;
    private PlayerViewController viewController;
    private SettingsManager settings;
    private Stage primaryStage;
    private boolean closing;

    public static void main(String[] args) {
        launch(args);
    }

    @Override public void init() {
        settings = new SettingsManager();
        settings.load();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(RetroAmpApp.class.getResource("ui/main-view.fxml"));
        Region view = loader.load();
        viewController = loader.getController();
        double designWidth = view.getPrefWidth();
        double designHeight = view.getPrefHeight();
        view.setClip(new Rectangle(designWidth, designHeight));
        StackPane viewport = new StackPane(new Group(view));
        viewport.getStyleClass().add("viewport");

        Scale transform = new Scale();
        view.getTransforms().add(transform);
        var scale = Bindings.min(viewport.widthProperty().divide(designWidth),
                viewport.heightProperty().divide(designHeight));
        transform.xProperty().bind(scale);
        transform.yProperty().bind(scale);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double initialScale = Math.min(1, Math.min((screen.getWidth() - 32) / designWidth,
                (screen.getHeight() - 64) / designHeight));
        Scene scene = new Scene(viewport, designWidth * initialScale, designHeight * initialScale);
        scene.getStylesheets().add(RetroAmpApp.class.getResource("ui/styles/main-view.css").toExternalForm());

        if (settings == null) init();
        controller = new PlayerController(new JavaFxMediaEngine(), new PlaylistManager(), new LibraryService(),
                new ListeningHistory(Path.of(System.getProperty("user.home"), ".retroamp98", "history.properties")),
                action -> { if (Platform.isFxApplicationThread()) action.run(); else Platform.runLater(action); });
        controller.applySettings(settings);
        viewController.attach(controller);
        controller.restoreHistory();
        viewController.installDesktopActions(scene);
        primaryStage = stage;

        stage.setTitle("RetroAmp98");
        stage.setScene(scene);
        stage.setMinWidth(Math.min(MIN_WIDTH, screen.getWidth()));
        stage.setMinHeight(Math.min(MIN_HEIGHT, screen.getHeight()));
        stage.show();
        var screens = new java.util.ArrayList<Rectangle2D>();
        screens.add(screen);
        for (Screen display : Screen.getScreens()) if (!screens.contains(display.getVisualBounds())) screens.add(display.getVisualBounds());
        Rectangle2D placement = WindowPlacement.restore(settings, screens, stage.getWidth(), stage.getHeight());
        stage.setWidth(placement.getWidth()); stage.setHeight(placement.getHeight());
        stage.setX(placement.getMinX()); stage.setY(placement.getMinY());
        scene.getRoot().requestFocus();
        controller.addListener(new ControllerListener() {
            @Override public void onVolumeChanged(double volume, boolean muted) { persist(); }
            @Override public void onShuffleChanged(boolean value) { persist(); }
            @Override public void onRepeatModeChanged(RepeatMode mode) { persist(); }
            @Override public void onPlaylistUpdated() { persist(); }
        });
        javafx.beans.InvalidationListener windowChanged = ignored -> { if (!stage.isIconified() && !stage.isMaximized()) persist(); };
        stage.widthProperty().addListener(windowChanged);
        stage.heightProperty().addListener(windowChanged);
        stage.xProperty().addListener(windowChanged);
        stage.yProperty().addListener(windowChanged);
    }

    private void persist() {
        if (closing || primaryStage == null) return;
        if (!primaryStage.isIconified() && !primaryStage.isMaximized()) {
            settings.setWindowWidth((int) primaryStage.getWidth());
            settings.setWindowHeight((int) primaryStage.getHeight());
            settings.setWindowX(primaryStage.getX());
            settings.setWindowY(primaryStage.getY());
        }
        controller.updateSettings(settings);
        settings.saveAsync(error -> {
            if (!closing) viewController.onNotice(ControllerListener.NoticeLevel.WARNING, "Settings", "Could not save this session. Check access to the settings folder.");
        });
    }

    @Override
    public void stop() {
        if (controller == null) return;
        persist();
        closing = true;
        viewController.close();
        controller.close();
        settings.close();
    }
}
