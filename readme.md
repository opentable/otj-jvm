This library provides convenient access to JVM debugging information.

- `ThreadInfo.format` Get a `String` output as if you had run
  `jcmd Thread.print -l` on the command-line.
- `Memory.dumpHeap` Dump heap to filesystem.
- `Memory.formatNmt` Human-readable formatting of [NMT][1].  Provides
  simpler and more concise formatting than that returned by the JVM.
- `Memory.getNmt` Parsed NMT data (`Nmt` instance), suitable for
  tracking in analytics systems, such as Graphite.
- `Memory.pollNmt` Runs poller thread that periodically logs
  human-readable NMT.
- `Nmt.invoke` If you really must, you can easily get at the
  JVM-formatted human-readable NMT summary.

[1]: https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/tooldescr007.html
