package main.gui;

import main.model.Video;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 *
 */
public class DownloadConfig {
    private final List<Video> videos;
    private final File file;

    public DownloadConfig(List<Video> videos, File file) {
        Objects.requireNonNull(videos);

        this.videos = videos;
        this.file = file;
    }

    public List<Video> getVideos() {
        return videos;
    }

    public File getFile() {
        return file;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DownloadConfig that = (DownloadConfig) o;

        if (getVideos() != null ? !getVideos().equals(that.getVideos()) : that.getVideos() != null) return false;
        return getFile() != null ? getFile().equals(that.getFile()) : that.getFile() == null;
    }

    @Override
    public int hashCode() {
        int result = getVideos() != null ? getVideos().hashCode() : 0;
        result = 31 * result + (getFile() != null ? getFile().hashCode() : 0);
        return result;
    }
}
