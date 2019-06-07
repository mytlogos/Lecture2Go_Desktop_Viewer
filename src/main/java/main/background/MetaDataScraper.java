package main.background;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import main.GodData;
import main.background.storage.Storage;
import main.model.Video;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;
import java.util.*;

/**
 *
 */
public class MetaDataScraper extends Service<Void> {
    private final static Set<Video> currentSet = new HashSet<>();

    private final Collection<Video> videos;

    public MetaDataScraper(Collection<Video> videos) {
        this.videos = new ArrayList<>(videos);
    }

    public MetaDataScraper() {
        this.videos = new ArrayList<>(GodData.get().getVideos());
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                final Collection<Video> unworked = MetaDataScraper.getUnworked(MetaDataScraper.this.videos);
                if (unworked.isEmpty()) {
                    System.out.println("nothing to see, nothing to scrape");
                    return null;
                }
                final Client client = new Client();
                Set<Video> changedVideos = new HashSet<>();
                System.out.println(String.format("scraping meta for %d videos on %s", unworked.size(), Thread.currentThread().getName()));

                for (Video video : unworked) {
                    if (video.getChildren().isEmpty()) {
                        if (video.getDownloadUrl() == null || video.getDownloadUrl().isEmpty()) {
                            parseVideoPage(client, changedVideos, video);
                        }
                    } else if (video.getChildren().stream().anyMatch(child -> child.getDuration() == 0)) {
                        parseXmlFeed(client, changedVideos, video);
                    }

                    System.out.println(String.format("scraped %d video meta", changedVideos.size()));

                    if (changedVideos.size() > 10) {
                        Storage.getDatabase().updateVideos(changedVideos);
                        changedVideos.clear();
                    }
                }
                if (!changedVideos.isEmpty()) {
                    Storage.getDatabase().updateVideos(changedVideos);
                }
                MetaDataScraper.currentSet.removeAll(unworked);
                System.out.println(String.format("finished meta data scraping: got at least %d", changedVideos.size()));
                return null;
            }
        };
    }

    private synchronized static Collection<Video> getUnworked(Collection<Video> videos) {
        videos.removeAll(MetaDataScraper.currentSet);
        MetaDataScraper.currentSet.addAll(videos);
        return videos;
    }

    private void parseVideoPage(Client client, Set<Video> changedVideos, Video video) throws IOException {
        final Response<ResponseBody> page = client.getVideoPage(video.getId());
        final VideoMeta meta = new HtmlParser().parseVideoPage(page, video);

        if (meta != null) {
            video.setDuration(meta.getDuration());
            video.setDownloadUrl(meta.getDownloadResource());
            changedVideos.add(video);
        }
    }

    private void parseXmlFeed(Client client, Set<Video> changedVideos, Video video) throws IOException {
        final Response<ResponseBody> xml = client.getVideoXml(video.getId());
        final List<VideoMeta> metas = new VideoFeedParser().parse(xml);

        if (metas == null) {
            return;
        }

        final List<Video> children = new ArrayList<>(video.getChildren());

        for (VideoMeta meta : metas) {
            for (Iterator<Video> iterator = children.iterator(); iterator.hasNext(); ) {
                Video child = iterator.next();

                if (child.getId() == meta.getVideoId()) {
                    iterator.remove();
                    child.setDuration(meta.getDuration());
                    child.setDownloadUrl(meta.getDownloadResource());
                    child.setSize(meta.getSize());
                    break;
                }
            }
        }
        if (children.size() < video.getChildren().size()) {
            changedVideos.add(video);
        }
    }
}
