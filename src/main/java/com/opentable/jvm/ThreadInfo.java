package com.opentable.jvm;

import javax.annotation.Nullable;

public class ThreadInfo {
    /**
     * Like "jcmd Thread.print -l".
     * Logs a warning if there was an error getting the thread dump.
     * @return Human-readable dump of all thread stacks.  null if there was an error getting it.
     */
    @Nullable
    public static String format() {
        return Dcmd.exec("threadPrint", "-l");
    }
}