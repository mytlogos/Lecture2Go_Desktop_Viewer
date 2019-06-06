package main.gui;

import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import main.QueueLikeMap;
import main.background.storage.Storage;
import main.model.Video;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class VideoCell extends TreeCell<Video> {
    private static Executor loading = Executors.newFixedThreadPool(10);
    private static Executor saving = Executors.newFixedThreadPool(10);
    private static Map<Video, Integer> map = new HashMap<>();
    private static QueueLikeMap<Integer, byte[]> imageCache = new QueueLikeMap<>(100);

    @FXML
    private ImageView image;
    @FXML
    private Text title;
    @FXML
    private Text creator;
    @FXML
    private Text tags;
    @FXML
    private Text date;
    private Parent root;

    public static VideoCell createCell() {
        final VideoCell cell = new VideoCell();
        final FXMLLoader loader = new FXMLLoader(VideoCell.class.getResource("/item.fxml"));
        loader.setController(cell);
        try {
            cell.root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return cell;
    }

    @Override
    protected void updateItem(Video item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty && item != null) {
            map.merge(item, 1, Integer::sum);
            System.out.println(String.format("updating %d for the %d th time", item.getId(), map.get(item)));

            setGraphic(this.root);
            this.title.setText(item.getName());
            this.creator.setText(String.join(", ", item.getDozents()));
            this.tags.setText(String.join(", ", item.getTags()));

            if (item.getCover() != null && !item.getCover().isEmpty()) {
                this.loadCoverFromFile(item);
/*
                this.image.setDisable(true);
                final Task<InputStream> task = this.getLocalCoverImage(item);
                task.setOnSucceeded(event -> loadCoverImage(item, task.getValue()));
                //noinspection ThrowableNotThrown
                task.setOnFailed(event -> event.getSource().getException().printStackTrace());
                VideoCell.loading.execute(task);
*/
            }

            if (item.getPublished() != null) {
                this.date.setText(item.getPublished().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            } else {
                this.date.setText(item.getSemester());
            }
        } else {
            setText(null);
            setGraphic(null);
        }
    }

    private void loadCoverFromFile(Video video) {
        final String path = URI.create(video.getCover()).getPath();
        final File file = Paths.get("img", path.substring(path.lastIndexOf("/") + 1)).toFile();

        if (file.exists()) {
            try {
                this.image.setImage(new Image(new FileInputStream(file)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            try {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            final Image image = new Image(video.getCover(), true);
            this.image.setImage(image);

            image.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.intValue() == 1) {
                    if (!image.isError()) {
                        saveCoverAsFile(video, image, file);
                    } else {
                        image.getException().printStackTrace();
                    }
                }
            });
        }
    }

    private void saveCoverAsFile(Video item, Image image, File file) {
        System.out.println("saving " + item.getCover());

        VideoCell.saving.execute(() -> {
            final Matcher matcher = Pattern.compile(".+\\.(\\w+?)$").matcher(item.getCover());

            if (!matcher.matches() || matcher.group(1) == null) {
                System.err.println("invalid cover url, has no valid image extension");
                return;
            }
            final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            try {
                ImageIO.write(bufferedImage, matcher.group(1), file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private Task<InputStream> getLocalCoverImage(Video video) {
        return new Task<InputStream>() {
            @Override
            protected InputStream call() throws Exception {
                return Storage.getDatabase().loadCover(video);
            }
        };
    }

    private void loadCoverImage(Video item, InputStream inputStream) {
        this.image.setDisable(false);

        System.out.println("loading " + item.getCover() + " from " + (inputStream == null ? "internet" : "database"));

        if (inputStream == null) {
            final Image image = new Image(item.getCover(), true);
            this.image.setImage(image);

            image.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.intValue() == 1) {
                    if (!image.isError()) {
                        saveCoverImage(item, image);
                    } else {
                        image.getException().printStackTrace();
                    }
                }
            });
        } else {
            this.image.setImage(new Image(inputStream));
        }
    }

    private void saveCoverImage(Video item, Image image) {
        System.out.println("saving " + item.getCover());

        VideoCell.saving.execute(() -> {
            final Matcher matcher = Pattern.compile(".+\\.(\\w+?)$").matcher(item.getCover());

            if (!matcher.matches() || matcher.group(1) == null) {
                System.err.println("invalid cover url, has no valid image extension");
                return;
            }
            final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                ImageIO.write(bufferedImage, matcher.group(1), outputStream);
                byte[] res = outputStream.toByteArray();
                Storage.getDatabase().saveCover(res, item);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
