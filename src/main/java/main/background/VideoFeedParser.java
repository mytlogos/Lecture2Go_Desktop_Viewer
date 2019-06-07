package main.background;

import okhttp3.ResponseBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
class VideoFeedParser {
    List<VideoMeta> parse(Response<ResponseBody> response) {
        if (response.body() == null) {
            return null;
        }
        final String string;
        try {
            string = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        List<VideoMeta> metas = new ArrayList<>();
        final Document document = Jsoup.parse(string, response.raw().request().url().toString(), Parser.xmlParser());
        final Pattern durationPattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
        final Pattern guidPattern = Pattern.compile("https://lecture2go\\.uni-hamburg\\.de/l2go/-/get/v/(\\d+)");

        for (Element item : document.getElementsByTag("item")) {
            final Element guid = item.getElementsByTag("guid").get(0);
            final Elements elements = item.getElementsByTag("itunes:duration");

            long duration = 0;

            if (!elements.isEmpty()) {
                final Element durationElement = elements.get(0);
                final Matcher matcher = durationPattern.matcher(durationElement.text());

                if (matcher.matches()) {
                    final int hours = Integer.parseInt(matcher.group(1));
                    final int minutes = Integer.parseInt(matcher.group(2));
                    final int seconds = Integer.parseInt(matcher.group(3));
                    duration = (hours * 60L * 60L) + (minutes * 60L) + seconds;
                }
            }
            final Matcher guidMatcher = guidPattern.matcher(guid.text());
            int videoId = 0;

            if (guidMatcher.matches()) {
                videoId = Integer.parseInt(guidMatcher.group(1));
            } else {
                System.out.println("guid link format changed: could not extract video id");
                continue;
            }
            final Elements enclosure = item.getElementsByTag("enclosure");

            final long lengthInBytes;
            final String url;

            if (!enclosure.isEmpty()) {
                final Element mediaElement = enclosure.get(0);

                final String type = mediaElement.attr("type");
                if (!type.equals("video/mp4")) {
                    System.out.println("an item with an media that is not a video: " + type);
                    continue;
                }

                lengthInBytes = Long.parseLong(mediaElement.attr("length"));
                url = mediaElement.attr("url");
            } else {
                lengthInBytes = 0;
                url = "";
            }

            metas.add(new VideoMeta(duration, url, lengthInBytes, videoId));
        }
        return metas;
    }
}
