package main.background;

/**
 *
 */
public class VideoMeta {
    private final long duration;
    private final String downloadResource;
    // size of the video in bytes
    private final long size;
    private final int videoId;

    public VideoMeta(long duration, String downloadResource, long size, int videoId) {
        this.duration = duration;
        this.downloadResource = downloadResource;
        this.size = size;
        this.videoId = videoId;
    }

    @Override
    public int hashCode() {
        int result = (int) (getDuration() ^ (getDuration() >>> 32));
        result = 31 * result + (getDownloadResource() != null ? getDownloadResource().hashCode() : 0);
        result = 31 * result + getVideoId();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VideoMeta videoMeta = (VideoMeta) o;

        if (getDuration() != videoMeta.getDuration()) return false;
        if (getVideoId() != videoMeta.getVideoId()) return false;
        return getDownloadResource() != null ? getDownloadResource().equals(videoMeta.getDownloadResource()) : videoMeta.getDownloadResource() == null;
    }

    public long getDuration() {
        return duration;
    }

    public String getDownloadResource() {
        return downloadResource;
    }

    public int getVideoId() {
        return videoId;
    }

    public long getSize() {
        return size;
    }
}
