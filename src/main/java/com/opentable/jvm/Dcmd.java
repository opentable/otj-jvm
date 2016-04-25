package com.opentable.jvm;

import java.util.Arrays;

import javax.annotation.Nullable;
import javax.management.MBeanException;
import javax.management.ReflectionException;

import com.sun.management.DiagnosticCommandMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.management.ManagementFactoryHelper;

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
        final DiagnosticCommandMBean bean = ManagementFactoryHelper.getDiagnosticCommandMBean();
        final Object[] args = new Object[]{a};
        final String[] signature = new String[]{String[].class.getName()};
        try {
            return (String)bean.invoke(cmd, args, signature);
        } catch (MBeanException | ReflectionException e) {
            log.warn(String.format("error invoking diagnostic command %s with args %s", cmd, Arrays.toString(args)), e);
            return null;
        }
    }
}
