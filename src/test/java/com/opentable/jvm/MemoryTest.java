package com.opentable.jvm;

import org.junit.Assert;
import org.junit.Test;

public class MemoryTest {
    @Test
    public void nmt() {
        final String out = Memory.formatNmt();
        Assert.assertNotNull(out);
        System.out.print(out);
    }
}
