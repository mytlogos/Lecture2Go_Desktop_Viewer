package main.model;

/**
 *
 */
public class Section extends AbstractQueryItem {
    private Faculty faculty;

    public Section(int id, String name) {
        super(id, name);
    }

    public Faculty getFaculty() {
        return faculty;
    }

    void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }
}
