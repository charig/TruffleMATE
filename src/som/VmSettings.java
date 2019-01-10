package som;

import bd.settings.Settings;

public class VmSettings implements Settings {
  public static final int NUM_THREADS;

  public static final boolean FAIL_ON_MISSING_OPTIMIZATIONS;
  public static final boolean DEBUG_MODE;
  public static final boolean INSTRUMENTATION;
  public static final boolean DYNAMIC_METRICS;
  public static final boolean DNU_PRINT_STACK_TRACE;
  public static final boolean TRUFFLE_DEBUGGER_ENABLED;
  public static final boolean IGV_DUMP_AFTER_PARSING;
  public static final boolean ANSI_COLOR_IN_OUTPUT;

  public static final String BASE_DIRECTORY;
  public static final String INSTRUMENTATION_PROP = "som.instrumentation";

  static {
    String prop = System.getProperty("som.threads");
    if (prop == null) {
      NUM_THREADS = 1; // Runtime.getRuntime().availableProcessors();
    } else {
      NUM_THREADS = Integer.valueOf(prop);
    }

    FAIL_ON_MISSING_OPTIMIZATIONS = getBool("som.failOnMissingOptimization", false);
    DEBUG_MODE      = getBool("som.debugMode",      false);
    TRUFFLE_DEBUGGER_ENABLED = getBool("som.truffleDebugger", false);

    boolean dm = getBool("som.dynamicMetrics", false);
    DYNAMIC_METRICS = dm;
    INSTRUMENTATION = dm || getBool(INSTRUMENTATION_PROP, false);

    DNU_PRINT_STACK_TRACE = getBool("som.printStackTraceOnDNU", false);
    IGV_DUMP_AFTER_PARSING = getBool("som.igvDumpAfterParsing", false);
    ANSI_COLOR_IN_OUTPUT = getBool("som.useAnsiColoring", false);

    BASE_DIRECTORY = System.getProperty("som.baseDir", System.getProperty("user.dir"));

  }

  private static boolean getBool(final String prop, final boolean defaultVal) {
    return Boolean.parseBoolean(System.getProperty(prop, defaultVal ? "true" : "false"));
  }

  @Override
  public boolean dynamicMetricsEnabled() {
    return DYNAMIC_METRICS;
  }
}
