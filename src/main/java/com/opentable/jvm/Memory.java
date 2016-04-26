package com.opentable.jvm;

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

public class Memory {
    private static final Logger LOG = LoggerFactory.getLogger(Memory.class);
    private static final String NMT_DISABLED = "Native memory tracking is not enabled\n";

    @VisibleForTesting
    static final String DEFAULT_TMP_PATH = "/tmp";

    // Replaceable reference for testing.
    @VisibleForTesting
    static Function<String, String> getenv = System::getenv;

    /**
     * Dumps heap with a name with a human-readable timestamp to the Mesos sandbox, if environment variable
     * MESOS_SANDBOX is defined, or to the system temporary directory otherwise.
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

    // TODO Re-implement formatNmt.
    // Add a lower-level method that does the call and then parses the string output into a data structure
    // with statically-typed fields, etc., that we could easily use to make automated NMT graphite, other tracking,
    // etc. application analytics/metrics calls.  Then have formatNmt use that method so there are fewer code paths.

    /**
     * Requires JVM argument -XX:NativeMemoryTracking=summary.
     * Logs a warning if there was an error getting the NMT summary or if NMT was disabled.
     * @return Human-readable NMT summary.  null if there was an error getting the summary.
     */
    @Nullable
    public static String formatNmt() {
        final String ret = Dcmd.exec("vmNativeMemory", "summary");
        if (NMT_DISABLED.equals(ret)) {
            LOG.warn(ret.trim());
            return null;
        }
        return ret;
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
     * On construction, kicks off a poller thread that will periodically log NMT.
     * Uses {@link #formatNmt()}, and so also requires JVM argument -XX:NativeMemoryTracking=summary.
     */
    public static class NmtPoller implements Runnable {
        private static final Logger LOG = LoggerFactory.getLogger(NmtPoller.class);
        private final Duration interval;
        private final ScheduledExecutorService exec;

        /**
         * @param interval The interval with which to poll and log NMT.
         */
        public NmtPoller(final Duration interval) {
            this.interval = interval;
            exec = Executors.newSingleThreadScheduledExecutor("nmt-poller");
            final long nanos = TimeUnit.SECONDS.toNanos(interval.getSeconds()) +
                    TimeUnit.NANOSECONDS.toNanos(interval.getNano());
            exec.scheduleWithFixedDelay(this, 0, nanos, TimeUnit.NANOSECONDS);
        }

        @Override
        public void run() {
            final String summary = formatNmt();
            // null return values will cause a warning to get logged without us needing to do so.
            if (summary != null) {
                LOG.info(summary);
            }
        }

        /**
         * Initiates shutdown and blocks until the poller completes, or the timeout occurs, or the current
         * thread is interrupted, whichever happens first.
         * @param timeout The amount of time to wait after instructing the poller to shut down.
         * @throws InterruptedException If the thread is interrupted while waiting for the poller to shut down.
         */
        public void shutdown(final Duration timeout) throws InterruptedException {
            exec.shutdownNow();
            final long nanos = TimeUnit.SECONDS.toNanos(timeout.getSeconds()) +
                    TimeUnit.NANOSECONDS.toNanos(timeout.getNano());
            exec.awaitTermination(nanos, TimeUnit.NANOSECONDS);
        }
    }
}
