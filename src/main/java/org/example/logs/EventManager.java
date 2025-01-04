package org.example.logs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventManager {
    Map<String, List<Subscriber>> listeners = new HashMap<>();

    public EventManager(String... operations) {
        for (String operation : operations) {
            this.listeners.put(operation, new ArrayList<>());
        }
    }

    public void subscribe(Subscriber subscriber, String event) {
        List<Subscriber> users = listeners.get(event);
        users.add(subscriber);
    }

    public void unSubscribe(Subscriber subscriber, String event) {
        List<Subscriber> users = listeners.get(event);
        users.remove(subscriber);
    }

    public void notifySubscribers(String event) {
        List<Subscriber> users = listeners.get(event);
        for (Subscriber listener : users) {
            listener.update();
        }
    }
}
