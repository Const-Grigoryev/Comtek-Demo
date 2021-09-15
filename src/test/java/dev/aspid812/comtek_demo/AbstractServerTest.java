package dev.aspid812.comtek_demo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class AbstractServerTest {
    final AbstractServer<String> subject = new AbstractServer<>() {
        @Override
        protected Message doWaiting() {
            return SHUTDOWN;
        }

        @Override
        protected Void doProcessing(String data) {
            return null;
        }

        @Override
        protected Void doSending() {
            return null;
        }
    };

    final AbstractServer.StateListener listener = mock(AbstractServer.StateListener.class);

    @Before
    public void setup() {
        subject.addStateListener(listener);
    }


    @Test
    public void enter_waiting_state_triggers_notification() {
        subject.waiting(() -> null);

        verify(listener, only()).onWaiting();
        verify(listener, never()).onProcessing();
        verify(listener, never()).onSending();
    }

    @Test
    public void enter_processing_state_triggers_notification() {
        subject.processing(() -> null);

        verify(listener, never()).onWaiting();
        verify(listener, only()).onProcessing();
        verify(listener, never()).onSending();
    }

    @Test
    public void enter_sending_state_triggers_notification() {
        subject.sending(() -> null);

        verify(listener, never()).onWaiting();
        verify(listener, never()).onProcessing();
        verify(listener, only()).onSending();
    }

    @Test
    public void server_makes_processing_notification_before_doing_actual_processing_work() {
        AbstractServer<String> subjectSpy = spy(subject);

        InOrder order = inOrder(subjectSpy, listener);

        subject.processing(() -> subjectSpy.doProcessing("Hello"));

        order.verify(listener).onProcessing();
        order.verify(subjectSpy).doProcessing("Hello");
    }

    @Test
    public void server_makes_sending_notification_before_doing_actual_sending_work() {
        AbstractServer<String> subjectSpy = spy(subject);

        InOrder order = inOrder(subjectSpy, listener);

        subject.processing(subjectSpy::doSending);

        order.verify(listener).onSending();
        order.verify(subjectSpy).doSending();
    }
}
