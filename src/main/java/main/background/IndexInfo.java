package main.background;

import java.time.LocalDateTime;

/**
 *
 */
public class IndexInfo {
    private final int page;
    private final int limit;
    private final LocalDateTime lastIndexed;
    private final int itemCount;
    private final String titleHash;


    public IndexInfo(int page, int limit, LocalDateTime lastIndexed, int itemCount, String titleHash) {
        this.page = page;
        this.limit = limit;
        this.lastIndexed = lastIndexed;
        this.itemCount = itemCount;
        this.titleHash = titleHash;
    }

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }

    public LocalDateTime getLastIndexed() {
        return lastIndexed;
    }

    public int getItemCount() {
        return itemCount;
    }

    public String getTitleHash() {
        return titleHash;
    }
}
