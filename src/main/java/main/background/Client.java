package main.background;

import main.model.*;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Client {

    private final Requests requests;

    public Client() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://lecture2go.uni-hamburg.de/")
                .build();

        this.requests = retrofit.create(Requests.class);
    }

    public Response<ResponseBody> query(Faculty faculty, Section section, Semester semester, Category category) throws IOException {
        return this.requests.getList(this.getQueryId(faculty), this.getQueryId(section), this.getQueryId(semester), this.getQueryId(category)).execute();
    }

    private int getQueryId(QueryItem item) {
        return item == null ? 0 : item.getId();
    }

    public Response<ResponseBody> query(int page, int number) throws IOException {
        return this.query(null, null, null, null, page, number);
    }

    public Response<ResponseBody> query(Faculty faculty, Section section, Semester semester, Category category, int page, int number) throws IOException {
        Map<String, String> query = new HashMap<>();
        query.put("p_p_id", "lgopenaccessvideos_WAR_lecture2goportlet");
        query.put("_lgopenaccessvideos_WAR_lecture2goportlet_parentInstitutionId", this.getQueryId(faculty) + "");
        query.put("_lgopenaccessvideos_WAR_lecture2goportlet_institutionId", this.getQueryId(section) + "");
        query.put("_lgopenaccessvideos_WAR_lecture2goportlet_termId", this.getQueryId(semester) + "");
        query.put("_lgopenaccessvideos_WAR_lecture2goportlet_categoryId", this.getQueryId(category) + "");
        query.put("_lgopenaccessvideos_WAR_lecture2goportlet_creatorId", "0");
        query.put("_lgopenaccessvideos_WAR_lecture2goportlet_delta", number + "");
        query.put("_lgopenaccessvideos_WAR_lecture2goportlet_advancedSearch", "false");
        query.put("_lgopenaccessvideos_WAR_lecture2goportlet_andOperator", "true");
        query.put("_lgopenaccessvideos_WAR_lecture2goportlet_resetCur", "false");
        query.put("_lgopenaccessvideos_WAR_lecture2goportlet_cur", page + "");
        return this.requests.query(query).execute();
    }

    public Response<ResponseBody> query(Query query) throws IOException {
        return this.query(
                query.getFaculty(),
                query.getSection(),
                query.getSemester(),
                query.getCategory(),
                query.getPage(),
                query.getLimit()
        );
    }

    public Response<ResponseBody> getVideoXml(int videoId) throws IOException {
        return this.requests.getVideoFeed(videoId).execute();
    }

    public Response<ResponseBody> getVideoPage(int videoId) throws IOException {
        return this.requests.getVideoPage(videoId).execute();
    }

    public Response<ResponseBody> getVideoStream(String videoResource) throws IOException {
        return this.requests.getVideoStream(videoResource).execute();
    }

}
