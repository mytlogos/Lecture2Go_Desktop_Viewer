package main.gui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import main.Util;
import main.model.Video;

import java.util.Map;
import java.util.function.Function;

/**
 *
 */
public class DownloadViewDialog {
    @FXML
    private DialogPane root;
    @FXML
    private Text fileLocation;
    @FXML
    private TableView<Video> videoList;
    @FXML
    private TextArea errorField;
    private Map<Video, Task<Void>> loadingMap;

    public void initialize() {
        this.generateColumn("Name", Video::getName);
        this.generateColumn("Größe", video -> Util.humanReadableByteCount(video.getSize(), false));
        this.generateColumnProperty("Größe geladen", video -> Bindings.createStringBinding(
                () -> {
                    final long bytesLoaded = (long) (this.loadingMap.get(video).getProgress() * video.getSize());
                    return Util.humanReadableByteCount(bytesLoaded, false);
                },
                this.loadingMap.get(video).progressProperty()
        ));
        this.generateColumnProperty("Heruntergeladen", video -> {
            final ReadOnlyDoubleProperty progressProperty = this.loadingMap.get(video).progressProperty();
            progressProperty.addListener((observable, oldValue, newValue) -> {
                if (newValue.intValue() < 0) {
                    System.out.println("sth was not good");
                }
            });
            return Bindings
                    .when(progressProperty.greaterThan(0))
                    .then(progressProperty.multiply(100).asString("%.2f%%"))
                    .otherwise("0.00");
        });
    }

    private void generateColumn(String text, Function<Video, String> extractor) {
        final TableColumn<Video, String> column = new TableColumn<>(text);
        column.setCellValueFactory(param -> new SimpleStringProperty(extractor.apply(param.getValue())));
        this.videoList.getColumns().add(column);
    }

    private void generateColumnProperty(String text, Function<Video, ObservableStringValue> extractor) {
        final TableColumn<Video, String> column = new TableColumn<>(text);
        column.setCellValueFactory(param -> extractor.apply(param.getValue()));
        this.videoList.getColumns().add(column);
    }

    public void setLoadingMap(Map<Video, Task<Void>> loadingMap) {
        this.loadingMap = loadingMap;
        this.videoList.getItems().addAll(loadingMap.keySet());

        for (Map.Entry<Video, Task<Void>> entry : loadingMap.entrySet()) {
            entry.getValue().setOnFailed(event -> concatErrorMessage(entry.getKey(), event.getSource().getException().getMessage()));
        }
    }

    private void concatErrorMessage(Video video, String msg) {
        final String newMsg = String.format("%s\n%s.15 failed: %s.80", this.errorField.getText(), video.getName(), msg);
        this.errorField.setText(newMsg);
    }
}
