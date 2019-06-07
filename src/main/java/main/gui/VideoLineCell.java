package main.gui;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import main.model.Video;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class VideoLineCell extends TreeCell<Video> {
    private final StringProperty titleSearch;
    private final StringProperty dozentSearch;

    @FXML
    private TextFlow titleFlow;
    @FXML
    private TextFlow creatorFlow;
    @FXML
    private Text date;
    @FXML
    private ImageView downloadAble;
    @FXML
    private Text duration;
    private Parent root;

    private VideoLineCell(StringProperty titleSearch, StringProperty dozentSearch) {
        this.titleSearch = titleSearch;
        this.dozentSearch = dozentSearch;
        this.titleSearch.addListener(observable -> this.updateTitleHighlight());
        this.dozentSearch.addListener(observable -> this.updateCreatorHighlight());
    }

    private void updateTitleHighlight() {
        final Video item = this.getItem();

        if (item == null || this.isEmpty()) {
            return;
        }

        highlightText(this.titleSearch.get(), this.titleFlow, item.getName(), 18);
    }

    private void updateCreatorHighlight() {
        final Video item = this.getItem();

        if (item == null || this.isEmpty()) {
            return;
        }
        highlightText(this.dozentSearch.get(), this.creatorFlow, item.getDozentsAsString(), 12);
    }

    private void highlightText(String query, TextFlow textFlow, String textString, double size) {
        final Video item = this.getItem();

        if (item == null || this.isEmpty()) {
            return;
        }

        textFlow.getChildren().clear();

        if (query == null || query.isEmpty()) {
            final Text text = new Text(textString);
            text.setFont(Font.font(Font.getDefault().getFamily(), size));
            textFlow.getChildren().add(text);
            return;
        }

        List<IndexRange> ranges = new ArrayList<>();
        final String lowerName = textString.toLowerCase();

        for (String s : query.toLowerCase().split(" ")) {
            if (s.isEmpty()) {
                continue;
            }

            int index = lowerName.indexOf(s);

            while (index >= 0) {
                ranges.add(new IndexRange(index, index + s.length()));
                index = lowerName.indexOf(s, index + s.length());
            }
        }
        ranges.sort(Comparator.comparingInt(IndexRange::getStart));

        List<IndexRange> mergedRanges = new ArrayList<>();

        for (IndexRange range : new ArrayList<>(ranges)) {
            for (boolean changed = true; changed; ) {
                changed = false;

                for (Iterator<IndexRange> iterator = ranges.iterator(); iterator.hasNext(); ) {
                    IndexRange indexRange = iterator.next();

                    // if the start of indexRange is in the bounds of the range
                    if (range.getStart() <= indexRange.getStart() && range.getEnd() >= indexRange.getStart() && range != indexRange) {
                        int end = range.getEnd() > indexRange.getEnd() ? range.getEnd() : indexRange.getEnd();
                        range = new IndexRange(range.getStart(), end);
                        iterator.remove();
                        changed = true;
                        break;
                    }
                }
            }
            mergedRanges.add(range);
        }

        int index = 0;
        int length = 0;

        for (IndexRange range : mergedRanges) {
            // an substring that needs no highlight
            if (range.getStart() > index) {
                final String substring = textString.substring(index, range.getStart());
                length += substring.length();

                final Text text = new Text(substring);
                text.setFont(Font.font(Font.getDefault().getFamily(), size));
                textFlow.getChildren().add(text);
            }
            final Text text = new Text(textString.substring(range.getStart(), range.getEnd()));
            length += text.getText().length();

            text.setFill(Color.DARKRED);
            text.setFont(Font.font(text.getFont().getFamily(), FontWeight.BOLD, size));

            textFlow.getChildren().add(text);
            index = range.getEnd();
        }

        // move the rest of the name in a text
        if (index < textString.length()) {
            final String substring = textString.substring(index);
            length += substring.length();

            final Text text = new Text(substring);
            text.setFont(Font.font(Font.getDefault().getFamily(), size));
            textFlow.getChildren().add(text);
        }

        //TODO 07.6.2019: remove this if its sure that it aint happening again
        if (length < textString.length()) {
            System.err.println("we lost some parts of the string!");
        }
    }

    static VideoLineCell createCell(StringProperty titleSearch, StringProperty dozentSearch) {
        final VideoLineCell cell = new VideoLineCell(titleSearch, dozentSearch);
        final FXMLLoader loader = new FXMLLoader(VideoTileCell.class.getResource("/lineItem.fxml"));
        loader.setController(cell);

        try {
            cell.root = loader.load();
            return cell;
        } catch (IOException e) {

            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Could not load View").show();
            return null;
        }
    }

    @Override
    protected void updateItem(Video item, boolean empty) {
        super.updateItem(item, empty);
        // TODO 07.6.2019: if the title is too long, it does not flow around or get clipped, which stretches the item
        if (!empty && item != null) {
            setGraphic(this.root);

            this.updateCreatorHighlight();
            this.updateTitleHighlight();

            if (item.getPublished() != null) {
                this.date.setText(item.getPublished().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            } else {
                this.date.setText(item.getSemester());
            }

            if (item.getDuration() > 0) {
                this.duration.setText(Video.formatDuration(item));
            } else if (!item.getChildren().isEmpty()) {
                final long sum = item.getChildren().stream().mapToLong(Video::getDuration).sum();

                if (sum > 0) {
                    this.duration.setText(Video.formatDuration(sum));
                }
            }

            if (item.isDownloadAble()) {
                this.downloadAble.setImage(new Image("/download.png"));
            } else {
                this.downloadAble.setImage(new Image("/downloadDisabled.png"));
            }
        } else {
            setText(null);
            setGraphic(null);
        }
    }
}
