package main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import main.background.storage.Storage;
import main.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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

    public void addSection(Collection<Section> items) {
        items.removeIf(item -> !this.addSection(item));
        Storage.getDatabase().getSectionHelper().add(items);
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

    public void removeSection(Collection<Section> items) {
        for (Section item : items) {
            this.removeSection(item);
        }
    }

    public void removeSection(Section section) {
        sections.remove(section);
    }

    public void addFaculty(Collection<Faculty> items) {
        items.removeIf(item -> !this.addFaculty(item));
        Storage.getDatabase().getFacultyHelper().add(items);
    }

    public boolean addFaculty(Faculty faculty) {
        return add(faculties, faculty);
    }

    public void removeFaculty(Collection<Faculty> items) {
        for (Faculty item : items) {
            this.removeFaculty(item);
        }
    }

    public void removeFaculty(Faculty faculty) {
        faculties.remove(faculty);
    }

    public void addCategory(Collection<Category> items) {
        items.removeIf(item -> !this.addCategory(item));
        Storage.getDatabase().getCategoryHelper().add(items);
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
        Storage.getDatabase().getSemesterHelper().add(items);
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
            if (this.addVideo(item)) {
                newVideos.add(item);
            } else {
                oldVideos.add(item);
            }
        }
        Storage.getDatabase().addVideos(newVideos);
        Storage.getDatabase().updateVideos(oldVideos);
    }

    public boolean addVideo(Video video) {
        Objects.requireNonNull(video);

        for (Video previousVideo : videos) {
            if (previousVideo.equals(video)) {
                for (Video child : video.getChildren()) {
                    if (!previousVideo.getChildren().contains(child)) {
                        previousVideo.addChild(child);
                    }
                }
                return false;
            }
        }
        this.videos.add(video);
        return true;
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
