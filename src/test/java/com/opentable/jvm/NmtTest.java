/*
 * Copyright (c) 2016 OpenTable, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.opentable.jvm;

import org.junit.Assert;
import org.junit.Test;

public class NmtTest {
    @Test
    public void parseUsage1() {
        final Nmt.Usage u = Nmt.parseUsage("reserved=76944KB, committed=48784KB");
        Assert.assertNotNull(u);
        Assert.assertEquals(u.reserved, 76944 * Nmt.K);
        Assert.assertEquals(u.committed, 48784 * Nmt.K);
    }

    @Test
    public void parseUsage2() {
        final Nmt.Usage u = Nmt.parseUsage("reserved=69124KB, committed=26356KB)\n");
        Assert.assertNotNull(u);
        Assert.assertEquals(u.reserved, 69124 * Nmt.K);
        Assert.assertEquals(u.committed, 26356 * Nmt.K);
    }

    @Test
    public void parseUsage3() {
        final Nmt.Usage u = Nmt.parseUsage("reserved=69124KB, committed=26356KB)");
        Assert.assertNotNull(u);
        Assert.assertEquals(u.reserved, 69124 * Nmt.K);
        Assert.assertEquals(u.committed, 26356 * Nmt.K);
    }

    @Test
    public void parse() {
        final String s = "\n" +
                "Native Memory Tracking:\n" +
                "\n" +
                "Total: reserved=5710704KB, committed=471520KB\n" +
                "-                 Java Heap (reserved=4194304KB, committed=262144KB)\n" +
                "                            (mmap: reserved=4194304KB, committed=262144KB) \n" +
                " \n" +
                "-                     Class (reserved=1066181KB, committed=18885KB)\n" +
                "                            (classes #1722)\n" +
                "                            (malloc=9413KB #951) \n" +
                "                            (mmap: reserved=1056768KB, committed=9472KB) \n" +
                " \n" +
                "-                    Thread (reserved=20756KB, committed=20756KB)\n" +
                "                            (thread #20)\n" +
                "                            (stack: reserved=20480KB, committed=20480KB)\n" +
                "                            (malloc=60KB #110) \n" +
                "                            (arena=215KB #40)\n";
        final Nmt nmt = Nmt.parse(s);
        validate(nmt);
        Assert.assertEquals(nmt.categories.size(), 3);
        Assert.assertEquals(nmt.categories.get("Class").committed, 18885 * Nmt.K);
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseTooFewLines() {
        Nmt.parse("a\nb");
    }

    @Test
    public void get() {
        validate(Nmt.get());
    }

    private void validate(final Nmt nmt) {
        Assert.assertNotNull(nmt);
        Assert.assertNotNull(nmt.total);
        Assert.assertNotNull(nmt.categories);
        Assert.assertTrue(nmt.categories.containsKey("Java Heap"));
        Assert.assertNotNull(nmt.categories.get("Java Heap"));
        Assert.assertTrue(nmt.categories.containsKey("Class"));
        Assert.assertNotNull(nmt.categories.get("Class"));
    }
}
