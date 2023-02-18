package dev.brighten.ac.handler.keepalive.actions;

public interface Action {

    void run();

    boolean confirmed();

    ActionType type();

}
