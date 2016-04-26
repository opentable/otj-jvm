package com.opentable.jvm;

import javax.annotation.Nullable;

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

    // Not parallel-safe.  Probably not worth it to do DI.
    @Test
    public void getHeapDumpDirMesos() {
        final String testValue = "/bing/bing/bam/bam";
        final Memory.EnvironmentProvider old = Memory.environmentProvider;
        Memory.environmentProvider = new Memory.EnvironmentProvider() {
            @Nullable
            @Override
            public String getenv(String name) {
                if (name.equals("MESOS_SANDBOX")) {
                    return testValue;
                }
                throw new AssertionError("unexpected environment inspection");
            }
        };
        Assert.assertEquals(Memory.getHeapDumpDir().toString(), testValue);
        Memory.environmentProvider = old;
    }
}
