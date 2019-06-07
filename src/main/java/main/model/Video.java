package main.model;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

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
    // duration in seconds
    private long duration;
    private String downloadUrl;
    // size in bytes, probably, but it differs from the actual size at the factor of ~4 (greater than it is)
    private LongProperty size = new SimpleLongProperty();
    private FilteredList<Video> filtered;


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

    public static String formatDuration(Video video) {
        return formatDuration(video.getDuration());
    }

    public static String formatDuration(long duration) {
        long hours = duration / 3600;
        long minutes = (duration % 3600) / 60;
        long seconds = duration % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * @return size of the video in Bytes
     */
    public long getSize() {
        return size.get();
    }

    public void setSize(long size) {
        this.size.set(size);
    }

    public ReadOnlyLongProperty sizeProperty() {
        return LongProperty.readOnlyLongProperty(size);
    }

    public boolean isDownloadAble() {
        return (this.getDownloadUrl() != null && !this.getDownloadUrl().isEmpty())
                || this.getChildren().stream().anyMatch(Video::isDownloadAble);
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
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
        if (this == child) {
            throw new IllegalArgumentException("cannot be parent and child of itself");
        }
        child.parent = this;

        if (!this.children.contains(child)) {
            this.children.add(child);
        }
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

    public String getDozentsAsString() {
        return String.join(", ", this.getDozents());
    }

    public List<String> getDozents() {
        return dozents;
    }

    public FilteredList<Video> getFilteredChildren() {
        if (this.filtered == null) {
            this.filtered = this.children.filtered(null);
        }
        return this.filtered;
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
