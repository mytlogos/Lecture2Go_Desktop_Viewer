package main.background;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import main.GodData;
import main.background.storage.Storage;
import main.model.Faculty;
import okhttp3.ResponseBody;
import retrofit2.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BackgroundIndexer extends Service<Void> {

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                this.updateTitle("Indexing");

                final List<Faculty> faculties = queryFacultyWithSections();
                GodData.get().addFaculty(faculties);

                // reset message and progress after querying sections
                this.updateProgress(-1, -1);
                this.updateMessage("");

                final List<IndexInfo> infos = Storage.getDatabase().getIndexInfos();

                final int limit = 50;
                int page = getPage(infos);

                Client client = new Client();
                Response<ResponseBody> response = client.query(page, limit);

                final int flushToDbLimit = 10;
                List<IndexInfo> newInfos = new ArrayList<>(flushToDbLimit);
                List<IndexInfo> updateInfos = new ArrayList<>(flushToDbLimit);

                while (response.body() != null && response.isSuccessful()) {
                    System.out.println("Read Page " + page);
                    final PageInfo pageInfo = new HtmlParser().parseList(response);

                    this.updateMessage("" + page * limit);

                    savePageInfo(pageInfo);

                    final int itemCount = pageInfo.getVideos().size();

                    if (itemCount == 0) {
                        break;
                    }

                    final IndexInfo newInfo = new IndexInfo(page, limit, LocalDateTime.now(), itemCount);

                    if (infos.size() < page) {
                        newInfos.add(newInfo);

                        if (newInfos.size() > flushToDbLimit) {
                            Storage.getDatabase().addIndexInfos(newInfos);
                        }
                    } else {
                        final IndexInfo indexInfo = infos.get(page - 1);

                        if (indexInfo.getPage() == page) {
                            updateInfos.add(newInfo);

                            if (updateInfos.size() > flushToDbLimit) {
                                Storage.getDatabase().updateIndexInfos(updateInfos);
                            }
                        } else {
                            System.out.println("pages not matching up");
                        }
                    }
                    Thread.sleep(1000);
                    page++;
                    response = client.query(page, limit);
                }
                this.updateProgress(page * limit, page * limit);
                this.updateTitle("Finished Indexing");
                System.out.println("finished indexing");
                return null;
            }

            private List<Faculty> queryFacultyWithSections() throws InterruptedException {
                this.updateMessage("Updating Faculties");

                List<Faculty> faculties = new ArrayList<>();
                Client client = new Client();
                try {
                    Response<ResponseBody> response = client.query(1, 1);
                    final PageInfo pageInfo = new HtmlParser().parseList(response);

                    List<Faculty> pageInfoFaculties = pageInfo.getFaculties();

                    for (int i = 0; i < pageInfoFaculties.size(); i++) {
                        Faculty faculty = pageInfoFaculties.get(i);
                        final Response<ResponseBody> query = client.query(new Query.Builder().setFaculty(faculty).build());
                        final PageInfo info = new HtmlParser().parseList(query);

                        faculty.addSection(info.getSections());
                        faculties.add(faculty);

                        this.updateProgress(i + 1, pageInfoFaculties.size());

                        Thread.sleep(1000);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return faculties;
            }
        };

    }


    private int getPage(List<IndexInfo> infos) {
        if (infos.isEmpty()) {
            return 1;
        }
        int page = 1;

        final IndexInfo indexInfo = infos.get(infos.size() - 1);

        if (indexInfo.getLimit() >= indexInfo.getItemCount()) {
            page = indexInfo.getPage();
        } else {
            IndexInfo previous = null;

            for (IndexInfo info : infos) {
                if (previous != null) {
                    if (info.getLastIndexed().toLocalDate().isBefore(previous.getLastIndexed().toLocalDate())) {
                        page = info.getPage();
                        previous = info;
                        break;
                    }
                }
                previous = info;
            }
            if (previous != null && page != previous.getPage()) {
                LocalDate today = LocalDate.now();

                for (IndexInfo info : infos) {
                    if (info.getLastIndexed().toLocalDate().isBefore(today)) {
                        page = info.getPage();
                        break;
                    }
                }
            }
        }
        return page;
    }

    private void savePageInfo(PageInfo pageInfo) {
        GodData.get().addVideo(pageInfo.getVideos());
        GodData.get().addFaculty(pageInfo.getFaculties());
        GodData.get().addSection(pageInfo.getSections());
        GodData.get().addSemester(pageInfo.getSemesters());
        GodData.get().addCategory(pageInfo.getCategories());
    }
}
