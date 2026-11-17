package io.github.nikhilvirdi.retroamp98.ui;

import io.github.nikhilvirdi.retroamp98.controller.ControllerListener;
import io.github.nikhilvirdi.retroamp98.controller.PlayerController;
import io.github.nikhilvirdi.retroamp98.engine.PlayerState;
import io.github.nikhilvirdi.retroamp98.engine.TrackInfo;
import io.github.nikhilvirdi.retroamp98.playlist.RepeatMode;
import io.github.nikhilvirdi.retroamp98.playlist.Track;
import io.github.nikhilvirdi.retroamp98.util.TimeUtils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.geometry.Side;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.scene.control.Slider;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

public final class PlayerViewController implements ControllerListener, AutoCloseable {

    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");

    @FXML private Label songTitle;
    @FXML private Label artist;
    @FXML private Label timing;
    @FXML private Label album;
    @FXML private Label trackNumber;
    @FXML private Text description;
    @FXML private ScrollPane descriptionScroll;
    @FXML private VBox songInfo;
    @FXML private ImageView artwork;
    @FXML private StackPane artworkFrame;
    @FXML private Label elapsedTime;
    @FXML private Label totalTime;
    @FXML private Region progressState;
    @FXML private Region progressFill;
    @FXML private Slider seekSlider;
    @FXML private PixelSpectrum spectrum;
    @FXML private ListView<Track> recentTracks;
    @FXML private Label trackCount;
    @FXML private Button viewSwitch;
    @FXML private Button libraryMenu;
    @FXML private Button previousButton;
    @FXML private Button playButton;
    @FXML private Button pauseButton;
    @FXML private Button stopButton;
    @FXML private Button nextButton;
    @FXML private Button volumeDownButton;
    @FXML private Button volumeUpButton;
    @FXML private Button muteButton;
    @FXML private Button shuffleButton;
    @FXML private Button repeatButton;

    private PlayerController controller;
    private Track currentTrack;
    private Duration duration = Duration.ZERO;
    private Duration position = Duration.ZERO;
    private PlayerState displayState = PlayerState.IDLE;
    private boolean updatingSlider;
    private final Tooltip noticeTooltip = new Tooltip();
    private boolean historyView;
    private volatile long artworkRevision;
    private java.util.concurrent.CompletableFuture<?> artworkTask;
    private Scene desktopScene;

    @FXML
    private void initialize() {
        recentTracks.setCellFactory(list -> new TrackCell());
        recentTracks.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2) return;
            Node target = event.getTarget() instanceof Node node ? node : null;
            while (target != null && !(target instanceof ListCell<?>)) target = target.getParent();
            if (target instanceof ListCell<?> cell && !cell.isEmpty()) playSelected();
        });
        recentTracks.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) { playSelected(); event.consume(); }
        });
        viewSwitch.setOnAction(event -> { historyView = !historyView; refreshTracks(); });
        viewSwitch.setTooltip(new Tooltip("Click to switch Playlist / Recently Played"));
        ContextMenu menu = new ContextMenu();
        menu.setAutoFix(true);
        menu.getStyleClass().add("library-actions");
        menu.getItems().addAll(action("Add audio files…", this::openFiles), action("Add folder…", this::openFolder),
                action("Open M3U…", () -> choosePlaylist(false)), action("Save M3U…", () -> choosePlaylist(true)),
                new SeparatorMenuItem(), action("Play selected", this::playSelected));
        MenuItem remove = action("Remove selected", () -> {
            int index = recentTracks.getSelectionModel().getSelectedIndex();
            if (!historyView && index >= 0) controller.removeTrack(index);
        });
        MenuItem up = action("Move selected up", () -> moveSelected(-1));
        MenuItem down = action("Move selected down", () -> moveSelected(1));
        menu.getItems().addAll(remove, up, down, new SeparatorMenuItem(), action("Clear this view", () -> {
            if (historyView) controller.clearHistory(); else controller.clearPlaylist();
        }));
        menu.setOnShowing(event -> {
            int index = recentTracks.getSelectionModel().getSelectedIndex();
            remove.setDisable(historyView || index < 0);
            up.setDisable(historyView || index <= 0);
            down.setDisable(historyView || index < 0 || index >= recentTracks.getItems().size() - 1);
        });
        recentTracks.setContextMenu(menu);
        libraryMenu.setOnAction(event -> {
            libraryMenu.setText("≡");
            menu.show(libraryMenu, Side.BOTTOM, 0, 0);
            var anchor = libraryMenu.localToScreen(libraryMenu.getBoundsInLocal());
            var displays = Screen.getScreensForRectangle(anchor.getMinX(), anchor.getMinY(), anchor.getWidth(), anchor.getHeight());
            var screen = (displays.isEmpty() ? Screen.getPrimary() : displays.getFirst()).getVisualBounds();
            menu.setX(Math.clamp(anchor.getMaxX() - menu.getWidth(), screen.getMinX(), Math.max(screen.getMinX(), screen.getMaxX() - menu.getWidth())));
        });
        libraryMenu.setTooltip(new Tooltip("Ctrl+O: files · Ctrl+Shift+O: folder\nDrop files/folders anywhere.\nSpace: play/pause · arrows: seek/volume (player background focused)."));
        refreshTracks();
        trackCount.setText("0 TRACKS");
        artwork.setVisible(false);
        Rectangle infoClip = new Rectangle();
        infoClip.widthProperty().bind(songInfo.widthProperty());
        infoClip.heightProperty().bind(songInfo.heightProperty());
        songInfo.setClip(infoClip);
        artworkFrame.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> (artwork.getImage() == null ? Math.min(artwork.getFitWidth(), artwork.getFitHeight()) : artwork.getLayoutBounds().getWidth())
                        + artworkFrame.getInsets().getLeft() + artworkFrame.getInsets().getRight(),
                artwork.layoutBoundsProperty(), artwork.imageProperty(), artworkFrame.insetsProperty()));
        artworkFrame.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> (artwork.getImage() == null ? artwork.getFitHeight() : artwork.getLayoutBounds().getHeight())
                        + artworkFrame.getInsets().getTop() + artworkFrame.getInsets().getBottom(),
                artwork.layoutBoundsProperty(), artwork.imageProperty(), artworkFrame.insetsProperty()));
        artworkFrame.minWidthProperty().bind(artworkFrame.prefWidthProperty());
        artworkFrame.maxWidthProperty().bind(artworkFrame.prefWidthProperty());
        artworkFrame.minHeightProperty().bind(artworkFrame.prefHeightProperty());
        artworkFrame.maxHeightProperty().bind(artworkFrame.prefHeightProperty());
        Tooltip.install(songTitle, new Tooltip("Ctrl+O opens audio files"));
        Tooltip.install(description, noticeTooltip);
        noticeTooltip.setWrapText(true);
        noticeTooltip.setMaxWidth(600);
        var fillWidth = Bindings.createDoubleBinding(
                () -> Math.max(0, seekSlider.getWidth() - 13) * seekSlider.getValue(),
                seekSlider.widthProperty(), seekSlider.valueProperty());
        progressFill.prefWidthProperty().bind(fillWidth);
        progressFill.maxWidthProperty().bind(fillWidth);
        seekSlider.valueProperty().addListener((observable, previous, value) -> {
            if (updatingSlider || seekSlider.isDisabled()) return;
            showTimes(Duration.ofMillis((long) (value.doubleValue() * duration.toMillis())), duration);
            if (!seekSlider.isValueChanging()) commitSeek();
        });
        seekSlider.valueChangingProperty().addListener((observable, previous, changing) -> {
            if (!changing && !updatingSlider) commitSeek();
        });
        refreshProgress();
    }

    public void attach(PlayerController controller) {
        close();
        this.controller = controller;
        controller.addListener(this);
        previousButton.setOnAction(event -> controller.previous());
        playButton.setOnAction(event -> controller.play());
        pauseButton.setOnAction(event -> controller.pause());
        stopButton.setOnAction(event -> controller.stop());
        nextButton.setOnAction(event -> controller.next());
        volumeDownButton.setOnAction(event -> controller.setVolume(controller.getMediaEngine().getVolume() - 0.05));
        volumeUpButton.setOnAction(event -> controller.setVolume(controller.getMediaEngine().getVolume() + 0.05));
        muteButton.setOnAction(event -> controller.toggleMute());
        shuffleButton.setOnAction(event -> controller.toggleShuffle());
        repeatButton.setOnAction(event -> controller.cycleRepeatMode());
        onTrackChanged(controller.getPlaylistManager().current(), controller.getPlaylistManager().getCurrentIndex());
        onVolumeChanged(controller.getMediaEngine().getVolume(), controller.getMediaEngine().isMuted());
        onShuffleChanged(controller.getPlaylistManager().isShuffle());
        onRepeatModeChanged(controller.getPlaylistManager().getRepeatMode());
        onStateChanged(controller.getMediaEngine().getState());
        refreshTracks();
    }

    public void openFiles() {
        if (controller == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Audio Files");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio files", "*.mp3", "*.wav", "*.m4a"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        List<File> files = chooser.showOpenMultipleDialog(seekSlider.getScene().getWindow());
        seekSlider.getScene().getRoot().requestFocus();
        if (files != null) controller.openFiles(files.stream().map(File::toPath).toList());
    }

    public void openFolder() {
        if (controller == null) return;
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Add Audio Folder (including subfolders)");
        File folder = chooser.showDialog(seekSlider.getScene().getWindow());
        seekSlider.getScene().getRoot().requestFocus();
        if (folder != null) controller.openFolder(folder.toPath());
    }

    public void installDesktopActions(Scene scene) {
        if (desktopScene == scene) return;
        if (desktopScene != null) throw new IllegalStateException("Desktop actions already installed");
        desktopScene = scene;
        scene.getRoot().setFocusTraversable(true);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::openFiles);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::openFolder);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (controller == null || event.isConsumed() || event.isControlDown() || event.isAltDown()
                    || event.isMetaDown() || event.isShiftDown() || ownsNavigation(scene.getFocusOwner())) return;
            switch (event.getCode()) {
                case SPACE -> controller.playPause();
                case LEFT -> controller.skip(-5);
                case RIGHT -> controller.skip(5);
                case UP -> controller.setVolume(controller.getMediaEngine().getVolume() + 0.05);
                case DOWN -> controller.setVolume(controller.getMediaEngine().getVolume() - 0.05);
                default -> { return; }
            }
            event.consume();
        });
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getTarget() instanceof Node target && !ownsNavigation(target)) scene.getRoot().requestFocus();
        });
        PseudoClass dragging = PseudoClass.getPseudoClass("import-drag");
        scene.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles() && controller != null) {
                event.acceptTransferModes(TransferMode.COPY);
                songInfo.getScene().getRoot().pseudoClassStateChanged(dragging, true);
                event.consume();
            }
        });
        scene.setOnDragExited(event -> scene.getRoot().pseudoClassStateChanged(dragging, false));
        scene.setOnDragDropped(event -> {
            scene.getRoot().pseudoClassStateChanged(dragging, false);
            boolean accepted = controller != null && event.getDragboard().hasFiles();
            if (accepted) {
                controller.openFiles(event.getDragboard().getFiles().stream().map(File::toPath).toList());
                scene.getRoot().requestFocus();
            }
            event.setDropCompleted(accepted);
            event.consume();
        });
    }

    private static boolean ownsNavigation(Node node) {
        for (Node parent = node; parent != null; parent = parent.getParent()) {
            if (parent instanceof TextInputControl || parent instanceof ListView<?> || parent instanceof Slider
                    || parent instanceof ScrollPane || parent instanceof ButtonBase || parent instanceof ComboBoxBase<?>
                    || parent instanceof TableView<?> || parent instanceof TreeView<?>) return true;
        }
        return false;
    }

    private void choosePlaylist(boolean save) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(save ? "Save Playlist" : "Open Playlist");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("M3U playlist", "*.m3u", "*.m3u8"));
        File file = save ? chooser.showSaveDialog(seekSlider.getScene().getWindow()) : chooser.showOpenDialog(seekSlider.getScene().getWindow());
        if (file != null) {
            if (save) controller.savePlaylist(file.toPath()); else controller.openPlaylist(file.toPath());
        }
    }

    private static MenuItem action(String title, Runnable action) {
        MenuItem item = new MenuItem(title);
        item.setOnAction(event -> action.run());
        return item;
    }

    private void playSelected() {
        Track track = recentTracks.getSelectionModel().getSelectedItem();
        if (track == null || controller == null) return;
        if (historyView) controller.replayHistory(track);
        else controller.playTrack(recentTracks.getSelectionModel().getSelectedIndex());
    }

    private void moveSelected(int direction) {
        int from = recentTracks.getSelectionModel().getSelectedIndex();
        int to = from + direction;
        if (!historyView && from >= 0 && to >= 0 && to < recentTracks.getItems().size()) {
            controller.moveTrack(from, to);
            recentTracks.getSelectionModel().select(to);
        }
    }

    @Override public void onPlaylistUpdated() { onFx(this::refreshTracks); }
    @Override public void onHistoryUpdated() { onFx(this::refreshTracks); }

    private void refreshTracks() {
        Track selected = recentTracks.getSelectionModel().getSelectedItem();
        List<Track> tracks = controller == null ? List.of() : historyView ? controller.getHistory()
                : controller.getPlaylistManager().getPlaylist().getTracks();
        if (!recentTracks.getItems().equals(tracks)) {
            recentTracks.getItems().setAll(tracks);
            if (selected != null) recentTracks.getSelectionModel().select(selected);
        }
        viewSwitch.setText(historyView ? "RECENTLY PLAYED" : "PLAYLIST");
        recentTracks.setAccessibleText(viewSwitch.getText());
        recentTracks.setPlaceholder(new Label(historyView ? "No listening history yet" : "Add audio files or a folder using ≡"));
        trackCount.setText(tracks.size() + " TRACKS");
        recentTracks.refresh();
    }

    private final class TrackCell extends ListCell<Track> {
        private final HBox row;
        private final Label title;
        private final Label time;
        private final StackPane badge;
        private final Region glyph;

        private TrackCell() {
            FXMLLoader loader = new FXMLLoader(PlayerViewController.class.getResource("recent-track-row.fxml"));
            try { row = loader.load(); }
            catch (java.io.IOException e) { throw new IllegalStateException("Cannot load track row", e); }
            title = (Label) loader.getNamespace().get("trackTitle");
            time = (Label) loader.getNamespace().get("trackDuration");
            badge = (StackPane) loader.getNamespace().get("trackBadge");
            glyph = (Region) loader.getNamespace().get("statusGlyph");
            row.prefWidthProperty().bind(widthProperty());
            title.setMinWidth(0);
            time.setMinWidth(105);
            ((Region) row.getChildren().getFirst()).setMinWidth(27);
        }

        @Override protected void updateItem(Track track, boolean empty) {
            super.updateItem(track, empty);
            if (empty || track == null) { setGraphic(null); return; }
            title.setText(track.getTitle() + (track.getArtist() == null ? "" : " – " + track.getArtist()));
            time.setText(track.getMetadata().duration().isZero() ? "--:--" : TimeUtils.formatDuration(track.getMetadata().duration()));
            boolean active = currentTrack != null && currentTrack.getPath().equals(track.getPath()) && displayState == PlayerState.PLAYING;
            badge.getStyleClass().remove("playing");
            if (active) badge.getStyleClass().add("playing");
            glyph.getStyleClass().setAll("glyph", active ? "levels" : "note", "badge-note");
            setGraphic(row);
            setAccessibleText(title.getText() + " " + time.getText());
        }
    }

    @Override
    public void onTrackChanged(Track track, int index) {
        onFx(() -> {
            if (currentTrack != null && track != null && currentTrack.getPath().equals(track.getPath())) {
                refreshTracks();
                return;
            }
            currentTrack = track;
            spectrum.reset();
            descriptionScroll.setVvalue(0);
            songTitle.setText(track == null ? "NO TRACK LOADED" : track.getTitle());
            artist.setText(track == null ? "" : textOr(track.getArtist(), "Unknown Artist"));
            album.setText(track == null ? "" : textOr(track.getAlbum(), "Unknown Album"));
            album.setTooltip(track == null ? null : new Tooltip(textOr(track.getAlbum(), "Unknown Album")));
            trackNumber.setText(track == null ? "" : track.getMetadata().trackNumber());
            description.setText(track == null ? "Press Ctrl+O or use ≡ to add music." : textOr(track.getMetadata().description(), "No description available."));
            noticeTooltip.setText(track == null ? "" : track.getMetadata().description());
            songTitle.setTooltip(new Tooltip(track == null ? "Ctrl+O opens audio files" : track.getTitle()));
            artist.setTooltip(track == null ? null : new Tooltip("Album artist: " + textOr(track.getMetadata().albumArtist(), "Unknown")
                    + "\nYear: " + textOr(track.getMetadata().year(), "Unknown") + "\nGenre: " + textOr(track.getMetadata().genre(), "Unknown")));
            artwork.setImage(null);
            artwork.setVisible(false);
            long revision = ++artworkRevision;
            if (artworkTask != null) artworkTask.cancel(false);
            if (track != null && controller != null) {
                PlayerController owner = controller;
                var task = owner.getLibrary().submit(() -> revision != artworkRevision ? null
                        : AlbumArtwork.decode(owner.getLibrary().metadata().artwork(track.getPath())));
                artworkTask = task;
                task.whenComplete((image, error) -> onFx(() -> {
                            if (revision != artworkRevision || controller != owner) return;
                            artwork.setImage(error == null ? image : null);
                            artwork.setVisible(error == null && image != null);
                            artwork.setAccessibleText(artwork.isVisible() ? "Embedded album artwork for " + track.getTitle() : "Album artwork unavailable");
                        }));
            }
            position = Duration.ZERO;
            duration = Duration.ZERO;
            updatingSlider = true;
            seekSlider.setValueChanging(false);
            seekSlider.setValue(0);
            updatingSlider = false;
            refreshProgress();
            refreshTracks();
        });
    }

    @Override
    public void onTrackInfoReady(TrackInfo info) {
        onFx(() -> {
            if (currentTrack == null || info == null) return;
            if (currentTrack.getMetadata().title().isBlank()) songTitle.setText(textOr(info.title(), currentTrack.getTitle()));
            if (currentTrack.getMetadata().artist().isBlank()) artist.setText(textOr(info.artist(), textOr(currentTrack.getArtist(), "Unknown Artist")));
        });
    }

    @Override
    public void onStateChanged(PlayerState state) {
        onFx(() -> {
            if (controller == null) return;
            stateChanged(controller.getMediaEngine().getState());
        });
    }

    private void stateChanged(PlayerState state) {
        progressState.pseudoClassStateChanged(stateClass(displayState), false);
        displayState = state;
        if (state != PlayerState.PLAYING) spectrum.reset();
        progressState.pseudoClassStateChanged(stateClass(state), true);
        progressState.setAccessibleText(switch (state) {
            case PLAYING -> "Pause playback";
            case LOADING -> "Loading";
            case ERROR -> "Retry playback";
            default -> "Play";
        });
        playButton.pseudoClassStateChanged(ACTIVE, state == PlayerState.PLAYING);
        pauseButton.pseudoClassStateChanged(ACTIVE, state == PlayerState.PAUSED);
        stopButton.pseudoClassStateChanged(ACTIVE, state == PlayerState.STOPPED);
        position = controller.getMediaEngine().getPosition();
        duration = controller.getMediaEngine().getDuration();
        refreshProgress();
        refreshTracks();
    }

    @Override
    public void onPositionChanged(Duration position, Duration duration) {
        onFx(() -> {
            this.position = position;
            this.duration = duration;
            refreshProgress();
        });
    }

    @Override
    public void onDurationKnown(Duration duration) {
        onFx(() -> {
            this.duration = duration;
            refreshProgress();
        });
    }

    @Override public void onSpectrum(float[] magnitudes) {
        if (!Platform.isFxApplicationThread()) {
            float[] snapshot = magnitudes.clone();
            Platform.runLater(() -> onSpectrum(snapshot));
            return;
        }
        if (controller != null && controller.getMediaEngine().getState() == PlayerState.PLAYING) spectrum.update(magnitudes);
    }

    @Override
    public void onVolumeChanged(double volume, boolean muted) {
        onFx(() -> {
            muteButton.pseudoClassStateChanged(ACTIVE, muted);
            muteButton.setTooltip(new Tooltip(muted ? "Unmute" : "Mute"));
            String level = "Volume " + Math.round(volume * 100) + "%";
            volumeDownButton.setTooltip(new Tooltip(level));
            volumeUpButton.setTooltip(new Tooltip(level));
            volumeDownButton.setDisable(volume <= 0);
            volumeUpButton.setDisable(volume >= 1);
        });
    }

    @Override
    public void onShuffleChanged(boolean shuffle) {
        onFx(() -> {
            shuffleButton.pseudoClassStateChanged(ACTIVE, shuffle);
            shuffleButton.setTooltip(new Tooltip(shuffle ? "Shuffle on" : "Shuffle off"));
        });
    }

    @Override
    public void onRepeatModeChanged(RepeatMode mode) {
        onFx(() -> {
            repeatButton.pseudoClassStateChanged(ACTIVE, mode != RepeatMode.OFF);
            repeatButton.setTooltip(new Tooltip("Repeat " + mode.name().toLowerCase(Locale.ROOT)));
        });
    }

    @Override
    public void onNotice(NoticeLevel level, String title, String message) {
        onFx(() -> {
            if (currentTrack == null || displayState == PlayerState.ERROR) description.setText(title + "\n" + message);
            noticeTooltip.setText(message);
            libraryMenu.setTooltip(new Tooltip(title + "\n" + message));
            libraryMenu.setText("!");
        });
    }

    private void refreshProgress() {
        boolean available = !duration.isZero() && !duration.isNegative()
                && displayState != PlayerState.LOADING && displayState != PlayerState.ERROR;
        seekSlider.setDisable(!available);
        updatingSlider = true;
        if (!available) seekSlider.setValueChanging(false);
        updatingSlider = false;
        if (seekSlider.isValueChanging()) return;
        showTimes(position, duration);
        updatingSlider = true;
        seekSlider.setValue(available ? Math.clamp(position.toMillis() / (double) duration.toMillis(), 0, 1) : 0);
        updatingSlider = false;
    }

    private void showTimes(Duration position, Duration duration) {
        String elapsed = TimeUtils.formatDuration(position);
        String total = duration.isZero() || duration.isNegative() ? "--:--" : TimeUtils.formatDuration(duration);
        elapsedTime.setText(elapsed);
        totalTime.setText(total);
        elapsedTime.setStyle("-fx-font-size: " + Math.min(27.5, 137.5 / elapsed.length()) + "px;");
        totalTime.setStyle("-fx-font-size: " + Math.min(27.5, 137.5 / total.length()) + "px;");
        timing.setText(elapsed + " / " + total);
    }

    private void commitSeek() {
        if (controller != null && !seekSlider.isDisabled()) {
            controller.seek(Duration.ofMillis((long) (seekSlider.getValue() * duration.toMillis())));
        }
    }

    private static String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static PseudoClass stateClass(PlayerState state) {
        return PseudoClass.getPseudoClass(state.name().toLowerCase(Locale.ROOT));
    }

    private static void onFx(Runnable action) {
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }

    @Override
    public void close() {
        currentTrack = null;
        artworkRevision++;
        if (artworkTask != null) artworkTask.cancel(false);
        if (artwork != null) artwork.setImage(null);
        if (spectrum != null) spectrum.reset();
        if (controller != null) {
            controller.removeListener(this);
            controller = null;
        }
    }
}
