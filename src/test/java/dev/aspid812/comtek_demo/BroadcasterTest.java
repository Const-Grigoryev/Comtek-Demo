package dev.aspid812.comtek_demo;

import org.junit.Before;
import org.junit.Test;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class BroadcasterTest {
    interface Listener {
        void onEvent();
    }

    final Broadcaster<Listener> subject = new Broadcaster<>();

    final Listener listener1 = mock(Listener.class);
    final Listener listener2 = mock(Listener.class);

    @Before
    public void setup() {
        subject.addListener(listener1);
        subject.addListener(listener2);
    }

    @Test
    public void added_listeners_receive_notifications() {
        subject.broadcast(Listener::onEvent);

        verify(listener1).onEvent();
        verify(listener2).onEvent();
    }

    @Test
    public void remover_listeners_dont_receive_notifications() {
        subject.removeListener(listener1);

        subject.broadcast(Listener::onEvent);

        verify(listener1, never()).onEvent();
        verify(listener2).onEvent();
    }
}
