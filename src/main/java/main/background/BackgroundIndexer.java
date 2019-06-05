package main.background;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import main.GodData;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 *
 */
public class BackgroundIndexer extends Service<Void> {

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Client client = new Client();
                Response<ResponseBody> response = client.query(1, 50);

                int i = 1;
                this.updateTitle("Indexing");

                while (response.body() != null && response.isSuccessful()) {
                    System.out.println("Read Page " + i);
                    final PageInfo info = new HtmlParser().parse(response);

                    this.updateProgress(i * 50, -1);

                    GodData.get().addVideo(info.getVideos());
                    GodData.get().addFaculty(info.getFaculties());
                    GodData.get().addSection(info.getSections());
                    GodData.get().addSemester(info.getSemesters());
                    GodData.get().addCategory(info.getCategories());

                    Thread.sleep(1000);
                    i++;
                    response = client.query(i, 50);
                }
                this.updateProgress(i * 50, i * 50);
                this.updateTitle("Finished Indexing");
                System.out.println("finished indexing");
                return null;
            }
        };
    }
}
