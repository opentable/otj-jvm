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

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sun.management.HotSpotDiagnosticMXBean;

import com.google.common.annotations.VisibleForTesting;
import com.mogwee.executors.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memory usage inspection routines.
 */
public class Memory {
    private static final Logger LOG = LoggerFactory.getLogger(Memory.class);

    @VisibleForTesting
    static final String DEFAULT_TMP_PATH = "/tmp";

    // Replaceable reference for testing.
    @SuppressWarnings({"PMD.MutableStaticState"})
    @VisibleForTesting
    static Function<String, String> getenv = System::getenv;

    private Memory() {}

    /**
     * Dumps heap with a name with a human-readable timestamp to the Mesos sandbox, if environment variable
     * {@code MESOS_SANDBOX} is defined, or to the system temporary directory otherwise.
     * Logs where the heap dump will be written.
     * Logs a warning if there was a problem preventing the heap dump from being successfully created.
     */
    public static void dumpHeap() {
        dumpHeap(getHeapDumpPath());
    }

    /**
     * Dumps heap to the specified path.
     * Logs where the heap dump will be written.
     * Logs a warning if there was a problem preventing the heap dump from being successfully created.
     * @param path Where to put the heap dump.
     */
    public static void dumpHeap(final Path path) {
        LOG.info("writing heap dump to {}", path);
        final HotSpotDiagnosticMXBean bean = getBean(HotSpotDiagnosticMXBean.class);
        if (bean == null) {
            return;
        }
        try {
            bean.dumpHeap(path.toString(), true);
        } catch (IOException e) {
            LOG.warn("error writing heap dump", e);
        }
    }

    /**
     * Requires JVM argument {@code -XX:NativeMemoryTracking=summary}.
     * Logs a warning if there was an error getting the NMT summary or if NMT was disabled.
     * This warning will be logged only once per process instance.
     * Produces simpler and more concise human-readable summary of NMT than the native human-readable output from the
     * JVM.
     * @return Human-readable NMT summary.  null if there was an error getting the summary.
     * @see #getNmt()
     */
    @Nullable
    public static String formatNmt() {
        final Nmt nmt = Nmt.get();
        if (nmt == null) {
            return null;
        }
        return nmt.toString();
    }

    /**
     * Requires JVM argument {@code -XX:NativeMemoryTracking=summary}.
     * Logs a warning if there was an error getting the NMT summary or if NMT was disabled.
     * This warning will be logged only once per process instance.
     * @return {@link Nmt} instance. null if there was an error getting the summary.
     */
    @Nullable
    public static Nmt getNmt() {
        return Nmt.get();
    }

    /**
     * Kicks off a poller thread that will periodically log human-readable NMT.
     * Uses {@link #formatNmt()} internally, and so also requires JVM argument
     * {@code -XX:NativeMemoryTracking=summary}.
     * @param interval The interval with which to poll and log NMT.
     * @return {@link NmtCloseable} that you can use to terminate the poller.
     */
    public static NmtCloseable pollNmt(final Duration interval) {
        final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor("nmt-poller");
        final long intervalNanos = TimeUnit.SECONDS.toNanos(interval.getSeconds()) +
                TimeUnit.NANOSECONDS.toNanos(interval.getNano());
        final Runnable command = () -> {
            final String summary = formatNmt();
            // null return values will cause a warning to get logged without us needing to do so.
            if (summary != null) {
                LOG.info("\n" + summary);
            }
        };
        exec.scheduleWithFixedDelay(command, 0, intervalNanos, TimeUnit.NANOSECONDS);
        return exec::shutdownNow;
    }

    static String formatBytes(final long bytes) {
        final int k = 1024;
        if (bytes < k) {
            return bytes + " B";
        }
        final int exp = (int)(Math.log(bytes) / Math.log(k));
        final char unit = "KMGTPEZ".charAt(exp - 1);
        final double printBytes = bytes / Math.pow(k, exp);
        return String.format("%.2f %ciB", printBytes, unit);
    }

    @Nullable
    private static <T extends PlatformManagedObject> T getBean(final Class<T> iface) {
        final String name = iface.getCanonicalName();
        T ret;
        try {
            ret = ManagementFactory.getPlatformMXBean(iface);
        } catch (IllegalArgumentException e) {
            LOG.warn("error getting bean {}", name, e);
            return null;
        }
        if (ret == null) {
            LOG.warn("bean {} did not exist", name);
        }
        return ret;
    }

    @VisibleForTesting
    @Nonnull
    static Path getTmpDir() {
        final String propName = "java.io.tmpdir";
        String val = null;
        try {
            val = System.getProperty(propName);
        } catch (SecurityException e) {
            LOG.warn("error getting system property {}", propName, e);
        }
        if (val == null) {
            val = DEFAULT_TMP_PATH;
        }
        return Paths.get(val);
    }

    @VisibleForTesting
    @Nonnull
    static Path getHeapDumpDir() {
        final String envName = "MESOS_SANDBOX";
        final String envVar;
        try {
            envVar = getenv.apply(envName);
        } catch (SecurityException e) {
            LOG.warn("error getting environment variable {}", envName, e);
            return getTmpDir();
        }
        if (envVar == null) {
            return getTmpDir();
        }
        return Paths.get(envVar);
    }

    @Nonnull
    private static Path getHeapDumpPath() {
        final String filename = String.format("heapdump-%s.hprof", Instant.now());
        return getHeapDumpDir().resolve(filename);
    }

    /**
     * Returned by {@link #pollNmt(Duration)} call to facilitate poller shutdown.
     */
    public interface NmtCloseable extends Closeable {
        /**
         * Initiates immediate shutdown of the poller.
         */
        void close();
    }
}
