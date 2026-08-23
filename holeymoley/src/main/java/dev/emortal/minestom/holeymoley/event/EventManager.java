package dev.emortal.minestom.holeymoley.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class EventManager {

    private final Map<String, Class<? extends Event>> eventMap = new HashMap<>();

    public EventManager() {}

    public void registerEvent(String eventId, Class<? extends Event> event) {
        eventMap.put(eventId, event);
    }

    public Event createRandomEvent() {
        List<Class<? extends Event>> events = new ArrayList<>(eventMap.values());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Class<? extends Event> randomEventClass = events.get(random.nextInt(events.size()));
        try {
            return randomEventClass.getDeclaredConstructor().newInstance();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

}
