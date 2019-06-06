package main.background.storage;

import main.Util;
import main.background.IndexInfo;
import main.model.Faculty;
import main.model.QueryItem;
import main.model.Section;
import main.model.Video;

import java.io.File;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiFunction;

/**
 *
 */
final class SqliteStorage extends Storage {
    private static final String name = "lec2goViewer";
    private static final String imageName = "lec2goViewerImages";
    private static final String workDir = System.getProperty("user.dir") + File.separator;
    private static final String location = workDir;
    private final SqliteConManager conManager;
    private final SqliteConManager imageConManager;
    private final Map<String, String> tables = new HashMap<>();
    private Map<String, String> imageTables = new HashMap<>();

    SqliteStorage() {
        this.conManager = new SqliteConManager(SqliteStorage.name, SqliteStorage.location);
        this.imageConManager = new SqliteConManager(SqliteStorage.imageName, SqliteStorage.location);
        this.init();
    }

    private void init() {

        this.tables.put("query_item", "key VARCHAR(255) NOT NULL, id INT NOT NULL, name VARCHAR(500) NOT NULL, PRIMARY KEY(id, key)");
        this.tables.put("section", "parent_id INT NOT NULL, id INT NOT NULL, name VARCHAR(500) NOT NULL, PRIMARY KEY(id), FOREIGN KEY(parent_id) REFERENCES query_item(id)");
        this.imageTables.put("cover", "id INT NOT NULL, image BLOB NOT NULL, PRIMARY KEY(id)");
        this.imageTables.put("index_info", "page INT NOT NULL, limit INT NOT NULL, last_indexed VARCHAR(255) NOT NULL, item_count INT NOT NULL, PRIMARY KEY(page)");
        // todo make an additional column for updates date, so that videos that never 'changed' or were never 'inserted' anymore are checked and then maybe deleted
        this.tables.put("video", "cover VARCHAR(255) NOT NULL, id INT NOT NULL, name VARCHAR(700) NOT NULL, semester VARCHAR(500), published VARCHAR(500), PRIMARY KEY(id)");
        this.tables.put("dozent", "id INT NOT NULL, name VARCHAR(700) NOT NULL, PRIMARY KEY(id, name), FOREIGN KEY (id) REFERENCES video(id)");
        this.tables.put("tags", "id INT NOT NULL, tag VARCHAR(700) NOT NULL, PRIMARY KEY(id, tag), FOREIGN KEY (id) REFERENCES video(id)");
        this.tables.put("video_video", "parent_id INT NOT NULL, sub_id INT NOT NULL, PRIMARY KEY(parent_id, sub_id), FOREIGN KEY (parent_id) REFERENCES video(id),FOREIGN KEY (sub_id) REFERENCES video(id)");
    }

    @Override
    public void check() {
        this.conManager.getConAuto(connection -> {
            for (Map.Entry<String, String> entry : this.tables.entrySet()) {
                if (this.tableDoesNotExist(connection, entry.getKey())) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("CREATE TABLE IF NOT EXISTS " + entry.getKey() + " (" + entry.getValue() + ")");
                    }
                }
            }
        });
        this.imageConManager.getConAuto(connection -> {
            for (Map.Entry<String, String> entry : this.imageTables.entrySet()) {
                if (this.tableDoesNotExist(connection, entry.getKey())) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("CREATE TABLE IF NOT EXISTS " + entry.getKey() + " (" + entry.getValue() + ")");
                    }
                }
            }
        });
    }


    @Override
    FacultyHelper createFacultyHelper() {
        return new SqliteFacultyHelper(this.conManager);
    }

    @Override
    <T extends QueryItem> QueryItemHelper<T> createQueryItemHelper(String key, BiFunction<Integer, String, T> function) {
        return new SqliteItemHelper<>(key, function, this.conManager);
    }

    @Override
    public void saveCover(byte[] bytes, Video video) {
        this.imageConManager.getConAuto(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO cover VALUES(?,?)")) {
                statement.setInt(1, video.getId());
                statement.setBytes(2, bytes);
                statement.execute();
            }
        });
    }

    @Override
    public InputStream loadCover(Video video) {
        return this.imageConManager.getConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM cover WHERE id=?;")) {
                statement.setInt(1, video.getId());
                try (ResultSet query = statement.executeQuery()) {
                    if (!query.next()) {
                        return null;
                    }
                    return query.getBinaryStream(2);
                }
            }
        });
    }

    @Override
    public List<Video> getVideos() {
        return this.conManager.getConnection(connection -> {
            List<Video> videos = new ArrayList<>();
            Map<Integer, Video> idMappings = new HashMap<>();

            try (Statement statement = connection.createStatement()) {
                try (ResultSet query = statement.executeQuery("SELECT * FROM video;")) {
                    while (query.next()) {
                        final String cover = query.getString(1);
                        final int id = query.getInt(2);
                        final String name = query.getString(3);
                        final String semester = query.getString(4);
                        final String published = query.getString(5);

                        final Video video;

                        if (semester != null) {
                            video = new Video(cover, id, name, new ArrayList<>(), semester, new ArrayList<>());
                        } else if (published != null) {
                            LocalDate publishedDate = Util.parseLocalDate(published);
                            video = new Video(cover, id, name, new ArrayList<>(), publishedDate, new ArrayList<>());
                        } else {
                            throw new IllegalStateException("video has neither semester nor published date!");
                        }
                        videos.add(video);
                        idMappings.put(video.getId(), video);
                    }
                }
                try (ResultSet query = statement.executeQuery("SELECT * FROM video_video;")) {
                    while (query.next()) {
                        final int parentId = query.getInt(1);
                        final int subId = query.getInt(2);

                        final Video parentVideo = idMappings.get(parentId);

                        if (parentVideo == null) {
                            // todo delete loose mappings
                            System.out.println("lost mapping: " + parentId);
                            continue;
                        }

                        for (Iterator<Video> iterator = videos.iterator(); iterator.hasNext(); ) {
                            Video video = iterator.next();
                            if (video.getId() == subId) {
                                parentVideo.addChild(video);
                                iterator.remove();
                                break;
                            }
                        }
                    }
                }
                try (ResultSet query = statement.executeQuery("SELECT * FROM dozent;")) {
                    while (query.next()) {
                        final int videoId = query.getInt(1);
                        final String dozent = query.getString(2);

                        final Video video = idMappings.get(videoId);

                        if (video == null) {
                            // todo delete loose mappings
                            System.out.println("lost mapping: " + videoId);
                            continue;
                        }
                        video.getDozents().add(dozent);
                    }
                }
                try (ResultSet query = statement.executeQuery("SELECT * FROM tags;")) {
                    while (query.next()) {
                        final int videoId = query.getInt(1);
                        final String tag = query.getString(2);

                        final Video video = idMappings.get(videoId);

                        if (video == null) {
                            // todo delete loose mappings
                            System.out.println("lost mapping: " + videoId);
                            continue;
                        }
                        video.getTags().add(tag);
                    }
                }
            }
            return videos;
        });
    }

    @Override
    public void addVideos(Collection<Video> videos) {
        if (videos.isEmpty()) {
            return;
        }
        this.conManager.getConAuto(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO video VALUES (?,?,?,?,?)")) {
                for (Video video : videos) {
                    addVideoBatch(statement, video);

                    for (Video child : video.getChildren()) {
                        addVideoBatch(statement, child);
                    }
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO video_video VALUES(?,?)")) {
                for (Video video : videos) {
                    for (Video child : video.getChildren()) {
                        statement.setInt(1, video.getId());
                        statement.setInt(2, child.getId());
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO dozent VALUES(?,?)")) {
                for (Video video : videos) {
                    for (String dozent : video.getDozents()) {
                        statement.setInt(1, video.getId());
                        statement.setString(2, dozent);
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO tags VALUES(?,?)")) {
                for (Video video : videos) {
                    for (String tag : video.getTags()) {
                        statement.setInt(1, video.getId());
                        statement.setString(2, tag);
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            }
        });
    }

    private void addVideoBatch(PreparedStatement statement, Video child) throws SQLException {
        statement.setString(1, child.getCover());
        statement.setInt(2, child.getId());
        statement.setString(3, child.getName());
        statement.setString(4, child.getSemester());
        statement.setString(5, Util.formatLocalDate(child.getPublished()));
        statement.addBatch();
    }

    @Override
    public void updateVideos(Collection<Video> videos) {
        if (videos.isEmpty()) {
            return;
        }
        this.conManager.getConAuto(connection -> {
           /* try (PreparedStatement statement = connection.prepareStatement("UPDATE video SET cover=?, name=?, semester=?, published=? WHERE id=?")) {
                for (Video video : videos) {
                    statement.setString(1, video.getCover());
                    statement.setString(2, video.getName());
                    statement.setString(3, video.getSemester());
                    statement.setString(4, Util.formatLocalDate(video.getPublished()));
                    statement.setInt(5, video.getId());
                    statement.addBatch();
                }
                statement.executeBatch();
            }*/

            try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO video_video VALUES(?,?)")) {
                for (Video video : videos) {
                    for (Video child : video.getChildren()) {
                        statement.setInt(1, video.getId());
                        statement.setInt(2, child.getId());
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO dozent VALUES(?,?)")) {
                for (Video video : videos) {
                    for (String dozent : video.getDozents()) {
                        statement.setInt(1, video.getId());
                        statement.setString(2, dozent);
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO tags VALUES(?,?)")) {
                for (Video video : videos) {
                    for (String tag : video.getTags()) {
                        statement.setInt(1, video.getId());
                        statement.setString(2, tag);
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            }
        });
    }

    @Override
    public void deleteVideos(Collection<Video> videos) {
        this.conManager.getConAuto(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM video_video WHERE parent_id=? OR sub_id=?")) {
                for (Video video : videos) {
                    statement.setInt(1, video.getId());
                    statement.setInt(2, video.getId());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM video WHERE id=?")) {
                for (Video video : videos) {
                    statement.setInt(1, video.getId());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    @Override
    public List<IndexInfo> getIndexInfos() {
        return this.conManager.getConnectionAuto(connection -> {
            List<IndexInfo> indexInfos = new ArrayList<>();
            try (Statement statement = connection.createStatement()) {
                try (ResultSet query = statement.executeQuery("SELECT * FROM index_info;")) {
                    while (query.next()) {
                        final int page = query.getInt(1);
                        final int limit = query.getInt(2);
                        final String lastIndexedString = query.getString(3);
                        final int itemCount = query.getInt(4);

                        indexInfos.add(new IndexInfo(page, limit, Util.parseLocalDateTime(lastIndexedString), itemCount, ""));
                    }
                }
            }
            return indexInfos;
        });
    }

    @Override
    public void addIndexInfos(List<IndexInfo> infos) {
        this.conManager.getConAuto(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO index_info VALUES(?,?,?,?)")) {
                for (IndexInfo info : infos) {
                    statement.setInt(1, info.getPage());
                    statement.setInt(2, info.getLimit());
                    statement.setString(3, Util.formatLocalDateTime(info.getLastIndexed()));
                    statement.setInt(4, info.getItemCount());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    @Override
    public void updateIndexInfos(List<IndexInfo> infos) {
        this.conManager.getConAuto(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE index_info SET limit=?, last_indexed=?, item_count=? WHERE page=?;")) {
                for (IndexInfo info : infos) {
                    statement.setInt(1, info.getLimit());
                    statement.setString(2, Util.formatLocalDateTime(info.getLastIndexed()));
                    statement.setInt(3, info.getItemCount());
                    statement.setInt(4, info.getPage());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    /**
     * Checks if the specified table of the class exists already.
     * Uses the given Connection.
     *
     * @param connection connection to the database
     * @return true if the table exists
     * @throws SQLException if there was an error while checking with the database
     */
    private boolean tableDoesNotExist(Connection connection, String table) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        boolean exists;

        try (ResultSet res = meta.getTables(null, null, table, null)) {
            exists = false;
            while (res.next()) {
                if (table.equalsIgnoreCase(res.getString("TABLE_NAME"))) {
                    exists = true;
                    break;
                }
            }
        }
        return !exists;
    }


    /**
     *
     */
    private static class SqliteItemHelper<T extends QueryItem> extends QueryItemHelper<T> {
        private final SqliteConManager conManager;

        SqliteItemHelper(String key, BiFunction<Integer, String, T> createFunction, SqliteConManager conManager) {
            super(key, createFunction);
            this.conManager = conManager;
        }

        @Override
        public void add(Collection<T> items) {
            if (items.isEmpty()) {
                return;
            }
            this.conManager.getConAuto(connection -> {
                for (T item : items) {
                    this.add(item, connection);
                }
            });
        }

        @Override
        public void add(T item) {
            this.conManager.getConAuto(connection -> add(item, connection));
        }

        @Override
        public void delete(Collection<T> items) {
            if (items.isEmpty()) {
                return;
            }
            this.conManager.getConAuto(connection -> {
                for (T item : items) {
                    this.delete(item, connection);
                }
            });
        }

        @Override
        public void delete(T item) {
            this.conManager.getConAuto(connection -> delete(item, connection));
        }

        @Override
        public List<T> getAll() {
            return this.conManager.getConnection(connection -> {
                List<T> list = new ArrayList<>();

                try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM query_item WHERE key=?;")) {
                    statement.setString(1, this.key);

                    try (ResultSet query = statement.executeQuery()) {
                        while (query.next()) {
                            final int id = query.getInt(2);
                            final String name = query.getString(3);

                            final T t = this.createFunction.apply(id, name);
                            if (t != null) {
                                list.add(t);
                            }
                        }
                    }
                }
                return list;
            });
        }

        private void delete(T item, Connection connection) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM query_item WHERE id=? AND key=?")) {
                statement.setInt(1, item.getId());
                statement.setString(2, this.key);
                statement.execute();
            }
        }

        private void add(T item, Connection connection) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO query_item VALUES (?,?,?)")) {
                statement.setString(1, this.key);
                statement.setInt(2, item.getId());
                statement.setString(3, item.getName());
                statement.execute();
            }
        }

    }

    private static class SqliteFacultyHelper extends FacultyHelper {
        private final SqliteConManager conManager;

        SqliteFacultyHelper(SqliteConManager conManager) {
            this.conManager = conManager;
        }

        @Override
        public void add(Collection<Faculty> items) {
            if (items.isEmpty()) {
                return;
            }
            this.conManager.getCon(connection -> {
                for (Faculty item : items) {
                    this.add(item, connection);
                }
                connection.commit();
            });
        }

        @Override
        public void add(Faculty item) {
            this.conManager.getConAuto(connection -> add(item, connection));
        }

        @Override
        public void delete(Collection<Faculty> items) {
            if (items.isEmpty()) {
                return;
            }
            this.conManager.getCon(connection -> {
                for (Faculty item : items) {
                    this.delete(item, connection);
                }
                connection.commit();
            });
        }

        @Override
        public void delete(Faculty item) {
            this.conManager.getConAuto(connection -> delete(item, connection));
        }

        @Override
        public List<Faculty> getAll() {
            return this.conManager.getConnection(connection -> {
                List<Faculty> list = new ArrayList<>();
                Map<Integer, Faculty> map = new HashMap<>();

                try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM query_item WHERE key=?;")) {
                    statement.setString(1, this.key);

                    try (ResultSet query = statement.executeQuery()) {
                        while (query.next()) {
                            final int id = query.getInt(2);
                            final String name = query.getString(3);

                            final Faculty t = new Faculty(id, name);
                            map.put(id, t);
                            list.add(t);
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM section;")) {
                    try (ResultSet query = statement.executeQuery()) {
                        while (query.next()) {
                            final int parentId = query.getInt(1);
                            final int id = query.getInt(2);
                            final String name = query.getString(3);

                            final Section t = new Section(id, name);
                            final Faculty faculty = map.get(parentId);

                            if (faculty == null) {
                                System.out.println("loose section: " + id);
                                continue;
                            }
                            faculty.addSection(t);
                        }
                    }
                }
                return list;
            });
        }

        private void delete(Faculty item, Connection connection) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM query_item WHERE id=? AND key=?")) {
                statement.setInt(1, item.getId());
                statement.setString(2, this.key);
                statement.execute();
            }
        }

        private void add(Faculty item, Connection connection) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO query_item VALUES (?,?,?)")) {
                statement.setString(1, this.key);
                statement.setInt(2, item.getId());
                statement.setString(3, item.getName());
                statement.execute();
            }
        }

    }

}
