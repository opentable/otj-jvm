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
class Dcmd { //NOPMD
    private static final Logger LOG = LoggerFactory.getLogger(Dcmd.class);

    /**
     * Logs a warning and returns null if there was an error running the command.
     * @param cmd The command to execute.
     * @param args Varargs: the command's arguments.
     * @return The result of the command.  null if there was an error running the command.
     */
    @Nullable
    static String invoke(String cmd, String ...args) {
        final ObjectName name;
        try {
            name = new ObjectName("com.sun.management", "type", "DiagnosticCommand");
        } catch (MalformedObjectNameException e) {
            throw new AssertionError("should never happen", e);
        }
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final Object[] wrappedArgs = new Object[]{args};
        final String[] signature = new String[]{String[].class.getName()};
        try {
            return (String)server.invoke(name, cmd, wrappedArgs, signature);
        } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
            LOG.warn("error invoking diagnostic command {} with args {}", cmd, Arrays.toString(args), e);
            return null;
        }
    }
}
