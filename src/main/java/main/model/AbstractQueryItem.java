package main.model;

import java.util.Objects;

/**
 *
 */
public class AbstractQueryItem implements QueryItem {

    private int id;
    private String name;

    AbstractQueryItem(int id, String name) {
        Objects.requireNonNull(name);
        if (id <= 0) {
            throw new IllegalArgumentException("id is not valid: smaller/equal to 0");
        }
        this.id = id;
        this.name = name;
    }

    @Override
    public int hashCode() {
        int result = getId();
        result = 31 * result + getName().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractQueryItem that = (AbstractQueryItem) o;

        if (getId() != that.getId()) return false;
        return getName().equals(that.getName());
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
