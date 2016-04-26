package com.opentable.jvm;

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
}
