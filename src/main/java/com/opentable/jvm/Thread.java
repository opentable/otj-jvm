package com.opentable.jvm;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class Thread {
    public static String formatInfo() {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ThreadInfo info : bean.dumpAllThreads(true, true)) {
            if (first) {
                first = false;
            } else {
                sb.append("\n");
            }
            formatInfo(sb, info);
        }
        return sb.toString();
    }

    private static void formatInfo(final StringBuilder sb, final ThreadInfo info) {
        final java.lang.Thread.State state = info.getThreadState();
        sb.append("\"").append(info.getThreadName()).append("\"@").append(info.getThreadId())
                .append("\n   state: ").append(state);

        if (info.getLockName() != null) {
            sb.append(" on ").append(info.getLockName());
        }
        if (info.getLockOwnerName() != null) {
            sb.append(" owned by \"").append(info.getLockOwnerName()).append("\"@").append(info.getLockOwnerId());
        }
        if (info.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (info.isInNative()) {
            sb.append(" (in native)");
        }
        sb.append('\n');

        final StackTraceElement[] elts = info.getStackTrace();
        if (elts.length != 0) {
            for (int i = 0; i < elts.length; i++) {
                sb.append("\tat ").append(elts[i]).append('\n');
                for (MonitorInfo monitorInfo : info.getLockedMonitors()) {
                    if (monitorInfo.getLockedStackDepth() == i) {
                        sb.append("\t- locked ").append(monitorInfo).append('\n');
                    }
                }
            }
            sb.append("\n   ");
        }

        final LockInfo[] lockInfos = info.getLockedSynchronizers();
        if (lockInfos.length == 0) {
            sb.append("No locked ownable synchronizers.\n");
        } else {
            sb.append("Locked ownable synchronizers:\n");
            for (LockInfo lockInfo : lockInfos) {
                sb.append("\t- ").append(lockInfo).append('\n');
            }
        }
    }
}
