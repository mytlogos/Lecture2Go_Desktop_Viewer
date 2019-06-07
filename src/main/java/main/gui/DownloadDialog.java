package main.gui;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import main.Util;
import main.model.Video;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.function.Function;

/**
 *
 */
public class DownloadDialog {

    @FXML
    private CheckBox dontShowDownloadDialog;
    @FXML
    private Text neededSpace;
    @FXML
    private DialogPane root;
    @FXML
    private Text videoName;
    @FXML
    private Text fileLocation;
    @FXML
    private TableView<Video> videoList;
    @FXML
    private Text freeSpace;
    @FXML
    private Text errorField;
    private Video video;
    private File file;

    public void openFileChooser() {
        final long size;

        if (video.getChildren().isEmpty()) {
            final FileChooser chooser = new FileChooser();
            chooser.setInitialFileName(this.video.getName() + ".mp4");

            this.file = chooser.showSaveDialog(this.root.getScene().getWindow());
            size = this.video.getSize();

        } else {
            final DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Wähle einen Ordner für die Videos aus");

            this.file = chooser.showDialog(this.root.getScene().getWindow());
            size = this.video.getChildren().stream().mapToLong(Video::getSize).sum();
        }

        if (this.file != null) {
            this.fileLocation.setText(this.file.getAbsolutePath());
            final long usableSpace = this.file.getUsableSpace();

            this.freeSpace.setText(Util.humanReadableByteCount(usableSpace, false));
            this.neededSpace.setText(Util.humanReadableByteCount(size, true));

            if (size > usableSpace) {
                this.root.lookupButton(ButtonType.YES).setDisable(true);
                this.errorField.setText("Nicht genügend Speicher vorhanden");
            } else {
                this.root.lookupButton(ButtonType.YES).setDisable(false);
                this.errorField.setText(null);
            }
        }
    }

    public void setVideo(@NotNull Video video) {
        this.video = video;
        if (video.getChildren().isEmpty()) {
            this.videoList.getItems().add(video);
        } else {
            for (Video child : video.getChildren()) {
                if (child.isDownloadAble()) {
                    this.videoList.getItems().add(child);
                }
            }
        }
        this.videoName.setText(video.getName());
    }

    public void initialize() {
        this.generateColumn("Name", Video::getName);
        this.generateColumn("Länge", Video::formatDuration);
        this.generateColumn("Größe", video -> Util.humanReadableByteCount(video.getSize(), false));

        this.root.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                final int index = this.videoList.getSelectionModel().getSelectedIndex();

                if (index >= 0) {
                    this.videoList.getItems().remove(index);
                }
            }
        });

        this.videoList.getItems().addListener((InvalidationListener) observable -> {
            long size = this.videoList.getItems().stream().mapToLong(Video::getSize).sum();
            this.neededSpace.setText(Util.humanReadableByteCount(size, false));
        });
    }

    private void generateColumn(String text, Function<Video, String> extractor) {
        final TableColumn<Video, String> column = new TableColumn<>(text);
        column.setCellValueFactory(param -> new SimpleStringProperty(extractor.apply(param.getValue())));
        this.videoList.getColumns().add(column);
    }

    boolean showDownloadDialog() {
        return this.dontShowDownloadDialog == null || !this.dontShowDownloadDialog.isSelected();
    }

    DownloadConfig getDownloadConfig() {
        return new DownloadConfig(this.videoList.getItems(), this.file);
    }
}
