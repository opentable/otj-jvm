package com.opentable.jvm;

import java.time.Duration;

/**
 * Sample program to run the NMT poller and shut it down.
 */
public class MemoryPollNmt {
    public static void main(String[] args) {
        final Duration aSec = Duration.ofSeconds(1);
        final Duration toSleep = Duration.ofSeconds(3);
        final Memory.NmtPollerController ctl = Memory.pollNmt(aSec);
        try {
            Thread.sleep(toSleep.toMillis());
        } catch (InterruptedException e) {
            throw new AssertionError("such interrupt");
        }
        try {
            ctl.shutdown(aSec);
        } catch (InterruptedException e) {
            throw new AssertionError("such interrupt");
        }
    }
}
