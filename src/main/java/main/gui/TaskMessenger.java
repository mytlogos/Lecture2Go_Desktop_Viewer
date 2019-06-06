package main.gui;

import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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

    TaskMessenger(StringProperty messageProperty, DoubleProperty doubleProperty) {
        this.messageProperty = messageProperty;
        this.doubleProperty = doubleProperty;
    }

    void registerWorker(Worker<?> worker) {
        if (this.workers.add(worker)) {
            final InvalidationListener listener = observable -> this.processMessage(worker);

            this.doubleProperty.setValue(worker.getProgress());
            worker.progressProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    TaskMessenger.this.doubleProperty.setValue(newValue);
                    TaskMessenger.this.processMessage(worker);
                    TaskMessenger.this.workers.remove(worker);

                    worker.progressProperty().removeListener(this);
                    worker.totalWorkProperty().removeListener(listener);
                    worker.workDoneProperty().removeListener(listener);
                    worker.titleProperty().removeListener(listener);
                    worker.messageProperty().removeListener(listener);
                }
            });

            worker.totalWorkProperty().addListener(listener);
            worker.workDoneProperty().addListener(listener);
            worker.titleProperty().addListener(listener);
            worker.messageProperty().addListener(listener);
        }
    }

    private void processMessage(Worker<?> worker) {
        String format = "Tasks: " + this.workers.size();

        if (!worker.getTitle().isEmpty()) {
            format += " - " + worker.getTitle();
        }

        if (!worker.getMessage().isEmpty()) {
            format += " - " + worker.getMessage();
        }

        if (worker.getWorkDone() >= 0) {
            format += " - " + worker.getWorkDone();

            if (worker.getTotalWork() < 0) {
                format += " of ?";
            } else if (worker.getTotalWork() == worker.getWorkDone()) {
                format += " of " + worker.getTotalWork();
            }
        }

        messageProperty.setValue(format);
    }

}
