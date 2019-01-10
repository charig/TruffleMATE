package som;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import som.vm.Universe;

public class VMOptions {
  public final String[] args;
  public final boolean showUsage;

  @CompilationFinal public boolean debuggerEnabled;
  @CompilationFinal public boolean webDebuggerEnabled;
  @CompilationFinal public boolean profilingEnabled;
  @CompilationFinal public boolean dynamicMetricsEnabled;
  @CompilationFinal public boolean printAST;
  @CompilationFinal public boolean vmReflectionEnabled;
  @CompilationFinal public boolean vmReflectionActivated;
  @CompilationFinal public boolean unoptimizedIH;
  @CompilationFinal public boolean envInObject;
  @CompilationFinal public List<URL> classPath;

  public VMOptions(final String[] args) {
    vmReflectionEnabled = false;
    printAST = false;
    unoptimizedIH = false;
    envInObject = false;
    classPath = new ArrayList<URL>();
    this.args = processVmArguments(args);
    showUsage = args.length == 0;
    if (!VmSettings.INSTRUMENTATION &&
        (debuggerEnabled || webDebuggerEnabled || profilingEnabled ||
        dynamicMetricsEnabled)) {
      throw new IllegalStateException(
          "Instrumentation is not enabled, but one of the tools is used. " +
          "Please set -D" + VmSettings.INSTRUMENTATION_PROP + "=true");
    }
  }

  private String[] processVmArguments(final String[] arguments) {
    int currentArg = 0;
    boolean parsedArgument = true;

    while (parsedArgument) {
      if (currentArg >= arguments.length) {
        return null;
      } else {
        if (arguments[currentArg].equals("--debug")) {
          debuggerEnabled = true;
          currentArg += 1;
        } else if (arguments[currentArg].equals("--web-debug")) {
          webDebuggerEnabled = true;
          currentArg += 1;
        } else if (arguments[currentArg].equals("--profile")) {
          profilingEnabled = true;
          currentArg += 1;
        } else if (arguments[currentArg].equals("--dynamic-metrics")) {
          dynamicMetricsEnabled = true;
          currentArg += 1;
        } else if (arguments[currentArg].equals("-cp") && currentArg + 1 <= arguments.length) {
          setupClassPath(arguments[currentArg + 1]);
          currentArg += 2;
        } else if (arguments[currentArg].equals("-activateMate")) {
          vmReflectionActivated = true;
          currentArg += 1;
        } else if (arguments[currentArg].equals("--mate")) {
          vmReflectionEnabled = true;
          currentArg += 1;
        } else if (arguments[currentArg].equals("--unoptimizedIH")) {
          unoptimizedIH = true;
          currentArg += 1;
        } else if (arguments[currentArg].equals("--envInObject")) {
          envInObject = true;
          currentArg += 1;
        } else {
          parsedArgument = false;
        }
      }
    }

    // store remaining arguments
    if (currentArg < arguments.length) {
      return Arrays.copyOfRange(arguments, currentArg, arguments.length);
    } else {
      return null;
    }
  }

  public boolean printUsage() {
    if (!showUsage) {
      return true;
    }

    Universe.println("VM arguments, need to come before any application arguments:");
    Universe.println("");
    Universe.println("  --debug                Run in Truffle Debugger/REPL");
    Universe.println("  --web-debug            Start web debugger");
    Universe.println("");
    Universe.println("  --profile              Enable the TruffleProfiler");
    Universe.println("  --dynamic-metrics      Enable the DynamicMetrics tool");
    Universe.println("alternative options include:                                   ");
    Universe.println("    -cp <directories separated by " + File.pathSeparator + ">");
    Universe.println("                  set search path for application classes");
    return false;
  }

  @TruffleBoundary
  public void setupClassPath(final String cp) {
    // Create a new tokenizer to split up the string of directories
    StringTokenizer tokenizer = new StringTokenizer(cp, File.pathSeparator);

    // Get the directories and put them into the class path array
    for (int i = 0; tokenizer.hasMoreTokens(); i++) {
      try {
        classPath.add(i, new File(tokenizer.nextToken()).toURI().toURL());
      } catch (MalformedURLException e) {
        Universe.errorExit("Classpath was not provided in proper format");
      }
    }
  }
}
