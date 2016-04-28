package com.opentable.jvm;

import java.time.Duration;

/**
 * Sample program to run the NMT poller and shut it down.
 */
public class MemoryPollNmt {
    public static void main(String[] args) throws InterruptedException {
        final Duration aSec = Duration.ofSeconds(1);
        final Duration toSleep = Duration.ofSeconds(3);
        try (final Memory.NmtCloseable c = Memory.pollNmt(aSec)) {
            Thread.sleep(toSleep.toMillis());
        }
    }
}
