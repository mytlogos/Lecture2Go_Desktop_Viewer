package main.gui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import main.GodData;
import main.background.*;
import main.model.*;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class Controller {
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

    public void initialize() {
        this.taskMessenger = new TaskMessenger(this.taskMessage.textProperty(), this.progressBar.progressProperty());

        final StartupService service = new StartupService();
        final BackgroundIndexer indexer = new BackgroundIndexer();

        this.taskMessenger.registerWorker(service);
        this.taskMessenger.registerWorker(indexer);

        service.start();
        indexer.start();

        this.facultyList.setItems(GodData.get().getFaculties());
        this.sectionList.setItems(GodData.get().getSections());
        this.semesterList.setItems(GodData.get().getSemesters());
        this.categoryList.setItems(GodData.get().getCategories());

        runSelectedQuery();

        this.facultyList.setCellFactory(param -> new QueryItemCell<>());
        this.sectionList.setCellFactory(param -> new QueryItemCell<>());
        this.semesterList.setCellFactory(param -> new QueryItemCell<>());
        this.categoryList.setCellFactory(param -> new QueryItemCell<>());
        this.videoList.setCellFactory(param -> VideoCell.createCell());
        this.videoList.setShowRoot(false);

        final FilteredList<Video> filtered = GodData.get().getVideos().filtered(null);
        final InvalidationListener listener = observable -> filtered.setPredicate(this.getPredicate());

        this.facultyList.getSelectionModel().selectedItemProperty().addListener(listener);
        this.sectionList.getSelectionModel().selectedItemProperty().addListener(listener);
        this.semesterList.getSelectionModel().selectedItemProperty().addListener(listener);
        this.categoryList.getSelectionModel().selectedItemProperty().addListener(listener);

        final PauseTransition pause = new PauseTransition(Duration.seconds(1));

        filtered.addListener((ListChangeListener<? super Video>) observable -> {
            if (observable.next()) {
                pause.setOnFinished(event -> runSelectedQuery());
                pause.playFromStart();

                final TreeItem<Video> root = this.convertToTreeItems(filtered, null);
                Platform.runLater(() -> this.videoList.setRoot(root));
            }
        });

        this.downloadBtn.disableProperty().bind(this.videoList.selectionModelProperty().isNull());
        this.downloadBtn.setOnAction(event -> downloadSelected());
    }

    private void downloadSelected() {
        System.out.println("downloading");
    }

    private void runSelectedQuery() {
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
                String body = new String(response.body().bytes());
                final PageInfo info = new HtmlParser().parse(response.raw().request().url().toString(), body);

                GodData.get().addVideo(info.getVideos());
                GodData.get().addFaculty(info.getFaculties());
                GodData.get().addSection(info.getSections());
                GodData.get().addSemester(info.getSemesters());
                GodData.get().addCategory(info.getCategories());

                return info;
            }
        };
        this.taskMessenger.registerWorker(task);
        task.setOnFailed(event -> System.out.println(event.getSource().getException()));
        new Thread(task).start();
    }

    private Predicate<Video> getPredicate() {
        return video -> {
            final Category category = this.getSelectedCategory();
            if (category != null) {
                category.getName();
            }
            final Faculty faculty = this.getSelectedFaculty();
/*
            if (faculty != null && !video.getTags().contains(faculty.getName())) {
                return false;
            }
            final Section section = this.getSelectedSection();

            if (section != null && !video.getTags().contains(section.getName())) {

            }*/
            final Semester semester = this.getSelectedSemester();

            if (semester != null && !video.getSemester().equals(semester.getName())) {
                return false;
            }
            return true;
        };
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

    private TreeItem<Video> convertToTreeItems(Collection<Video> videos, TreeItem<Video> root) {
        if (root == null) {
            root = new TreeItem<>();
        }
        for (Video video : videos) {
            final TreeItem<Video> item = new TreeItem<>(video);
            this.videoTreeItemMap.put(video, item);

            video.getChildren().addListener((ListChangeListener<? super Video>) c -> {
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
