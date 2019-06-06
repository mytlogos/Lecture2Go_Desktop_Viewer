package main.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;

/**
 *
 */
public class Faculty extends AbstractQueryItem {

    private ObservableList<Section> sections = FXCollections.observableArrayList();

    public Faculty(int id, String name) {
        super(id, name);
    }

    public List<Section> getSections() {
        return FXCollections.unmodifiableObservableList(this.sections);
    }

    public void addSection(Section section) {
        Objects.requireNonNull(section);

        if (!this.sections.contains(section)) {
            section.setFaculty(this);
            this.sections.add(section);
        }
    }

    public void removeSection(Section section) {
        this.sections.remove(section);
    }
}
