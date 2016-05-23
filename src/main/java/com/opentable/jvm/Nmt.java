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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for Native Memory Tracking information.
 * @see Memory#getNmt()
 */
public class Nmt {
    private static final Logger LOG = LoggerFactory.getLogger(Nmt.class);
    private static final String NMT_DISABLED = "Native memory tracking is not enabled\n";
    // Data comes back in "KB", as formatted by the HotSpot VM.
    // However, a read of the source code (specifically share/vm/utilities/globalDefinitions.hpp)
    // reveals that they actually mean KiB.
    @VisibleForTesting
    static final long K = 1024;

    public final Usage total;
    // Keys are human-readable category names, such as "Java Heap" or "Arena Chunk".
    // Categories' usages are not guaranteed to sum to total usage.
    // Entries will be in the same order as the diagnostic command output.
    public final Map<String, Usage> categories;

    private Nmt(final Usage total, final Map<String, Usage> categories) {
        this.total = total;
        this.categories = categories;
    }

    /**
     * Requires JVM argument -XX:NativeMemoryTracking=summary.
     * Logs a warning if there was an error getting the NMT summary or if NMT was disabled.
     * @return Human-readable NMT summary.  null if there was an error getting the summary.
     */
    @Nullable
    static String invoke() {
        final String ret = Dcmd.invoke("vmNativeMemory", "summary");
        if (NMT_DISABLED.equals(ret)) {
            LOG.warn(ret.trim());
            return null;
        }
        return ret;
    }

    /**
     * Requires JVM argument -XX:NativeMemoryTracking=summary.
     * Logs a warning if there was an error getting the NMT summary or if NMT was disabled.
     * @return null if there was an error getting the summary.
     */
    @Nullable
    static Nmt get() {
        final String nmt = invoke();
        if (nmt == null) {
            return null;
        }
        try {
            return parse(nmt);
        } catch (IllegalArgumentException e) {
            LOG.warn("un-parseable NMT data:\n{}", nmt, e);
            return null;
        }
    }

    /**
     * @param s String to parse.
     * @return Filled-out {@link Usage} instance.
     * @throws IllegalArgumentException with human-readable error if string couldn't be parsed.
     */
    @VisibleForTesting
    static Usage parseUsage(final String s) {
        final String reservedLabel = "reserved=";
        final String firstKB = "KB, ";
        int i, j;
        i = s.indexOf(reservedLabel);
        if (i == -1) {
            throw new IllegalArgumentException("could not find reserved label");
        }
        j = s.indexOf(firstKB, i);
        if (j == -1) {
            throw new IllegalArgumentException("could not find KB after reserved label");
        }
        final String reservedStr = s.substring(i + reservedLabel.length(), j);
        final long reserved;
        try {
            reserved = Long.parseLong(reservedStr) * K;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("could not parse reserved %s", reservedStr));
        }
        final String committedLabel = "committed=";
        i = s.indexOf(committedLabel, j + firstKB.length());
        if (i == -1) {
            throw new IllegalArgumentException("could not find committed label");
        }
        j = s.indexOf("KB", i);
        final String committedStr = s.substring(i + committedLabel.length(), j);
        final long committed;
        try {
            committed = Long.parseLong(committedStr) * K;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("could not parse committed %s", committedStr));
        }
        return new Usage(reserved, committed);
    }

    /**
     * @param nmt {@link #invoke()} Diagnostic command} output to parse.
     * @return Filled-out {@link Nmt} instance.
     * @throws IllegalArgumentException with human-readable error if string couldn't be parsed.
     */
    @VisibleForTesting
    static Nmt parse(@Nonnull final String nmt) {
        final String prefixTotal = "Total: ";
        final String prefixDash = "-";
        final String prefixParen = " (";

        final List<String> lines = Arrays.asList(nmt.split("\n"));
        if (lines.size() < 5) {
            throw new IllegalArgumentException("insufficient lines to parse");
        }
        final Iterator<String> itr = lines.iterator();
        final String totalStr;
        itr.next(); // First line expected to be empty.
        itr.next(); // Second line expected to be "Native Memory Tracking:".
        itr.next(); // Third line expected to be empty.
        totalStr = itr.next();
        if (!totalStr.startsWith(prefixTotal)) {
            throw new IllegalArgumentException("first line is not total");
        }
        final Usage total;
        try {
            total = parseUsage(totalStr.substring(prefixTotal.length()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("could not parse total", e);
        }
        final Map<String, Usage> categories = new LinkedHashMap<>();
        int line = 1;
        while (itr.hasNext()) {
            String s = itr.next();
            ++line;
            if (!s.startsWith(prefixDash)) {
                continue;
            }
            s = s.substring(prefixDash.length()).trim();
            final int i = s.indexOf(prefixParen);
            if (i == -1) {
                throw new IllegalArgumentException(String.format("missing opening paren on line %d", line));
            }
            final String category = s.substring(0, i);
            final Usage usage;
            try {
                usage = parseUsage(s.substring(i + prefixParen.length()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("could not parse usage on line %d", line));
            }
            categories.put(category, usage);
        }
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("no categories parsed");
        }
        return new Nmt(total, categories);
    }

    /**
     * Fields are in bytes.
     */
    public static class Usage {
        public final long reserved;
        public final long committed;
        private Usage(final long reserved, final long committed) {
            this.reserved = reserved;
            this.committed = committed;
        }
    }
}
