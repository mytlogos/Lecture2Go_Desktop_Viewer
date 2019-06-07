package main;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import main.background.storage.Storage;
import main.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 *
 */
public class GodData {
    private static final GodData DATA = new GodData();
    private final ObservableList<Section> sections = FXCollections.observableArrayList();
    private final ObservableList<Faculty> faculties = FXCollections.observableArrayList();
    private final ObservableList<Semester> semesters = FXCollections.observableArrayList();
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final ObservableList<Video> videos = FXCollections.observableArrayList();
    private Executor executor = Executors.newFixedThreadPool(5);

    public static GodData get() {
        return DATA;
    }

    public ObservableList<Section> getSections() {
        return FXCollections.unmodifiableObservableList(sections);
    }

    public ObservableList<Faculty> getFaculties() {
        return FXCollections.unmodifiableObservableList(faculties);
    }

    public ObservableList<Semester> getSemesters() {
        return FXCollections.unmodifiableObservableList(semesters);
    }

    public ObservableList<Category> getCategories() {
        return FXCollections.unmodifiableObservableList(categories);
    }

    public ObservableList<Video> getVideos() {
        return FXCollections.unmodifiableObservableList(videos);
    }

    public void addListener(ListChangeListener<Video> videoListChangeListener) {
        this.videos.addListener(videoListChangeListener);
    }

    public void addSection(Collection<Section> items) {
        items.forEach(this::addSection);
    }

    public boolean addSection(Section section) {
        return add(sections, section);
    }

    private <T> boolean add(List<T> list, T value) {
        Objects.requireNonNull(list);
        Objects.requireNonNull(value);

        if (!list.contains(value)) {
            list.add(value);
            return true;
        }
        return false;
    }

    public void addFaculty(Collection<Faculty> items) {
        items.forEach(this::addFaculty);
        final Runnable runnable = () -> Storage.getDatabase().getFacultyHelper().add(items);

        if (Platform.isFxApplicationThread()) {
            this.executor.execute(runnable);
        } else {
            runnable.run();
        }
    }

    public boolean addFaculty(Faculty faculty) {
        Objects.requireNonNull(faculty);

        for (Faculty currentFaculty : this.faculties) {
            if (currentFaculty.equals(faculty)) {
                currentFaculty.addSection(faculty.getSections());
                return false;
            }
        }
        this.faculties.add(faculty);
        return true;
    }

    public void removeFaculty(Collection<Faculty> items) {
        for (Faculty item : items) {
            this.removeFaculty(item);
        }
    }

    public void removeFaculty(Faculty faculty) {
        if (faculties.remove(faculty)) {
            this.removeSection(faculty.getSections());
        }
    }

    public void removeSection(Collection<Section> items) {
        for (Section item : items) {
            this.removeSection(item);
        }
    }

    public void removeSection(Section section) {
        sections.remove(section);
    }

    public void addCategory(Collection<Category> items) {
        items.removeIf(item -> !this.addCategory(item));
        final Runnable runnable = () -> Storage.getDatabase().getCategoryHelper().add(items);

        if (Platform.isFxApplicationThread()) {
            this.executor.execute(runnable);
        } else {
            runnable.run();
        }
    }

    public boolean addCategory(Category category) {
        return add(categories, category);
    }

    public void removeCategory(Collection<Category> items) {
        for (Category item : items) {
            this.removeCategory(item);
        }
    }

    public void removeCategory(Category category) {
        categories.remove(category);
    }

    public void addSemester(Collection<Semester> items) {
        items.removeIf(item -> !this.addSemester(item));
        final Runnable runnable = () -> Storage.getDatabase().getSemesterHelper().add(items);

        if (Platform.isFxApplicationThread()) {
            this.executor.execute(runnable);
        } else {
            runnable.run();
        }
    }

    public boolean addSemester(Semester semester) {
        return add(semesters, semester);
    }

    public void removeSemester(Collection<Semester> items) {
        for (Semester item : items) {
            this.removeSemester(item);
        }
    }

    public void removeSemester(Semester semester) {
        semesters.remove(semester);
    }

    public void addVideo(Collection<Video> items) {
        List<Video> newVideos = new ArrayList<>();
        List<Video> oldVideos = new ArrayList<>();

        for (Video item : items) {
            if (this.mergeVideo(item)) {
                oldVideos.add(item);
            } else {
                newVideos.add(item);
            }
        }
        final Runnable runnable = () -> {
            videos.addAll(newVideos);
            Storage.getDatabase().addVideos(newVideos);
            Storage.getDatabase().updateVideos(oldVideos);
        };
        if (Platform.isFxApplicationThread()) {
            this.executor.execute(runnable);
        } else {
            runnable.run();
        }

    }

    /**
     * @param video video to merge with current videos
     * @return true if a equal video was found
     */
    private boolean mergeVideo(Video video) {
        Objects.requireNonNull(video);

        for (Video previousVideo : videos) {
            if (previousVideo.equals(video)) {
                for (Video child : video.getChildren()) {
                    if (!previousVideo.getChildren().contains(child)) {
                        previousVideo.addChild(child);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean addVideo(Video video) {
        Objects.requireNonNull(video);

        if (!mergeVideo(video)) {
            this.videos.add(video);
            return true;
        }
        return false;
    }

    public void removeVideo(Collection<Video> items) {
        for (Video item : items) {
            this.removeVideo(item);
        }
    }

    public void removeVideo(Video video) {
        videos.remove(video);
    }
}
