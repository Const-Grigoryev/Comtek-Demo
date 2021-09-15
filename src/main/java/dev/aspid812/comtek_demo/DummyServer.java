package dev.aspid812.comtek_demo;

import java.util.concurrent.TimeUnit;

public class DummyServer<D> extends AbstractServer<D> {
    private final long processingDelay;
    private final long sendingDelay;

    public DummyServer(long processingDelay, long sendingDelay) {
        this.processingDelay = processingDelay;
        this.sendingDelay = sendingDelay;
    }

    private static void forceSleep(long duration, TimeUnit unit) {
        long wakeup = System.nanoTime() + TimeUnit.NANOSECONDS.convert(duration, unit);
        boolean enough = false;
        boolean interrupted = false;

        while (!enough) {
            try {
                TimeUnit.NANOSECONDS.sleep(wakeup - System.nanoTime());
                enough = true;
            }
            catch (InterruptedException ex) {
                interrupted = true;
            }
        }

        if (interrupted) {
            // Restore interruption flag
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected Void doProcessing(D data) {
        forceSleep(processingDelay, TimeUnit.MILLISECONDS);
        return null;
    }

    @Override
    protected Void doSending() {
        forceSleep(sendingDelay, TimeUnit.MILLISECONDS);
        return null;
    }
}
