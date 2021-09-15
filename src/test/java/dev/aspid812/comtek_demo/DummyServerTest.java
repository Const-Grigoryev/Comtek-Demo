package dev.aspid812.comtek_demo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DummyServerTest {
    final DummyServer<String> subject;

    final long processingDelay;
    final long sendingDelay;

    private static Properties loadProperties(String fileName) throws IOException {
        try (InputStream input = ClassLoader.getSystemResourceAsStream(fileName)) {
            Properties prop = new Properties();
            prop.load(input);
            return prop;
        }
    }

    public DummyServerTest() throws IOException {
        Properties prop = loadProperties("dummyserver.properties");
        processingDelay = Long.parseLong(prop.getProperty("processingDelay", "0"));
        sendingDelay = Long.parseLong(prop.getProperty("sendingDelay", "0"));
        subject = new DummyServer<>(processingDelay, sendingDelay);
    }


    private static class TimingListener implements AbstractServer.StateListener {
        final AtomicLong waitingStartTime = new AtomicLong(0);
        final AtomicLong processingStartTime = new AtomicLong(0);
        final AtomicLong sendingStartTime = new AtomicLong(0);

        @Override
        public void onWaiting() {
            waitingStartTime.set(System.nanoTime());
        }

        @Override
        public void onProcessing() {
            processingStartTime.set(System.nanoTime());
        }

        @Override
        public void onSending() {
            sendingStartTime.set(System.nanoTime());
        }
    }


    private static class CountDownListener implements AbstractServer.StateListener {
        final CountDownLatch waitingLatch;
        final CountDownLatch processingLatch;
        final CountDownLatch sendingLatch;

        public CountDownListener(int waitingCount, int processingCount, int sendingCount) {
            waitingLatch = new CountDownLatch(waitingCount);
            processingLatch = new CountDownLatch(processingCount);
            sendingLatch = new CountDownLatch(sendingCount);
        }

        @Override
        public void onWaiting() {
            waitingLatch.countDown();
        }

        @Override
        public void onProcessing() {
            processingLatch.countDown();
        }

        @Override
        public void onSending() {
            sendingLatch.countDown();
        }
    }


    Thread workerThread;

    @Before
    public void setup() throws InterruptedException {
        CountDownListener sync = new CountDownListener(1, 0, 0);
        subject.addStateListener(sync);

        workerThread = new Thread(subject, "DummyServer-Thread");
        workerThread.start();

        sync.waitingLatch.await();
        subject.removeStateListener(sync);
    }

    @After
    public void teardown() {
        workerThread.interrupt();
    }


    @Test
    public void freshly_created_server_is_ready_to_accept_data() throws InterruptedException {
        boolean accepted = subject.submitData("Request 0", 10, TimeUnit.MILLISECONDS);
        assertTrue(accepted);
    }

    @Test(timeout = 5000)
    public void implementation_methods_maintain_desired_timing() throws InterruptedException {
        TimingListener timer = new TimingListener();
        subject.addStateListener(timer);

        CountDownListener sync = new CountDownListener(1, 0, 0);
        subject.addStateListener(sync);

        subject.submitData("Request 0", 10, TimeUnit.MILLISECONDS);
        sync.waitingLatch.await();

        long processingNanoDelay = TimeUnit.MILLISECONDS.toNanos(processingDelay);
        long processingNanoTime = timer.sendingStartTime.get() - timer.processingStartTime.get();
        assertTrue("processingNanoTime >= processingNanoDelay", processingNanoTime >= processingNanoDelay);

        long sendingNanoDelay = TimeUnit.MILLISECONDS.toNanos(processingDelay);
        long sendingNanoTime = timer.waitingStartTime.get() - timer.sendingStartTime.get();
        assertTrue("sendingNanoTime >= sendingNanoDelay", sendingNanoTime >= sendingNanoDelay);
    }

    @Test(timeout = 5000)
    public void server_state_changes_in_proper_order() throws InterruptedException {
        AbstractServer.StateListener listener = mock(AbstractServer.StateListener.class);
        subject.addStateListener(listener);

        CountDownListener sync = new CountDownListener(1, 0, 0);
        subject.addStateListener(sync);

        InOrder order = inOrder(listener);

        subject.submitData("Request 0", 10, TimeUnit.MILLISECONDS);
        sync.waitingLatch.await();

        order.verify(listener).onProcessing();
        order.verify(listener).onSending();
        order.verify(listener).onWaiting();
    }

    @Test(timeout = 5000)
    public void server_in_processing_state_refuses_new_requests() throws InterruptedException {
        CountDownListener sync = new CountDownListener(0, 1, 1);
        subject.addStateListener(sync);

        subject.submitData("Request 0", 10, TimeUnit.MILLISECONDS);
        sync.processingLatch.await();
        boolean accepted = subject.submitData("Request 1", 10, TimeUnit.MILLISECONDS);
        assertEquals("The server has changed its state within testing process. Hint: increase processing delay",
                1, sync.sendingLatch.getCount());

        assertFalse(accepted);
    }

    @Test(timeout = 5000)
    public void server_in_sending_state_refuses_new_requests() throws InterruptedException {
        CountDownListener sync = new CountDownListener(1, 0, 1);
        subject.addStateListener(sync);

        subject.submitData("Request 0", 10, TimeUnit.MILLISECONDS);
        sync.sendingLatch.await();
        boolean accepted = subject.submitData("Request 1", 10, TimeUnit.MILLISECONDS);
        assertEquals("The server has changed its state within testing process. Hint: increase sending delay",
                1, sync.waitingLatch.getCount());

        assertFalse(accepted);
    }

    @Test(timeout = 5000)
    public void server_in_waiting_state_accepts_new_requests() throws InterruptedException {
        CountDownListener sync = new CountDownListener(1, 2, 0);
        subject.addStateListener(sync);

        subject.submitData("Request 0", 10, TimeUnit.MILLISECONDS);
        sync.waitingLatch.await();
        boolean accepted = subject.submitData("Request 1", 10, TimeUnit.MILLISECONDS);

        assertTrue(accepted);
    }
}
