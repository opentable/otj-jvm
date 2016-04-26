package com.opentable.jvm;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import javax.annotation.Nullable;

import com.sun.management.HotSpotDiagnosticMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Memory {
    private static final Logger LOG = LoggerFactory.getLogger(Memory.class);
    private static final String nmtDisabled = "Native memory tracking is not enabled\n";

    /**
     * Dumps heap with a name with a human-readable timestamp to the Mesos sandbox.
     * Logs where the heap dump will be written.
     * Logs a warning if there was a problem preventing the heap dump from being successfully created.
     */
    public static void dumpHeap() {
        final String path = String.format("/mnt/mesos/sandbox/heapdump-%s.hprof", Instant.now());
        dumpHeap(Paths.get(path));
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
}
