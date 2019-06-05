package main.background.storage;

import main.model.*;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/**
 *
 */
public abstract class Storage {
    private static Storage STORAGE;
    private static boolean checked;

    public synchronized static Storage getDatabase() {
        Storage storage = new SqliteStorage();

        if (!checked) {
            storage.check();
            checked = true;
        }
        return storage;
    }

    public void check() {

    }

    public QueryItemHelper<Faculty> getFacultyHelper() {
        return createQueryItemHelper("faculty", Faculty::new);
    }

    abstract <T extends QueryItem> QueryItemHelper<T> createQueryItemHelper(String key, BiFunction<Integer, String, T> function);

    public QueryItemHelper<Section> getSectionHelper() {
        return createQueryItemHelper("section", Section::new);
    }

    public QueryItemHelper<Semester> getSemesterHelper() {
        return createQueryItemHelper("semester", Semester::new);
    }

    public QueryItemHelper<Category> getCategoryHelper() {
        return createQueryItemHelper("category", Category::new);
    }

    public abstract void saveCover(byte[] bytes, Video video);

    public abstract InputStream loadCover(Video video);

    public abstract List<Video> getVideos();

    public abstract void addVideos(Collection<Video> videos);

    public abstract void updateVideos(Collection<Video> videos);

    public abstract void deleteVideos(Collection<Video> videos);
}
