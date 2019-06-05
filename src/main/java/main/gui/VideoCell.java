package main.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import main.background.storage.Storage;
import main.model.Video;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class VideoCell extends TreeCell<Video> {
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
            setGraphic(this.root);
            this.title.setText(item.getName());
            this.creator.setText(String.join(", ", item.getDozents()));
            this.tags.setText(String.join(", ", item.getTags()));

            if (item.getCover() != null && !item.getCover().isEmpty()) {
                final InputStream inputStream = Storage.getDatabase().loadCover(item);

                if (inputStream == null) {
                    final Image image = new Image(item.getCover(), true);
                    this.image.setImage(image);

                    image.progressProperty().addListener((observable, oldValue, newValue) -> {

                        if (newValue.intValue() == 1) {
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
                        }
                    });
                } else {
                    this.image.setImage(new Image(inputStream));
                }
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
}
