package main.background;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import main.GodData;
import main.background.storage.Storage;
import okhttp3.ResponseBody;
import retrofit2.Response;

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
                final List<IndexInfo> infos = Storage.getDatabase().getIndexInfos();

                final int limit = 50;
                int i = 1;

                if (!infos.isEmpty()) {
                    final IndexInfo indexInfo = infos.get(infos.size() - 1);

                    if (indexInfo.getLimit() == indexInfo.getItemCount()) {
                        i = indexInfo.getPage();
                    }
                }

                Client client = new Client();
                Response<ResponseBody> response = client.query(i, limit);

                this.updateTitle("Indexing");

                while (response.body() != null && response.isSuccessful()) {
                    System.out.println("Read Page " + i);
                    final PageInfo info = new HtmlParser().parse(response);

                    this.updateMessage("" + i * limit);

                    GodData.get().addVideo(info.getVideos());
                    GodData.get().addFaculty(info.getFaculties());
                    GodData.get().addSection(info.getSections());
                    GodData.get().addSemester(info.getSemesters());
                    GodData.get().addCategory(info.getCategories());

                    Thread.sleep(1000);
                    i++;
                    response = client.query(i, limit);
                }
                this.updateProgress(i * limit, i * limit);
                this.updateTitle("Finished Indexing");
                System.out.println("finished indexing");
                return null;
            }
        };
    }
}
