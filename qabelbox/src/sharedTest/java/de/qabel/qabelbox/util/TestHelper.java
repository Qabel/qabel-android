package de.qabel.qabelbox.util;

import java.util.concurrent.Callable;

public class TestHelper {
    public static void waitUntil(Callable<Boolean> requirement, String message) throws Exception {
        long maximumTime = System.currentTimeMillis() + 10000L;
        long pollInterval = 100L;
        while (System.currentTimeMillis() < maximumTime) {
            if (requirement.call()) {
                return;
            }
            Thread.sleep(pollInterval);
        }
        fail(message);
    }

    public static void fail(String message) {
        throw new AssertionError(message);
    }
}
