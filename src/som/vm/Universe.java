/**
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package som.vm;

import static som.vm.constants.Classes.systemClass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import som.VMOptions;
import som.VmSettings;
import som.interpreter.Invokable;
import som.interpreter.MateNode;
import som.interpreter.MateifyVisitor;
import som.interpreter.SomLanguage;
import som.interpreter.TruffleCompiler;
import som.primitives.Primitives;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.MateClasses;
import som.vm.constants.Nil;
import som.vmobjects.InvokableLayoutImpl;
import som.vmobjects.SArray;
import som.vmobjects.SBasicObjectLayoutImpl;
import som.vmobjects.SBlock;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SInvokable.SPrimitive;
import som.vmobjects.SObject;
import som.vmobjects.SObjectLayoutImpl;
import som.vmobjects.SReflectiveObject;
import som.vmobjects.SReflectiveObjectLayoutImpl;
import som.vmobjects.SReflectiveObjectLayoutImpl.SReflectiveObjectType;
import som.vmobjects.SSymbol;
import tools.debugger.Tags;
import tools.dym.DynamicMetrics;
import tools.language.StructuralProbe;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.ExecutionContext;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Builder;
import com.oracle.truffle.api.vm.PolyglotEngine.Instrument;

public class Universe extends ExecutionContext {
  public Universe(final String[] args) throws IOException {
    if (current != null) {
      current.validUniverse.invalidate();
    }
    current = this;
    truffleRuntime = Truffle.getRuntime();
    avoidExit    = false;
    lastExitCode = 0;
    options = new VMOptions(args);
    mateDeactivated = this.getTruffleRuntime().createAssumption();
    mateActivated = null;
    globalSemanticsDeactivated = this.getTruffleRuntime().createAssumption();
    globalSemanticsActivated = null;
    globalSemantics = null;
    validUniverse = this.getTruffleRuntime().createAssumption();

    if (options.vmReflectionActivated) {
      activatedMate();
    }

    if (ObjectMemory.last == null) {
      objectMemory = new ObjectMemory(options.classPath, structuralProbe);
      objectMemory.initializeSystem();
    } else {
      objectMemory = ObjectMemory.last;
    }
    if (options.showUsage) {
      VMOptions.printUsageAndExit();
    }

  }

  public static Universe getInitializedVM(String[] arguments) throws IOException {
    Builder builder = PolyglotEngine.newBuilder();
    builder.config(SomLanguage.MIME_TYPE, SomLanguage.CMD_ARGS, arguments);
    PolyglotEngine engine = builder.build();

    engine.getInstruments().values().forEach(i -> i.setEnabled(false));

    // Trigger initialization
    assert null == engine.getLanguages().get(SomLanguage.MIME_TYPE).getGlobalObject();
    return Universe.getCurrent();
  }

  public static void main(final String[] args) {
    Builder builder = PolyglotEngine.newBuilder();
    builder.config(SomLanguage.MIME_TYPE, SomLanguage.CMD_ARGS, args);

    VMOptions vmOptions = new VMOptions(args);
    if (vmOptions.debuggerEnabled) {
      // startDebugger(builder);
    } else {
      startExecution(builder, vmOptions);
    }
  }

  private static void startExecution(final Builder builder,
      final VMOptions vmOptions) {
    engine = builder.build();

    Map<String, Instrument> instruments = engine.getInstruments();
    Instrument profiler = instruments.get("profiler");
    if (vmOptions.profilingEnabled && profiler == null) {
      errorPrintln("Truffle profiler not available. Might be a class path issue");
    } else if (profiler != null) {
      profiler.setEnabled(vmOptions.profilingEnabled);
    }
    // instruments.get(Highlight.ID).setEnabled(vmOptions.highlightingEnabled);

    Debugger debugger = null;
    if (VmSettings.TRUFFLE_DEBUGGER_ENABLED) {
      debugger = Debugger.find(engine);
    }

    /*if (vmOptions.webDebuggerEnabled) {
      assert VmSettings.TRUFFLE_DEBUGGER_ENABLED && debugger != null;
      Instrument webDebuggerInst = instruments.get(WebDebugger.ID);
      webDebuggerInst.setEnabled(true);

      webDebugger = webDebuggerInst.lookup(WebDebugger.class);
      webDebugger.startServer(debugger);
    }*/

    if (vmOptions.dynamicMetricsEnabled) {
      assert VmSettings.DYNAMIC_METRICS;
      Instrument dynM = instruments.get(DynamicMetrics.ID);
      dynM.setEnabled(true);
      structuralProbe = dynM.lookup(StructuralProbe.class);
      assert structuralProbe != null : "Initialization of DynamicMetrics tool incomplete";
    }

    engine.eval(SomLanguage.START);
    engine.dispose();
    System.exit(Universe.getCurrent().lastExitCode);
  }

  public Object execute(final String className, final String selector) {
    DynamicObject clazz = loadClass(symbolFor(className));

    // Lookup the initialize invokable on the system class
    DynamicObject initialize = SClass.lookupInvokable(SObject.getSOMClass(clazz),
        symbolFor(selector));
    return SInvokable.invoke(initialize, MateClasses.STANDARD_ENVIRONMENT, ExecutionLevel.Base, clazz);
  }

  public Object execute() {
    // Start the shell if no filename is given
    String[] arguments = options.args;
    if (arguments.length == 0) {
      Shell shell = new Shell(this);
      return shell.start();
    }

    // Lookup the initialize invokable on the system class
    DynamicObject initialize = SClass.lookupInvokable(
        systemClass, symbolFor("initialize:"));

    return SInvokable.invoke(initialize, MateClasses.STANDARD_ENVIRONMENT, ExecutionLevel.Base, objectMemory.getSystemObject(), SArray.create(arguments));
  }

  public void mateify(DynamicObject clazz) {
    int countOfInvokables = SClass.getNumberOfInstanceInvokables(clazz);
    for (int i = 0; i < countOfInvokables; i++) {
      this.mateifyMethod(SClass.getInstanceInvokable(clazz, i));
    }
  }

  public void mateifyMethod(DynamicObject method) {
    this.mateifyNode(InvokableLayoutImpl.INSTANCE.getInvokable(method));
  }

  public void mateifyNode(Node node) {
    MateifyVisitor visitor = new MateifyVisitor();
    if (node.getParent() == null) {
      Node actual = ((MateNode) node).asMateNode();
      if (actual == null) { actual = node; }
      for (Node child: actual.getChildren()) {
        child.accept(visitor);
      }
    } else {
      node.accept(visitor);
    }
  }

  public TruffleRuntime getTruffleRuntime() {
    return truffleRuntime;
  }

  public ObjectMemory getObjectMemory() {
    return objectMemory;
  }

  public Primitives getPrimitives() {
    return objectMemory.getPrimitives();
  }

  public void exit(final int errorCode) {
    TruffleCompiler.transferToInterpreter("exit");
    // Exit from the Java system
    if (!avoidExit) {
      System.exit(errorCode);
    } else {
      lastExitCode = errorCode;
    }
  }

  public int lastExitCode() {
    return lastExitCode;
  }

  public static void callerNeedsToBeOptimized(final String msg) {
    if (VmSettings.FAIL_ON_MISSING_OPTIMIZATIONS) {
      CompilerAsserts.neverPartOfCompilation(msg);
    }
  }

  public static void errorExit(final String message) {
    TruffleCompiler.transferToInterpreter("errorExit");
    errorPrintln("Runtime Error: " + message);
    Universe.getCurrent().exit(1);
  }

  @TruffleBoundary
  public SSymbol symbolFor(final String string) {
    return objectMemory.symbolFor(string);
  }

  public static SBlock newBlock(final DynamicObject method,
      final MaterializedFrame context) {
    return new SBlock(method, context);
  }

  @TruffleBoundary
  public static DynamicObject newMethod(final SSymbol signature,
      final Invokable truffleInvokable, final boolean isPrimitive,
      final DynamicObject[] embeddedBlocks) {
    if (isPrimitive) {
      return SPrimitive.create(signature, truffleInvokable);
    } else {
      return SMethod.create(signature, truffleInvokable, embeddedBlocks);
    }
  }

  public DynamicObject loadClass(final SSymbol name) {
    DynamicObject result = (DynamicObject) getGlobal(name);
    if (result != null) { return result; }
    return this.loadClass(getSourceForClassName(name));
  }

  public Source getSourceForClassName(final SSymbol name) {
    File file = new File(resolveClassFilePath(name.getString()));
    try {
      return Source.newBuilder(file).mimeType(
          SomLanguage.MIME_TYPE).name(name.getString()).build();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RuntimeException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  public DynamicObject loadClass(final Source source) {
    return objectMemory.loadClass(source, null);
  }

  @TruffleBoundary
  public boolean hasGlobal(final SSymbol name) {
    return objectMemory.hasGlobal(name);
  }

  @TruffleBoundary
  public DynamicObject getGlobal(final SSymbol name) {
    return objectMemory.getGlobal(name);
  }

  public void setGlobal(final String name, final DynamicObject value) {
    objectMemory.setGlobal(symbolFor(name), value);
  }

  @TruffleBoundary
  public void setGlobal(final SSymbol name, final DynamicObject value) {
    objectMemory.setGlobal(name, value);
  }

  @TruffleBoundary
  public DynamicObject loadShellClass(final String stmt) throws IOException {
    return objectMemory.loadShellClass(stmt);
  }

  public void setAvoidExit(final boolean value) {
    avoidExit = value;
  }

  @TruffleBoundary
  public static void errorPrint(final String msg) {
    // Checkstyle: stop
    System.err.print(msg);
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void errorPrintln(final String msg) {
    // Checkstyle: stop
    System.err.println(msg);
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void print(final String msg) {
    // Checkstyle: stop
    System.out.print(msg);
    // Checkstyle: resume
  }

  @TruffleBoundary
  public static void println(final String msg) {
    // Checkstyle: stop
    System.out.println(msg);
    // Checkstyle: resume
  }

  public static Universe getCurrent() {
    return current;
  }

  public static void setCurrent(final Universe universe) {
    current = universe;
  }

  public DynamicObjectFactory getInstancesFactory() {
    if (options.vmReflectionEnabled) {
      return SReflectiveObject.SREFLECTIVE_OBJECT_FACTORY;
      // return SReflectiveObjectEnvInObj.SREFLECTIVE_OBJECT_ENVINOBJ_FACTORY;
    } else {
      return SObject.SOBJECT_FACTORY;
    }
  }

  public SObject getInstanceArgumentsBuilder() {
    if (vmReflectionEnabled()) {
      // return new SReflectiveObjectEnvInObj();
      return new SReflectiveObject();
    } else {
      return new SObject();
    }
  }

  public String imageName() {
    return "Smalltalk/fake.image";
  }

  public static String frameOnStackSlotName() {
    // Name for the frameOnStack slot,
    // starting with ! to make it a name that's not possible in Smalltalk
    return "!frameOnStack";
  }

  public DynamicObject createNilObject() {
    DynamicObject dummyObjectForInitialization = SBasicObjectLayoutImpl.INSTANCE.createSBasicObject();
    if (options.vmReflectionEnabled) {
      return SReflectiveObjectLayoutImpl.INSTANCE.createSReflectiveObjectShape(dummyObjectForInitialization, dummyObjectForInitialization).newInstance();
      // return SReflectiveObjectEnvInObjLayoutImpl.INSTANCE.createSReflectiveObjectEnvInObjShape(dummyObjectForInitialization).newInstance(dummyObjectForInitialization);
    } else {
      return SObjectLayoutImpl.INSTANCE.createSObjectShape(dummyObjectForInitialization).newInstance();
    }
  }

  public DynamicObjectFactory createObjectShapeFactoryForClass(final DynamicObject clazz) {
    if (options.vmReflectionEnabled) {
      return SReflectiveObject.createObjectShapeFactoryForClass(clazz);
      // return SReflectiveObjectEnvInObj.createObjectShapeFactoryForClass(clazz);
    } else {
      return SObject.createObjectShapeFactoryForClass(clazz);
   }
  }

  public boolean vmReflectionEnabled() {
    return options.vmReflectionEnabled;
  }

  public boolean printAST() {
    return options.printAST;
  }

  public Assumption getMateDeactivatedAssumption() {
    return this.mateDeactivated;
  }

  public Assumption getMateActivatedAssumption() {
    return this.mateActivated;
  }

  public Assumption getGlobalSemanticsDeactivatedAssumption() {
    return this.globalSemanticsDeactivated;
  }

  public Assumption getGlobalSemanticsActivatedAssumption() {
    return this.globalSemanticsActivated;
  }

  public Assumption getValidUniverseAssumption() {
    return this.validUniverse;
  }

  public DynamicObject getGlobalSemantics() {
    return this.globalSemantics;
  }

  public void cacheNewObjectType(DynamicObject klass, ObjectType type) {
    if (objectTypes.containsKey(klass)) {
      objectTypes.get(klass).add(type);
    } else {
      ArrayList<ObjectType> list = new ArrayList<ObjectType>();
      list.add(type);
      objectTypes.put(klass, list);
    }
  }

  public ObjectType getCachedObjectType(DynamicObject klass, DynamicObject environment) {
    if (objectTypes.containsKey(klass)) {
      for (ObjectType type : objectTypes.get(klass)) {
        if (((SReflectiveObjectType) type).getEnvironment() == environment) {
          return type;
        }
      }
    }
    return null;
  }

  public void activatedMate() {
    if (this.getMateDeactivatedAssumption().isValid()) {
      this.getMateDeactivatedAssumption().invalidate();
    }
    mateActivated = this.getTruffleRuntime().createAssumption();
  }

  public void deactivateMate() {
    if (this.getMateActivatedAssumption().isValid()) {
      this.getMateActivatedAssumption().invalidate();
    }
    mateDeactivated = this.getTruffleRuntime().createAssumption();
  }

  public String resolveClassFilePath(String className) throws IllegalStateException {
    for (String cpEntry : options.classPath) {
      // Load the class from a file and return the loaded class
      String fname = cpEntry + File.separator + className + ".som";
      File file = new File(fname);
      if (file.exists() && !file.isDirectory()) {
          return fname;
      }
    }
    throw new IllegalStateException(className
          + " class could not be loaded. "
          + "It is likely that the class path has not been initialized properly. "
          + "Please set system property 'system.class.path' or "
          + "pass the '-cp' command-line parameter.");

  }

  public DynamicObject getTrueObject()   { return objectMemory.getTrueObject(); }
  public DynamicObject getFalseObject()  { return objectMemory.getFalseObject(); }
  public DynamicObject getSystemObject() { return objectMemory.getSystemObject(); }

  public static void reportSyntaxElement(final Class<? extends Tags> type,
      final SourceSection source) {
    /*if (webDebugger != null) {    
      webDebugger.reportSyntaxElement(type, source);
    }*/
  }

  public static void insertInstrumentationWrapper(final Node node) {
    // TODO: make thread-safe!!!
    // TODO: can I assert that it is locked?? helper on Node??
    if (VmSettings.INSTRUMENTATION) {
      assert node.getSourceSection() != null || (node instanceof WrapperNode) : "Node needs source section, or needs to be wrapper";
      // TODO: a way to check whether the node needs actually wrapping?
      // String[] tags = node.getSourceSection().getTags();
      // if (tags != null && tags.length > 0) {
      // InstrumentationHandler.insertInstrumentationWrapper(node);
      // }
    }
  }

  public void setGlobalEnvironment(DynamicObject environment) {
    if (globalSemanticsActivated.isValid()) {
      globalSemanticsActivated.invalidate();
    } else {
      globalSemanticsDeactivated.invalidate();
    }
    if (environment == Nil.nilObject) {
      globalSemanticsDeactivated = Truffle.getRuntime().createAssumption();
    } else {
      globalSemanticsActivated = Truffle.getRuntime().createAssumption();
    }
    globalSemantics = environment;
  }

  public boolean registerExport(final String name, final Object value) {
    boolean wasExportedAlready = exports.containsKey(name);
    exports.put(name, value);
    return wasExportedAlready;
  }

  public Object getExport(final String name) {
    return exports.get(name);
  }

  private final TruffleRuntime                  truffleRuntime;
  // TODO: this is not how it is supposed to be... it is just a hack to cope
  //       with the use of system.exit in SOM to enable testing
  @CompilationFinal private boolean             avoidExit;
  private int                                   lastExitCode;

  // Latest instance
  // WARNING: this is problematic with multiple interpreters in the same VM...
  @CompilationFinal private static Universe current;
  @CompilationFinal private static PolyglotEngine engine;
  private final ObjectMemory objectMemory;
  @CompilationFinal private static StructuralProbe structuralProbe;
  // @CompilationFinal private static WebDebugger webDebugger;
  @CompilationFinal private static Debugger    debugger;

  private final VMOptions options;
  private final Map<String, Object> exports = new HashMap<>();
  public static final Source emptySource = Source.newBuilder("").name("Empty Source for primitives and mate wrappers").
      mimeType(SomLanguage.MIME_TYPE).build();

  @CompilationFinal private Assumption mateActivated;
  @CompilationFinal private Assumption mateDeactivated;
  @CompilationFinal private Assumption globalSemanticsActivated;
  @CompilationFinal private Assumption globalSemanticsDeactivated;
  @CompilationFinal private DynamicObject globalSemantics;
  @CompilationFinal private Assumption validUniverse;
  @CompilationFinal private Map<DynamicObject, List<ObjectType>> objectTypes = new HashMap<DynamicObject, List<ObjectType>>();
}
