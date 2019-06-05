package main.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.List;

/**
 *
 */
public class Video implements HierarchyData<Video> {
    private final List<String> dozents;
    private final ObservableList<Video> children = FXCollections.observableArrayList();
    private final String cover;
    private final int id;
    private final String name;
    private final String semester;
    private final LocalDate published;
    private final List<String> tags;
    private Video parent;


    public Video(String cover, int id, String name, List<String> dozents, LocalDate published, List<String> tags) {
        this(cover, id, name, dozents, null, published, tags);
    }


    private Video(String cover, int id, String name, List<String> dozents, String semester, LocalDate published, List<String> tags) {
        this.cover = cover;
        this.id = id;
        this.name = name;
        this.dozents = dozents;
        this.semester = semester;
        this.published = published;
        this.tags = tags;
    }

    public Video(String cover, int id, String name, List<String> dozents, String semester, List<String> tags) {
        this(cover, id, name, dozents, semester, null, tags);
    }

    public static Video createSeries(String cover, int id, String name, List<String> dozents, String semester, List<String> tags, List<Video> videos) {
        final Video video = new Video(cover, id, name, dozents, semester, tags);
        video.children.addAll(videos);
        return video;
    }

    public String getCover() {
        return cover;
    }

    public String getName() {
        return name;
    }

    public String getSemester() {
        return semester;
    }

    public LocalDate getPublished() {
        return published;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getDozents() {
        return dozents;
    }

    @Override
    public ObservableList<Video> getChildren() {
        return FXCollections.unmodifiableObservableList(this.children);
    }

    @Override
    public Video getParent() {
        return this.parent;
    }

    @Override
    public void addChild(Video child) {
        if (this.parent != null) {
            throw new IllegalStateException("grandparents are not allowed!");
        }
        child.parent = this;

        if (!this.children.contains(child)) {
            this.children.add(child);
        }
    }

    @Override
    public int hashCode() {
        return getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Video video = (Video) o;

        return getId() == video.getId();
    }

    public int getId() {
        return id;
    }
}
