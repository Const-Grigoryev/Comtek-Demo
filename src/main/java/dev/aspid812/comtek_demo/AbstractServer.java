package dev.aspid812.comtek_demo;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class AbstractServer<D> implements Runnable {
    public interface StateListener {
        void onWaiting();
        void onProcessing();
        void onSending();
    }

    private final Broadcaster<StateListener> stateBroadcaster = new Broadcaster<>();

    public void addStateListener(StateListener listener) {
        stateBroadcaster.addListener(listener);
    }

    public void removeStateListener(StateListener listener) {
        stateBroadcaster.removeListener(listener);
    }


    <R> R waiting(Supplier<? extends R> action) {
        stateBroadcaster.broadcast(StateListener::onWaiting);
        return action.get();
    }

    <R> R processing(Supplier<? extends R> action) {
        stateBroadcaster.broadcast(StateListener::onProcessing);
        return action.get();
    }

    <R> R sending(Supplier<? extends R> action) {
        stateBroadcaster.broadcast(StateListener::onSending);
        return action.get();
    }


    class Message {
        private final D payload;

        public Message(D payload) {
            this.payload = payload;
        }

        public D getPayload() {
            return payload;
        }
    }

    final Message SHUTDOWN = new Message(null);

    private final BlockingQueue<Message> incomingMessages = new SynchronousQueue<>();

    public boolean submitData(D data, long timeout, TimeUnit unit) throws InterruptedException {
        return incomingMessages.offer(new Message(data), timeout, unit);
    }

    public boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException {
        return incomingMessages.offer(SHUTDOWN, timeout, unit);
    }


    Message doWaiting() {
        try {
            return incomingMessages.take();
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return SHUTDOWN;
        }
    }

    protected abstract Void doProcessing(D data);

    protected abstract Void doSending();

    @Override
    public void run() {
        boolean shutdown = false;
        while (!shutdown) {
            Message msg = waiting(this::doWaiting);

            if (msg == SHUTDOWN) {
                shutdown = true;
            } else {
                processing(() -> {
                    D data = msg.getPayload();
                    return this.doProcessing(data);
                });
                sending(this::doSending);
            }
        }
    }
}
