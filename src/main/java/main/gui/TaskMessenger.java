package main.gui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
class TaskMessenger {

    private final StringProperty messageProperty;
    private final DoubleProperty doubleProperty;
    private final Set<Worker<?>> workers = new HashSet<>();
    private int count = 0;

    TaskMessenger(StringProperty messageProperty, DoubleProperty doubleProperty) {
        this.messageProperty = messageProperty;
        this.doubleProperty = doubleProperty;
    }

    void registerWorker(Worker<?> worker) {
        count++;
        if (this.workers.add(worker)) {
            worker.progressProperty().addListener((observable, oldValue, newValue) -> {
                this.doubleProperty.setValue(newValue);
                this.processMessage(worker);
            });
            worker.titleProperty().addListener(observable -> this.processMessage(worker));
            worker.messageProperty().addListener(observable -> this.processMessage(worker));
        }
    }

    private void processMessage(Worker<?> worker) {
        if (worker.getTotalWork() < 0) {
            messageProperty.setValue(String.format("Tasks: %d - %s - %s - %f of ?", workers.size(), worker.getTitle(), worker.getMessage(), worker.getWorkDone()));
        } else if (worker.getTotalWork() == worker.getWorkDone()) {
            messageProperty.setValue(String.format("Tasks: %d - %s - %f of %f", workers.size(), worker.getTitle(), worker.getWorkDone(), worker.getTotalWork()));
        } else {
            messageProperty.setValue(String.format("Tasks: %d - %s - %s - %f of %f", workers.size(), worker.getTitle(), worker.getMessage(), worker.getWorkDone(), worker.getTotalWork()));
        }
    }

}
