package dev.brighten.ac.handler.keepalive.actions;

import dev.brighten.ac.utils.RunUtils;
import lombok.Getter;
import lombok.val;

import java.util.*;
import java.util.function.Consumer;

public class ActionManager {

    public ActionManager() {
        // Removing any unconfirmed actions.
        RunUtils.taskTimerAsync(task -> unconfirmedActions.removeIf(Action::confirmed), 40, 40);
    }
    private final Map<Class<? extends Action>, List<Consumer<Action>>> actionListeners = new HashMap<>();

    @Getter
    private final Set<Action> unconfirmedActions = new HashSet<>();

    public void listenForConfirmation(Class<? extends Action> action, Consumer<Action> listener) {
        synchronized (actionListeners) {
            actionListeners.compute(action, (key, list) -> {
                if(list == null) {
                    list = new ArrayList<>();
                }

                list.add(listener);

                return list;
            });
        }
    }

    public void confirmedAction(Action action) {
        unconfirmedActions.remove(action);

        val list = actionListeners.get(action.getClass());

        if(list != null) {
            for (Consumer<Action> actionConsumer : list) {
                actionConsumer.accept(action);
            }
        }
    }

    public void addAction(Action action) {
        if(!action.confirmed())
            unconfirmedActions.add(action);
    }

    
}
