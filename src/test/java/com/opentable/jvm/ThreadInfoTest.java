package com.opentable.jvm;

import org.junit.Assert;
import org.junit.Test;

public class ThreadInfoTest {
    @Test
    public void format() {
        final String out = ThreadInfo.format();
        Assert.assertNotNull(out);
        System.out.print(out);
    }
}
