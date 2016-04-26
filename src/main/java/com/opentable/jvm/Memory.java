package com.opentable.jvm;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sun.management.HotSpotDiagnosticMXBean;

import com.google.common.annotations.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Memory {
    private static final Logger LOG = LoggerFactory.getLogger(Memory.class);
    private static final String nmtDisabled = "Native memory tracking is not enabled\n";
    @VisibleForTesting
    static EnvironmentProvider environmentProvider = new EnvironmentProvider() {
        @Nullable
        @Override
        public String getenv(String name) {
            return System.getenv(name);
        }
    };

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

    // TODO Write little NMT poller.
    // Add a method that kicks off a thread that polls and logs NMT info at some regular (specified?) interval.
    // It'll return something you can call to shut down the poller.

    /**
     * Requires JVM argument -XX:NativeMemoryTracking=summary.
     * Logs a warning if there was an error getting the NMT summary or if NMT was disabled.
     * @return Human-readable NMT summary.  null if there was an error getting the summary.
     */
    @Nullable
    public static String formatNmt() {
        final String ret = Dcmd.exec("vmNativeMemory", "summary");
        if (nmtDisabled.equals(ret)) {
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

    @Nonnull
    private static Path getTempDir() {
        final String propName = "java.io.tmpdir";
        final String defaultVal = "/tmp";
        String val;
        try {
            val = System.getProperty(propName);
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new AssertionError("should never happen", e);
        } catch (SecurityException e) {
            LOG.warn("error getting system property {}", propName, e);
            val = defaultVal;
        }
        if (val == null) {
            val = defaultVal;
        }
        return Paths.get(val);
    }

    @VisibleForTesting
    @Nonnull
    static Path getHeapDumpDir() {
        final String envName = "MESOS_SANDBOX";
        final String envVar;
        try {
            envVar = environmentProvider.getenv(envName);
        } catch (NullPointerException e) {
            throw new AssertionError("should never happen", e);
        } catch (SecurityException e) {
            LOG.warn("error getting environment variable {}", envName, e);
            return getTempDir();
        }
        if (envVar == null) {
            return getTempDir();
        }
        return Paths.get(envVar);
    }

    @Nonnull
    private static Path getHeapDumpPath() {
        final String filename = String.format("heapdump-%s.hprof", Instant.now());
        return getHeapDumpDir().resolve(filename);
    }

    // Replaceable stand-in for System.getenv for testing.
    @VisibleForTesting
    interface EnvironmentProvider {
        @Nullable
        String getenv(String name);
    }
}
