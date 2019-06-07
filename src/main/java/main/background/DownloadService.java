package main.background;

import javafx.concurrent.Task;
import main.gui.DownloadConfig;
import main.model.Video;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class DownloadService {
    private final DownloadConfig config;
    private ExecutorService executor = Executors.newFixedThreadPool(2);

    public DownloadService(DownloadConfig config) {
        Objects.requireNonNull(config);
        this.config = config;
    }

    public Map<Video, Task<Void>> start() {
        final Map<Video, Task<Void>> map = new LinkedHashMap<>();
        if (!this.config.getFile().isDirectory()) {
            final Video video = this.config.getVideos().get(0);
            map.put(video, this.createTask(video));
        } else {
            for (Video video : this.config.getVideos()) {
                map.put(video, this.createTask(video));
            }
        }
        for (Task<Void> value : map.values()) {
            this.executor.execute(value);
        }
        return map;
    }

    private Task<Void> createTask(Video video) {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                final String url = video.getDownloadUrl();
                final String prefix = "https://l2gdownload.rrz.uni-hamburg.de/abo/";

                if (!url.startsWith(prefix)) {
                    throw new IllegalStateException("Download Url does not match the schema");
                }
                final Response<ResponseBody> stream = new Client().getVideoStream(url.substring(prefix.length()));
                if (stream.body() == null) {
                    throw new IllegalStateException(String.format("Could not get stream of '%s'", url));
                }
                final File file;
                if (config.getFile().isDirectory()) {
                    file = config.getFile().toPath().resolve(video.getName() + ".mp4").toFile();
                } else {
                    file = config.getFile();
                }
                if (!writeResponseBodyToDisk(video, file, stream.body())) {
                    throw new IOException("could not load everything");
                }

                return null;
            }

            private boolean writeResponseBodyToDisk(Video video, File file, ResponseBody body) {
                final long maxSize = body.contentLength() > 0 ? body.contentLength() : video.getSize();
                video.setSize(maxSize);

                byte[] fileReader = new byte[4096];
                long fileSizeDownloaded = 0;

                try (InputStream inputStream = body.byteStream(); OutputStream outputStream = new FileOutputStream(file)) {
                    int downloadedChunks = 0;
                    while (true) {
                        int read = inputStream.read(fileReader);

                        if (read == -1) {
                            break;
                        }

                        outputStream.write(fileReader, 0, read);

                        fileSizeDownloaded += read;
                        downloadedChunks++;

                        if (downloadedChunks > 50) {
                            downloadedChunks = 0;
                            this.updateProgress(fileSizeDownloaded, maxSize);
                        }
                    }
                    this.updateProgress(fileSizeDownloaded, maxSize);
                    outputStream.flush();

                    return true;
                } catch (IOException e) {
                    return false;
                }
            }
        };
    }

    public void cancel() {
        // TODO 07.6.2019: do cleanup
        this.executor.shutdownNow();
    }


}
