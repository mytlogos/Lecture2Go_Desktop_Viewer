package main.background.storage;

import main.model.Faculty;

/**
 *
 */
abstract class FacultyHelper extends QueryItemHelper<Faculty> {

    FacultyHelper() {
        super("faculty", null);
    }
}
