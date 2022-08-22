package dev.brighten.ac.check;

import dev.brighten.ac.Anticheat;
import dev.brighten.ac.utils.ClassScanner;
import dev.brighten.ac.utils.Tuple;
import dev.brighten.ac.utils.reflections.types.WrappedClass;
import dev.brighten.ac.utils.reflections.types.WrappedMethod;
import lombok.Getter;

import java.util.*;

@Getter
public class CheckManager {
    private final List<CheckStatic> checkClasses = new ArrayList<>();
    private final Map<Tuple<String, Class<?>>, WrappedMethod[]> events = new HashMap<>();
    private final Map<Tuple<String, Class<?>>, WrappedMethod[]> eventsWithTimestamp = new HashMap<>();

    public CheckManager() {
        for (WrappedClass aClass : ClassScanner.getClasses(CheckData.class,
                "dev.brighten.ac.check.impl")) {
            addCheck(aClass);
        }
    }

    public void addCheck(WrappedClass checkClass) {
        CheckStatic check = new CheckStatic(checkClass);

        if(!check.getCheckClass().isAnnotationPresent(CheckData.class)) {
            return;
        }

        CheckData checkData = check.getCheckClass().getAnnotation(CheckData.class);

        Anticheat.INSTANCE.alog(true, "&7Adding check to CheckManager: " + checkData.name());

        synchronized (events) {
            // Loop through all the methods in Check that contain @Action annotation by Class
            check.getEvents().forEach((actionClass, methods) -> {
                // Check if array is already cached for Class and return Array if so, if not create new Array
                /*events.compute(new Tuple<String, Class<?>>(checkData.name(), actionClass), (packetClass, array) -> {
                    if(array == null) array = new WrappedMethod[0];

                    // Adding preexisting cached WrappedMethod into List for further additions
                    List<WrappedMethod> methodList = new ArrayList<>(Arrays.asList(array));

                    // Adding all precached Check-specific WrappedMethod into global cache of WrappedMethods.
                    methodList.addAll(methods);

                    System.out.println("Registering " + packetClass.toString());

                    // Returning newly created array for use in detections.
                    return methodList.toArray(new WrappedMethod[0]);
                });*/
                for (WrappedMethod method : methods) {
                    if(method.getParameters().length == 1) {
                        events.compute(new Tuple<>(checkData.name(), actionClass), (packetClass, array) -> {
                            if(array == null) array = new WrappedMethod[0];

                            // Adding preexisting cached WrappedMethod into List for further additions
                            List<WrappedMethod> methodList = new ArrayList<>(Arrays.asList(array));

                            methodList.add(method);

                            // Adding all precached Check-specific WrappedMethod into global cache of WrappedMethods.

                            System.out.println("Registering regualr method:  " + packetClass.toString());

                            // Returning newly created array for use in detections.
                            return methodList.toArray(new WrappedMethod[0]);
                        });
                    } else if(method.getParameters().length == 2 && method.getParameterTypes()[1].equals(long.class)) {
                        eventsWithTimestamp.compute(new Tuple<>(checkData.name(), actionClass), (packetClass, array) -> {
                            if(array == null) array = new WrappedMethod[0];

                            // Adding preexisting cached WrappedMethod into List for further additions
                            List<WrappedMethod> methodList = new ArrayList<>(Arrays.asList(array));

                            methodList.add(method);

                            // Adding all precached Check-specific WrappedMethod into global cache of WrappedMethods.

                            System.out.println("Registering regualr method:  " + packetClass.toString());

                            // Returning newly created array for use in detections.
                            return methodList.toArray(new WrappedMethod[0]);
                        });
                    }
                }
            });
        }

        checkClasses.add(check);
    }

    public boolean isCheck(String name) {
        final String formattedName = name.replace("_", " ");
        return checkClasses.stream().anyMatch(c -> c.getCheckClass().getAnnotation(CheckData.class).name()
                .equalsIgnoreCase(formattedName));
    }
}
