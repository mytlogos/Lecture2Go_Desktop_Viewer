package main.gui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import main.GodData;
import main.background.*;
import main.model.*;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

public class Controller {
    private Executor executor = Executors.newFixedThreadPool(5);

    @FXML
    private ToggleGroup view;
    @FXML
    private CheckBox searchSubVideosCheck;
    @FXML
    private TextField titleSearch;
    @FXML
    private TextField dozentSearch;
    @FXML
    private Button downloadBtn;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Text taskMessage;
    @FXML
    private ListView<Faculty> facultyList;
    @FXML
    private ListView<Section> sectionList;
    @FXML
    private ListView<Semester> semesterList;
    @FXML
    private ListView<Category> categoryList;
    @FXML
    private TreeView<Video> videoList;
    private Map<Video, TreeItem<Video>> videoTreeItemMap = new HashMap<>();
    private TaskMessenger taskMessenger;
    private FilteredList<Video> filtered;
    // set TILE as default to prevent NPE in switch statement for cell factory binding
    private ObjectProperty<View> viewProperty = new SimpleObjectProperty<>(View.TILE);

    public void initialize() {
        this.progressBar.setProgress(0);
        this.taskMessenger = new TaskMessenger(this.taskMessage.textProperty(), this.progressBar.progressProperty());

        this.facultyList.setItems(GodData.get().getFaculties());
        this.semesterList.setItems(GodData.get().getSemesters());
        this.categoryList.setItems(GodData.get().getCategories());

        this.facultyList.setCellFactory(param -> new QueryItemCell<>());
        this.sectionList.setCellFactory(param -> new QueryItemCell<>());
        this.semesterList.setCellFactory(param -> new QueryItemCell<>());
        this.categoryList.setCellFactory(param -> new QueryItemCell<>());

        this.videoList.cellFactoryProperty().bind(Bindings.createObjectBinding(() -> {
            switch (this.viewProperty.getValue()) {
                case LINE:
                    return param -> VideoLineCell.createCell(
                            this.titleSearch.textProperty(),
                            this.dozentSearch.textProperty()
                    );
                case TILE:
                default:
                    return param -> VideoTileCell.createCell(
                            this.titleSearch.textProperty(),
                            this.dozentSearch.textProperty()
                    );
            }
        }, this.viewProperty));

        this.videoList.setShowRoot(false);

        final InvalidationListener listener = observable -> {
            updatePredicate();
            runSelectedQuery();
        };

        this.titleSearch.textProperty().addListener(observable -> this.updatePredicate());
        this.dozentSearch.textProperty().addListener(observable -> this.updatePredicate());
        this.searchSubVideosCheck.selectedProperty().addListener(observable -> this.updatePredicate());
        this.facultyList.getSelectionModel().selectedItemProperty().addListener(listener);
        this.facultyList.getSelectionModel().selectedItemProperty().addListener(listener);
        this.sectionList.getSelectionModel().selectedItemProperty().addListener(listener);
        this.semesterList.getSelectionModel().selectedItemProperty().addListener(listener);
        this.categoryList.getSelectionModel().selectedItemProperty().addListener(listener);

        this.facultyList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.sectionList.setItems(FXCollections.observableArrayList(newValue.getSections()));
            } else {
                this.sectionList.setItems(FXCollections.emptyObservableList());
            }
        });

        final PauseTransition pause = new PauseTransition(Duration.seconds(0.5));

        this.filtered = GodData.get().getVideos().filtered(null);
        GodData.get().addListener(c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    //noinspection unchecked
                    final MetaDataScraper scraper = new MetaDataScraper((Collection<Video>) c.getAddedSubList());
                    this.taskMessenger.registerWorker(scraper);
                    scraper.start();
                }
            }
        });

        final MetaDataScraper scraper = new MetaDataScraper(GodData.get().getVideos());
        this.taskMessenger.registerWorker(scraper);
        scraper.setOnFailed(event -> scraper.getException().printStackTrace());
        scraper.start();

        final StartupService service = new StartupService();
        final BackgroundIndexer indexer = new BackgroundIndexer();

        this.taskMessenger.registerWorker(service);
        this.taskMessenger.registerWorker(indexer);

        indexer.setOnFailed(event -> event.getSource().getException().printStackTrace());
        service.start();
        indexer.start();

        this.filtered.addListener((ListChangeListener<? super Video>) observable -> {
            if (observable.next()) {
                pause.setOnFinished(event -> {
                    final TreeItem<Video> root = this.convertToTreeItems(this.filtered, null);
                    Platform.runLater(() -> this.videoList.setRoot(root));
                });
                pause.playFromStart();
            }
        });

        this.downloadBtn.disableProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    final TreeItem<Video> item = this.videoList.getSelectionModel().getSelectedItem();
                    return item == null || item.getValue() == null || !item.getValue().isDownloadAble();
                },
                this.videoList.getSelectionModel().selectedItemProperty()
        ));

        this.downloadBtn.setOnAction(event -> downloadSelected());
    }

    public void runSelectedQuery() {
        final Task<PageInfo> task = new Task<PageInfo>() {
            @Override
            protected PageInfo call() throws Exception {
                final Response<ResponseBody> response = new Client().query(
                        Controller.this.getSelectedFaculty(),
                        Controller.this.getSelectedSection(),
                        Controller.this.getSelectedSemester(),
                        Controller.this.getSelectedCategory()
                );
                if (response.body() == null) {
                    return null;
                }
                return new HtmlParser().parseList(response);
            }
        };
        this.taskMessenger.registerWorker(task);
        task.setOnSucceeded(event -> {
            GodData.get().addVideo(task.getValue().getVideos());
            GodData.get().addFaculty(task.getValue().getFaculties());
            GodData.get().addSection(task.getValue().getSections());
            GodData.get().addSemester(task.getValue().getSemesters());
            GodData.get().addCategory(task.getValue().getCategories());
        });
        task.setOnFailed(event -> event.getSource().getException().printStackTrace());
        executor.execute(task);
    }

    public void switchToTile() {
        this.viewProperty.setValue(View.TILE);
    }

    public void switchToLine() {
        this.viewProperty.setValue(View.LINE);
    }

    private void updatePredicate() {
        this.filtered.setPredicate(this.getPredicate());

        final Predicate<Video> predicate;

        if (this.searchSubVideosCheck.isSelected()) {
            predicate = getPredicate();
        } else {
            predicate = null;
        }

        for (Video video : GodData.get().getVideos()) {
            video.getFilteredChildren().setPredicate(predicate);
        }
    }

    private void downloadSelected() {
        final FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/downloadDialog.fxml"));
        final DialogPane pane;
        try {
            pane = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Could not open Download Dialog").show();
            return;
        }
        final DownloadDialog downloadDialog = loader.getController();
        downloadDialog.setVideo(this.videoList.getSelectionModel().getSelectedItem().getValue());

        final Dialog<DownloadConfig> dialog = new Dialog<>();
        dialog.setResultConverter(param ->
                ((param != null) && (param.getButtonData() == ButtonBar.ButtonData.YES))
                        ? downloadDialog.getDownloadConfig()
                        : null
        );
        dialog.setDialogPane(pane);
        dialog.setResizable(true);
        dialog.setWidth(500);
        final Optional<DownloadConfig> configOptional = dialog.showAndWait();
        final DownloadConfig config = configOptional.orElse(null);

        if (config == null || config.getFile() == null || config.getVideos().isEmpty()) {
            return;
        }

        final DownloadService service = new DownloadService(config);
        final Map<Video, Task<Void>> taskMap = service.start();

        if (downloadDialog.showDownloadDialog()) {
            displayDownloadProgress(service, taskMap);
        }
    }

    private void displayDownloadProgress(DownloadService service, Map<Video, Task<Void>> taskMap) {
        final FXMLLoader displayLoader = new FXMLLoader(this.getClass().getResource("/downloadViewDialog.fxml"));
        final DialogPane displayPane;
        try {
            displayPane = displayLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Could not open Download View Dialog").show();
            return;
        }
        DownloadViewDialog dialog = displayLoader.getController();
        dialog.setLoadingMap(taskMap);

        final Dialog<Boolean> displayDownload = new Dialog<>();
        displayDownload.setDialogPane(displayPane);
        displayDownload.setResultConverter(param -> param == ButtonType.CANCEL);

        if (displayDownload.showAndWait().orElse(false)) {
            service.cancel();
        }
    }

    private Faculty getSelectedFaculty() {
        return this.facultyList.getSelectionModel().getSelectedItem();
    }

    private Section getSelectedSection() {
        return this.sectionList.getSelectionModel().getSelectedItem();
    }

    private Semester getSelectedSemester() {
        return this.semesterList.getSelectionModel().getSelectedItem();
    }

    private Category getSelectedCategory() {
        return this.categoryList.getSelectionModel().getSelectedItem();
    }

    private Predicate<Video> getPredicate() {
        return video -> {
            final Category category = this.getSelectedCategory();
            final Faculty faculty = this.getSelectedFaculty();
            final Semester semester = this.getSelectedSemester();
            final Section section = this.getSelectedSection();

            if (category != null && !video.getTags().contains(category.getName())) {
                return false;
            }

            if (faculty != null && !video.getTags().contains(faculty.getName())) {
                boolean found = false;

                for (Section facultySection : faculty.getSections()) {
                    if (video.getTags().contains(facultySection.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
            if (section != null && !video.getTags().contains(section.getName())) {
                return false;
            }
            if (semester != null && !Objects.equals(semester.getName(), video.getSemester())) {
                return false;
            }

            final String titleSearchText = this.titleSearch.getText();

            if (titleSearchText != null && !titleSearchText.isEmpty() && !containsSearchString(video.getName(), titleSearchText)) {
                return false;
            }
            final String dozentSearchText = this.dozentSearch.getText();

            return dozentSearchText == null
                    || dozentSearchText.isEmpty()
                    || containsSearchString(video.getDozentsAsString(), dozentSearchText);
        };
    }

    private boolean containsSearchString(String text, String searchText) {
        final String lowerName = text.toLowerCase();

        for (String s : searchText.toLowerCase().split(" ")) {
            if (s.isEmpty()) {
                continue;
            }

            if (!lowerName.contains(s)) {
                return false;
            }
        }
        return true;
    }

    private TreeItem<Video> convertToTreeItems(Collection<Video> videos, TreeItem<Video> root) {
        if (root == null) {
            root = new TreeItem<>();
        }
        for (Video video : videos) {
            final TreeItem<Video> item = new TreeItem<>(video);
            this.videoTreeItemMap.put(video, item);

            video.getFilteredChildren().addListener((ListChangeListener<? super Video>) c -> {
                while (c.next()) {
                    if (c.wasAdded()) {
                        //noinspection unchecked
                        this.convertToTreeItems((Collection<Video>) c.getAddedSubList(), item);
                    }
                    if (c.wasRemoved()) {
                        for (Video removed : c.getRemoved()) {
                            final TreeItem<Video> removeItem = this.videoTreeItemMap.get(removed);
                            item.getChildren().remove(removeItem);
                        }
                    }
                }
            });
            root.getChildren().add(item);
            this.convertToTreeItems(video.getChildren(), item);
        }
        return root;
    }

}
