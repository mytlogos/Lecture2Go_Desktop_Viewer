package main.background.storage;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 *
 */
public class Register {
    public static final Logger LOGGER;

    static {
        LOGGER = Logger.getGlobal();
        try {
            LOGGER.addHandler(new FileHandler(System.getProperty("user.dir") + System.getProperty("file.separator") + "l2go.log"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
