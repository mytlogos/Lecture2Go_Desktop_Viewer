package main.background;

import main.model.*;

import java.util.List;

/**
 *
 */
public class PageInfo {
    private List<Faculty> faculties;
    private List<Section> sections;
    private List<Semester> semesters;
    private List<Category> categories;
    private List<Video> videos;


    public PageInfo(List<Faculty> faculties, List<Section> sections, List<Semester> semesters, List<Category> categories, List<Video> videos) {
        this.faculties = faculties;
        this.sections = sections;
        this.semesters = semesters;
        this.categories = categories;
        this.videos = videos;
    }

    public List<Faculty> getFaculties() {
        return this.faculties;
    }

    public List<Section> getSections() {
        return this.sections;
    }

    public List<Semester> getSemesters() {
        return this.semesters;
    }

    public List<Category> getCategories() {
        return this.categories;
    }

    public List<Video> getVideos() {
        return this.videos;
    }
}
