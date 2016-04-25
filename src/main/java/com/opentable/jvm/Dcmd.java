package com.opentable.jvm;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import javax.annotation.Nullable;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute diagnostic commands.
 */
class Dcmd {
    private static final Logger log = LoggerFactory.getLogger(Dcmd.class);

    /**
     * Logs a warning and returns null if there was an error running the command.
     * @param cmd The command to execute.
     * @param a Varargs: the command's arguments.
     * @return The result of the command.  null if there was an error running the command.
     */
    @Nullable
    static String exec(String cmd, String ...a) {
        final ObjectName name;
        try {
            name = new ObjectName("com.sun.management", "type", "DiagnosticCommand");
        } catch (MalformedObjectNameException e) {
            throw new AssertionError("should never happen", e);
        }
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final Object[] args = new Object[]{a};
        final String[] signature = new String[]{String[].class.getName()};
        try {
            return (String)server.invoke(name, cmd, args, signature);
        } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
            log.warn(String.format("error invoking diagnostic command %s with args %s", cmd, Arrays.toString(args)), e);
            return null;
        }
    }
}
