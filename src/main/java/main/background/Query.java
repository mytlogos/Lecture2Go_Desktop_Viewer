package main.background;

import main.model.Category;
import main.model.Faculty;
import main.model.Section;
import main.model.Semester;

/**
 *
 */
public class Query {
    private final Faculty faculty;
    private final Section section;
    private final Semester semester;
    private final Category category;
    private final int limit;
    private final int page;

    private Query(Faculty faculty, Section section, Semester semester, Category category, int limit, int page) {
        this.faculty = faculty;
        this.section = section;
        this.semester = semester;
        this.category = category;
        this.limit = limit;
        this.page = page;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public Section getSection() {
        return section;
    }

    public Semester getSemester() {
        return semester;
    }

    public Category getCategory() {
        return category;
    }

    public int getLimit() {
        return limit;
    }

    public int getPage() {
        return page;
    }

    public static class Builder {
        private Faculty faculty;
        private Section section;
        private Semester semester;
        private Category category;
        private int limit;
        private int page;

        public Builder() {
        }

        public Builder setFaculty(Faculty faculty) {
            this.faculty = faculty;
            return this;
        }

        public Builder setSection(Section section) {
            this.section = section;
            return this;
        }

        public Builder setSemester(Semester semester) {
            this.semester = semester;
            return this;
        }

        public Builder setCategory(Category category) {
            this.category = category;
            return this;
        }

        public Builder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder setPage(int page) {
            this.page = page;
            return this;
        }

        public Query build() {
            return new Query(faculty, section, semester, category, limit, page);
        }
    }
}
