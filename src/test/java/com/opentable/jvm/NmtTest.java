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

    @Test
    public void parseJava17() {
        final String s = "Native Memory Tracking:\n" +
                "(Omitting categories weighting less than 1KB)\n" +
                "Total: reserved=1193060KB, committed=1015360KB\n" +
                "-                 Java Heap (reserved=786432KB, committed=786432KB)\n" +
                "                            (mmap: reserved=786432KB, committed=786432KB) \n" +
                " \n" +
                "-                     Class (reserved=34053KB, committed=9925KB)\n" +
                "                            (classes #14336)\n" +
                "                            (  instance classes #13428, array classes #908)\n" +
                "                            (malloc=1285KB #30379) \n" +
                "                            (mmap: reserved=32768KB, committed=8640KB) \n" +
                "                            (  Metadata:   )\n" +
                "                            (    reserved=65536KB, committed=59392KB)\n" +
                "                            (    used=59071KB)\n" +
                "                            (    waste=321KB =0.54%)\n" +
                "                            (  Class space:)\n" +
                "                            (    reserved=32768KB, committed=8640KB)\n" +
                "                            (    used=8368KB)\n" +
                "                            (    waste=272KB =3.14%)\n" +
                " \n" +
                "-                    Thread (reserved=106144KB, committed=10384KB)\n" +
                "                            (thread #103)\n" +
                "                            (stack: reserved=105848KB, committed=10088KB)\n" +
                "                            (malloc=178KB #623) \n" +
                "                            (arena=119KB #204)\n" +
                " \n" +
                "-                      Code (reserved=67120KB, committed=15588KB)\n" +
                "                            (malloc=1072KB #7947) \n" +
                "                            (mmap: reserved=66048KB, committed=14516KB) \n" +
                " \n" +
                "-                        GC (reserved=68727KB, committed=68727KB)\n" +
                "                            (malloc=6671KB #8592) \n" +
                "                            (mmap: reserved=62056KB, committed=62056KB) \n" +
                " \n" +
                "-                  Compiler (reserved=3053KB, committed=3053KB)\n" +
                "                            (malloc=198KB #665) \n" +
                "                            (arena=2855KB #10)\n" +
                " \n" +
                "-                  Internal (reserved=604KB, committed=604KB)\n" +
                "                            (malloc=568KB #11678) \n" +
                "                            (mmap: reserved=36KB, committed=36KB) \n" +
                " \n" +
                "-                     Other (reserved=16612KB, committed=16612KB)\n" +
                "                            (malloc=16612KB #43) \n" +
                " \n" +
                "-                    Symbol (reserved=14236KB, committed=14236KB)\n" +
                "                            (malloc=12630KB #348127) \n" +
                "                            (arena=1606KB #1)\n" +
                " \n" +
                "-    Native Memory Tracking (reserved=6469KB, committed=6469KB)\n" +
                "                            (malloc=17KB #246) \n" +
                "                            (tracking overhead=6452KB)\n" +
                " \n" +
                "-        Shared class space (reserved=12288KB, committed=12152KB)\n" +
                "                            (mmap: reserved=12288KB, committed=12152KB) \n" +
                " \n" +
                "-               Arena Chunk (reserved=10985KB, committed=10985KB)\n" +
                "                            (malloc=10985KB) \n" +
                " \n" +
                "-                   Tracing (reserved=32KB, committed=32KB)\n" +
                "                            (arena=32KB #1)\n" +
                " \n" +
                "-                   Logging (reserved=6KB, committed=6KB)\n" +
                "                            (malloc=6KB #226) \n" +
                " \n" +
                "-                 Arguments (reserved=3KB, committed=3KB)\n" +
                "                            (malloc=3KB #101) \n" +
                " \n" +
                "-                    Module (reserved=407KB, committed=407KB)\n" +
                "                            (malloc=407KB #2491) \n" +
                " \n" +
                "-                 Safepoint (reserved=8KB, committed=8KB)\n" +
                "                            (mmap: reserved=8KB, committed=8KB) \n" +
                " \n" +
                "-           Synchronization (reserved=81KB, committed=81KB)\n" +
                "                            (malloc=81KB #929) \n" +
                " \n" +
                "-            Serviceability (reserved=1KB, committed=1KB)\n" +
                "                            (malloc=1KB #14) \n" +
                " \n" +
                "-                 Metaspace (reserved=65799KB, committed=59655KB)\n" +
                "                            (malloc=263KB #154) \n" +
                "                            (mmap: reserved=65536KB, committed=59392KB) \n" +
                " \n" +
                "-      String Deduplication (reserved=1KB, committed=1KB)\n" +
                "                            (malloc=1KB #8) \n" +
                " \n" +
                " ";
        final Nmt nmt = Nmt.parse(s);
        validate(nmt);
        Assert.assertEquals(nmt.categories.size(), 21);
        Assert.assertEquals(nmt.categories.get("Class").committed, 9925 * Nmt.K);
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
