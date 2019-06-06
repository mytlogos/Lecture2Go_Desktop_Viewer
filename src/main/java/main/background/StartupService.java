package main.background;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import main.GodData;
import main.background.storage.Storage;
import main.model.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class StartupService extends Service<Void> {
    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
                this.updateTitle("Loading from Database");

                Storage storage = Storage.getDatabase();

                final List<Category> categories = storage.getCategoryHelper().getAll();
                GodData.get().addCategory(categories);
                this.updateMessage(String.format("Loaded %d Categories", categories.size()));

                final List<Faculty> faculties = storage.getFacultyHelper().getAll();
                GodData.get().addFaculty(faculties);
                this.updateMessage(String.format("Loaded %d Faculties", faculties.size()));

                final List<Section> sections = faculties.stream().flatMap(faculty -> faculty.getSections().stream()).collect(Collectors.toList());
                GodData.get().addSection(sections);
                this.updateMessage(String.format("Loaded %d Sections", sections.size()));

                final List<Semester> semesters = storage.getSemesterHelper().getAll();
                GodData.get().addSemester(semesters);
                this.updateMessage(String.format("Loaded %d Semesters", semesters.size()));

                final List<Video> videos = storage.getVideos();
                GodData.get().addVideo(videos);
                this.updateMessage(String.format("Loaded %d Videos", videos.size()));

                this.updateTitle("Finished Loading from Database");
                return null;
            }
        };
    }
}
