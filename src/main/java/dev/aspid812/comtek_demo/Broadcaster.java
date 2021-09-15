package dev.aspid812.comtek_demo;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public class Broadcaster<L> {
    private final Collection<L> listeners = new ConcurrentLinkedDeque<>();

    public void addListener(L listener) {
        listeners.add(listener);
    }

    public void removeListener(L listener) {
        listeners.remove(listener);
    }

    public void broadcast(Consumer<? super L> event) {
        for (L listener : listeners) {
            event.accept(listener);
        }
    }
}
