package com.opentable.jvm;

import java.time.Duration;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class MemoryTest {
    @Test
    public void formatNmt() {
        final String out = Memory.formatNmt();
        Assert.assertNotNull(out);
        System.out.print(out);
    }

    @Ignore
    @Test
    public void dumpHeap() {
        Memory.dumpHeap();
    }

    // Not parallel-safe.
    @Test
    public void dumpHeapTmpDirDefault() {
        final String propName = "java.io.tmpdir";
        final String old = System.clearProperty(propName);
        if (old == null) {
            throw new AssertionError("we were going to be the one to kill this");
        }
        Assert.assertEquals(Memory.getTmpDir().toString(), Memory.DEFAULT_TMP_PATH);
        System.setProperty(propName, old);
    }

    // Not parallel-safe.  Probably not worth it to do DI.
    @Test
    public void getHeapDumpDirMesos() {
        final String testValue = "/bing/bing/bam/bam";
        final Function<String, String> old = Memory.getenv;
        Memory.getenv = name -> {
            if (name.equals("MESOS_SANDBOX")) {
                return testValue;
            }
            throw new AssertionError("unexpected environment inspection");
        };
        Assert.assertEquals(Memory.getHeapDumpDir().toString(), testValue);
        Memory.getenv = old;
    }

    @Ignore
    @Test
    public void nmtPoller() {
        final Duration aSec = Duration.ofSeconds(1);
        final Duration toSleep = Duration.ofSeconds(3);
        final Memory.NmtPoller poller = new Memory.NmtPoller(aSec);
        try {
            Thread.sleep(toSleep.toMillis());
        } catch (InterruptedException e) {
            poller.shutdown(aSec);
        }
        poller.shutdown(aSec);
    }
}
