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

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

public class MemoryTest {
    @Test
    public void formatNmt() {
        final String out = Memory.formatNmt();
        Assert.assertNotNull(out);
        System.out.print(out);
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

    @Test
    public void formatBytes1() {
        Assert.assertEquals(Memory.formatBytes(1024), "1.00 KiB");
    }

    @Test
    public void formatBytes2() {
        Assert.assertEquals(Memory.formatBytes(20), "20 B");
    }

    @Test
    public void formatBytes3() {
        Assert.assertEquals(Memory.formatBytes(3 * 1024 * 1024), "3.00 MiB");
    }
}
