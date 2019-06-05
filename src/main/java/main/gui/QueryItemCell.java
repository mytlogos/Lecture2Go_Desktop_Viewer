package main.gui;

import javafx.scene.control.ListCell;
import main.model.QueryItem;

/**
 *
 */
public class QueryItemCell<T extends QueryItem> extends ListCell<T> {
    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(item.getName());
        }
    }
}
