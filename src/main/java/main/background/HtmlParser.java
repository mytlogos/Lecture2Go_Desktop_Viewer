package main.background;

import main.Util;
import main.model.*;
import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class HtmlParser {

    public PageInfo parse(Response<ResponseBody> response) throws IOException {
        return parse(response.raw().request().url().toString(), Objects.requireNonNull(response.body()).string());
    }

    public PageInfo parse(String url, String html) {
        final Document document = Jsoup.parse(html, url);
        final Elements accordions = document.select(".accordion-group");

        if (accordions.size() > 4 || accordions.size() < 3) {
            throw new IllegalStateException("Lecture2Go Format changed");
        }

        List<Section> sections;

        if (accordions.size() == 4) {
            // if 4 accordions are there, then the second one is the section accordion
            // available only if a faculty is selected
            final Element element = accordions.remove(1);
            sections = this.processAccordion(element, Section::new, 1);
        } else {
            sections = Collections.emptyList();
        }

        List<Faculty> faculties = this.processAccordion(accordions.get(0), Faculty::new, 2);
        List<Semester> semesters = this.processAccordion(accordions.get(1), Semester::new, 5);
        List<Category> categories = this.processAccordion(accordions.get(2), Category::new, 3);
        List<Video> videos = this.parseVideoItems(document);

        return new PageInfo(faculties, sections, semesters, categories, videos);
    }

    private <T> List<T> processAccordion(Element element, BiFunction<Integer, String, T> generator, int linkPosition) {
        final Elements contents = element.select(".toggler-content li a");
        final Pattern pattern = Pattern.compile("https://lecture2go\\.uni-hamburg\\.de/l2go/-/get/(\\d+)/(\\d+)/(\\d+)/(\\d+)/(\\d+)");

        List<T> list = new ArrayList<>(contents.size());

        for (Element content : contents) {
            final String link = content.attr("abs:href");
            final Matcher matcher = pattern.matcher(link);

            if (!matcher.matches() || matcher.group(linkPosition) == null || matcher.group(linkPosition).isEmpty()) {
                continue;
            }
            String name = content.text();
            int id = Integer.parseInt(matcher.group(linkPosition));
            list.add(generator.apply(id, name));
        }
        return list;
    }

    private List<Video> parseVideoItems(Document document) {
        final Pattern pattern = Pattern.compile("https://lecture2go\\.uni-hamburg\\.de/l2go/-/get/[lv]/(\\d+)");
        final List<Video> videos = new ArrayList<>();
        final Elements videoRows = document.select("table tbody > tr > td > .videotile");

        for (Element row : videoRows) {
            final Element img = row.selectFirst(".video-image-wrapper > img");
            final String coverLink = img.attr("abs:src");
            final String term = row.selectFirst(".term-of-creation").text();

            final Element titleElement = row.selectFirst(".lectureseries-title a");
            final String title = titleElement.text();
            final String titleLink = titleElement.attr("abs:href");
            final Matcher matcher = pattern.matcher(titleLink);

            if (!matcher.matches() || matcher.group(1) == null) {
                throw new IllegalStateException("Lecture2Go Link Format changed");
            }
            int id = Integer.parseInt(matcher.group(1));

            List<String> creators = this.parseCreators(row);

            List<String> tags = new ArrayList<>();

            for (Element label : row.select(".video-content-footer .labels a")) {
                final String text = label.text();

                if (!text.isEmpty()) {
                    tags.add(text);
                }
            }

            final Element videoSubList = row.nextElementSibling();
            if (videoSubList == null) {
                videos.add(new Video(coverLink, id, title, creators, term, tags));
            } else {
                List<Video> subVideos = this.parseSubVideoList(videoSubList);
                videos.add(Video.createSeries(coverLink, id, title, creators, term, tags, subVideos));
            }

        }
        return videos;
    }

    private List<String> parseCreators(Element row) {
        final Elements creatorElements = row.select(".allcreators a");

        List<String> creators = new ArrayList<>(creatorElements.size());

        for (Element creator : creatorElements) {
            final String name = creator.text();

            if (!name.isEmpty()) {
                creators.add(name);
            }
        }
        return creators;
    }

    private List<Video> parseSubVideoList(Element subListElement) {
        final Pattern pattern = Pattern.compile("window\\.location='https://lecture2go\\.uni-hamburg.de/l2go/-/get/v/(\\d+)'");
        final Elements elements = subListElement.select("li.videotile");
        List<Video> videos = new ArrayList<>(elements.size());

        for (Element row : elements) {
            final String coverLink = row.selectFirst("img.video-image").attr("abs:src");
            final String date = row.selectFirst(".generation-date").text();
            final String title = row.selectFirst(".title-small").text();

            final Matcher matcher = pattern.matcher(row.attr("onclick"));

            if (!matcher.matches() || matcher.group(1) == null) {
                throw new IllegalStateException("Lecture2Go Link Format changed");
            }
            int id = Integer.parseInt(matcher.group(1));

            final List<String> creators = this.parseCreators(row);

            videos.add(new Video(coverLink, id, title, creators, Util.parseLocalDate(date), Collections.emptyList()));
        }
        return videos;
    }
}
