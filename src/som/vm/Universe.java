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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.graalvm.polyglot.Context;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.InstrumentInfo;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode.WrapperNode;
import com.oracle.truffle.api.instrumentation.InstrumentationHandler;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import som.VMOptions;
import som.VmSettings;
import som.compiler.Parser.ParseError;
import som.compiler.SourcecodeCompiler;
import som.interpreter.Invokable;
import som.interpreter.MateifyVisitor;
import som.interpreter.NodeVisitorUtil;
import som.interpreter.SArguments;
import som.interpreter.SomLanguage;
import som.interpreter.TruffleCompiler;
import som.interpreter.nodes.AbstractMessageSpecializationsFactory;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.MateMessageSpecializationsFactory;
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
import tools.language.StructuralProbe;

public class Universe {
  final Env env;

  public Universe(final Env environment) throws IOException {
    env = environment;
    truffleRuntime = Truffle.getRuntime();
    options = new VMOptions(environment.getApplicationArguments());
    mateDeactivated = this.getTruffleRuntime().createAssumption();
    globalSemanticsDeactivated = this.getTruffleRuntime().createAssumption();
    optimizedIH = this.getTruffleRuntime().createAssumption();
    mateSpecializationFactory = new MateMessageSpecializationsFactory();
    somSpecializationFactory = new AbstractMessageSpecializationsFactory.SOMMessageSpecializationsFactory();
    current = this;
  }

  public void updateArguments(final String[] arguments) {
    options = new VMOptions(arguments);
    initializeGeneralConfigurations();
  }

  private void initializeGeneralConfigurations() {
    avoidExit    = false;
    lastExitCode = 0;
    if (options.vmReflectionActivated) {
      activatedMate();
    }

    if (options.unoptimizedIH) {
      unoptimizedIH();
    }
    if (options.showUsage) {
      VMOptions.printUsageAndExit();
    }
  }

  public void initialize(final SomLanguage language) {
    initializeGeneralConfigurations();
    initializeIntruments();

    objectMemory = new ObjectMemory(new SourcecodeCompiler(language), structuralProbe);
    try {
      objectMemory.initializeSystem();
    } catch (ParseError e) {
      Universe.errorExit(e.getMessage());
    }
  }

  private void initializeIntruments() {
    Map<String, InstrumentInfo> instruments = env.getInstruments();

    InstrumentInfo profilerInfo = instruments.get("profiler");
    if (options.profilingEnabled && profilerInfo  == null) {
      errorPrintln("Truffle profiler not available. Might be a class path issue");
    } else if (profilerInfo != null) {
      // profiler.setEnabled(vmOptions.profilingEnabled);
    }

    if (options.dynamicMetricsEnabled) {
      assert VmSettings.DYNAMIC_METRICS;
      // InstrumentInfo dynMetric = instruments.get(DynamicMetrics.ID);
      // dynMetric.setEnabled(true);
      // structuralProbe = dynM.lookup(StructuralProbe.class);
      // assert structuralProbe != null : "Initialization of DynamicMetrics tool incomplete";
    }
  }

  public static void createVM(final String[] arguments) {
    context = Context.newBuilder(SomLanguage.LANG_NAME).arguments(SomLanguage.LANG_NAME, arguments).build();
    context.initialize(SomLanguage.LANG_NAME);
  }

  public static void main(final String[] arguments) {
    createVM(arguments);
    context.eval(SomLanguage.START);
    System.exit(Universe.getCurrent().lastExitCode);
  }

  public Object execute(final String className, final String selector) {
    DynamicObject clazz = loadClass(symbolFor(className));
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


  public void mateify(final DynamicObject clazz) {
    int countOfInvokables = SClass.getNumberOfInstanceInvokables(clazz);
    for (int i = 0; i < countOfInvokables; i++) {
      this.mateifyMethod(SClass.getInstanceInvokable(clazz, i));
    }
  }

  public void mateifyMethod(final DynamicObject method) {
    this.mateifyNode(InvokableLayoutImpl.INSTANCE.getInvokable(method));

  }

  public Node mateifyNode(final Node node) {
    MateifyVisitor visitor = new MateifyVisitor();
    if (!(node instanceof RootNode) & node.getParent() == null) {
      return NodeVisitorUtil.applyVisitor((ExpressionNode) node, visitor);
    }
    node.accept(visitor);
    return node;
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
    return Symbols.symbolFor(string);
  }

  public static SBlock newBlock(final DynamicObject method,
      final DynamicObject blockClass, final MaterializedFrame context) {
    return new SBlock(method, blockClass, context);
  }

  public DynamicObject createInstance(final String className) {
    DynamicObject klass = this.loadClass(
        this.symbolFor(className));
    return objectMemory.newObject(klass);
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
    DynamicObject result = getGlobal(name);
    if (result != null) { return result; }
    return this.loadClass(getSourceForClassName(name));
  }

  @TruffleBoundary
  public Source getSourceForClassName(final SSymbol name) {
    TruffleFile file = env.getTruffleFile(name.getString());
    try {
      return Source.newBuilder(SomLanguage.LANG_NAME, file).name(name.getString()).mimeType(SomLanguage.MIME_TYPE).build();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RuntimeException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  @TruffleBoundary
  public DynamicObject loadClass(final Source source) {
    try {
      return objectMemory.loadClass(source, null);
    } catch (ParseError e) {
      Universe.errorExit(e.getMessage());
      return null;
    }
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

  public DynamicObject getBlockClass(final int numberOfArguments) {
    return objectMemory.getBlockClass(numberOfArguments);
  }

  @TruffleBoundary
  public DynamicObject loadShellClass(final String stmt) throws IOException {
    try {
      return objectMemory.loadShellClass(stmt);
    } catch (ParseError e) {
      Universe.errorExit(e.getMessage());
      return null;
    }
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

  public Assumption getOptimizedIHAssumption() {
    return this.optimizedIH;
  }

  public DynamicObject getGlobalSemantics() {
    return this.globalSemantics;
  }

  public void cacheNewObjectType(final DynamicObject klass, final ObjectType type) {
    if (objectTypes.containsKey(klass)) {
      objectTypes.get(klass).add(type);
    } else {
      ArrayList<ObjectType> list = new ArrayList<ObjectType>();
      list.add(type);
      objectTypes.put(klass, list);
    }
  }

  public ObjectType getCachedObjectType(final DynamicObject klass, final DynamicObject environment) {
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
    if (mateActivated == null || !this.getMateActivatedAssumption().isValid()) {
      mateActivated = this.getTruffleRuntime().createAssumption();
    }
  }

  public void unoptimizedIH() {
    optimizedIH.invalidate();
  }

  public void deactivateMate() {
    if (this.getMateActivatedAssumption().isValid()) {
      this.getMateActivatedAssumption().invalidate();
    }
    mateDeactivated = this.getTruffleRuntime().createAssumption();
  }

  public File resolveClassFilePath(final String className) throws IllegalStateException {
    File f;
    URL url = ClassLoader.getSystemResource(className + ".som");
    if (url != null) {
      return new File(url.getPath());
    } else {
      for (URL cp : this.options.classPath) {
        f = new File(cp.getPath() + className + ".som");
        if (f.exists() && !f.isDirectory()) {
          return f;
        }
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

  private static final Object dynamicInstrumentationLock = new Object();

  public static void insertInstrumentationWrapper(final Node node) {
    // TODO: make thread-safe!!!
    // TODO: can I assert that it is locked?? helper on Node??
    if (VmSettings.INSTRUMENTATION) {
      assert node.getSourceSection() != null
          || (node instanceof WrapperNode) : "Node needs source section, or needs to be wrapper";
      synchronized (dynamicInstrumentationLock) {
        InstrumentationHandler.insertInstrumentationWrapper(node);
      }
    }
  }

  public void setGlobalEnvironment(final DynamicObject environment) {
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

  public AbstractMessageSpecializationsFactory specializationFactory() {
    if (vmReflectionEnabled()) {
        if (SArguments.getExecutionLevel(truffleRuntime.getCurrentFrame().getFrame(FrameAccess.READ_ONLY)) == ExecutionLevel.Base) {
          return mateSpecializationFactory;
        }
    }
    return somSpecializationFactory;
  }

  public Object getExport(final String name) {
    return exports.get(name);
  }

  private final TruffleRuntime                  truffleRuntime;
  public final AbstractMessageSpecializationsFactory mateSpecializationFactory;
  public final AbstractMessageSpecializationsFactory somSpecializationFactory;

  // TODO: this is not how it is supposed to be... it is just a hack to cope
  //       with the use of system.exit in SOM to enable testing
  @CompilationFinal private boolean             avoidExit;
  private int                                   lastExitCode;

  // Latest instance
  // WARNING: this is problematic with multiple interpreters in the same VM...
  @CompilationFinal private static Universe current;
  @CompilationFinal public static Context context;
  @CompilationFinal ObjectMemory objectMemory;
  @CompilationFinal private static StructuralProbe structuralProbe;
  // @CompilationFinal private static WebDebugger webDebugger;
  @CompilationFinal private static Debugger    debugger;

  @CompilationFinal VMOptions options;
  private final Map<String, Object> exports = new HashMap<>();
  public static final Source emptySource = Source.newBuilder(SomLanguage.LANG_NAME, "Empty Source for primitives and mate wrappers", null).
      mimeType(SomLanguage.MIME_TYPE).build();

  @CompilationFinal private Assumption mateActivated;
  @CompilationFinal private Assumption mateDeactivated;
  @CompilationFinal private Assumption globalSemanticsActivated;
  @CompilationFinal private Assumption globalSemanticsDeactivated;
  @CompilationFinal private Assumption optimizedIH;
  @CompilationFinal private DynamicObject globalSemantics;

  @CompilationFinal private Map<DynamicObject, List<ObjectType>> objectTypes = new HashMap<DynamicObject, List<ObjectType>>();
}
