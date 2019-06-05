package main.model;

/**
 *
 */
public class AbstractQueryItem implements QueryItem {

    private int id;
    private String name;

    public AbstractQueryItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
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
    public int hashCode() {
        int result = getId();
        result = 31 * result + getName().hashCode();
        return result;
    }
}
