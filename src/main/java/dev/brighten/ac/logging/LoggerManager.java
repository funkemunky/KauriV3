package dev.brighten.ac.logging;

import dev.brighten.ac.logging.sql.MySQL;


public class LoggerManager {

    /*
     * Structure of Log
     * UUID hashcode (INT32),
     */
    public void init() {
        // Starting up H2
        MySQL.initH2();
    }

    public void shutDown() {
        MySQL.shutdown();
    }
}
