package som.vm;

import static som.vm.constants.Classes.arrayClass;
import static som.vm.constants.Classes.booleanClass;
import static som.vm.constants.Classes.characterClass;
import static som.vm.constants.Classes.classClass;
import static som.vm.constants.Classes.doubleClass;
import static som.vm.constants.Classes.falseClass;
import static som.vm.constants.Classes.integerClass;
import static som.vm.constants.Classes.metaclassClass;
import static som.vm.constants.Classes.methodClass;
import static som.vm.constants.Classes.nilClass;
import static som.vm.constants.Classes.objectClass;
import static som.vm.constants.Classes.primitiveClass;
import static som.vm.constants.Classes.stringClass;
import static som.vm.constants.Classes.symbolClass;
import static som.vm.constants.Classes.systemClass;
import static som.vm.constants.Classes.trueClass;
import static som.vm.constants.MateClasses.astNodeClass;
import static som.vm.constants.MateClasses.contextClass;
import static som.vm.constants.MateClasses.environmentMO;
import static som.vm.constants.MateClasses.messageMO;
import static som.vm.constants.MateClasses.operationalSemanticsMO;
import static som.vm.constants.MateClasses.shapeClass;

import java.io.IOException;
import java.util.HashMap;

import som.compiler.Disassembler;
import som.primitives.Primitives;
import som.vm.constants.Globals;
import som.vm.constants.Nil;
import som.vmobjects.SArray;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import som.vmobjects.SReflectiveObject;
import som.vmobjects.SSymbol;
import tools.language.StructuralProbe;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.Source;

//This is a pseudo object memory because the objects are actually managed by the Truffle/Java memory manager
public class ObjectMemory {
  @CompilationFinal public static ObjectMemory last;
  private final HashMap<SSymbol, DynamicObject> globals;
  private final HashMap<String, SSymbol> symbolTable;

  @CompilationFinal private DynamicObject trueObject;
  @CompilationFinal private DynamicObject falseObject;
  @CompilationFinal private DynamicObject systemObject;

  // Optimizations
  private final DynamicObject[] blockClasses;
  private final StructuralProbe structuralProbe;
  private static final SObject layoutClass = Universe.getCurrent().getInstanceArgumentsBuilder();

  private final Primitives primitives;

  protected ObjectMemory(final StructuralProbe probe) {
    last = this;
    globals      = new HashMap<SSymbol, DynamicObject>();
    symbolTable  = new HashMap<>();
    blockClasses = new DynamicObject[5];
    structuralProbe = probe;
    primitives = new Primitives(this);
  }

  protected void initializeSystem() {
    // Setup the fields that were not possible to setup before to avoid cyclic initialization dependencies during allocation
    DynamicObject nilObject = Nil.nilObject;
    SObject.setClass(nilObject, nilClass);
    SClass.setSuperclass(SObject.getSOMClass(objectClass), classClass);
    SClass.setSuperclass(metaclassClass, classClass);
    SClass.setSuperclass(SObject.getSOMClass(metaclassClass), SObject.getSOMClass(classClass));

    initializeSystemClassName(); // Need to do this now because before there was no symbol table!

    // Load methods and fields into the system classes
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(objectClass)), objectClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(classClass)), classClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(metaclassClass)), metaclassClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(nilClass)), nilClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(arrayClass)), arrayClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(methodClass)), methodClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(stringClass)), stringClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(characterClass)), characterClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(symbolClass)), symbolClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(integerClass)), integerClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(primitiveClass)), primitiveClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(doubleClass)), doubleClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(booleanClass)), booleanClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(trueClass)), trueClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(falseClass)), falseClass);
    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(systemClass)), systemClass);

    // Load the generic block class
    blockClasses[0] = loadClass(Universe.getCurrent().getSourceForClassName(symbolFor("Block")), null);

    // Setup the true and false objects
    trueObject  = newObject(trueClass);
    falseObject = newObject(falseClass);
    // Load the system class and create an instance of it
    systemObject = newObject(systemClass);

    // Put special objects into the dictionary of globals
    setGlobal("nil",    nilObject);
    setGlobal("true",   trueObject);
    setGlobal("false",  falseObject);
    setGlobal("system", systemObject);

    // Load the remaining block classes
    loadBlockClass(1);
    loadBlockClass(2);
    loadBlockClass(3);
    loadBlockClass(4);

    if (Globals.trueObject != trueObject) {
      Universe.errorExit("Initialization went wrong for class Globals");
    }

    if (null == blockClasses[1]) {
      Universe.errorExit("Initialization went wrong for class Blocks");
    }

    loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(contextClass)), contextClass);

    if (Universe.getCurrent().vmReflectionEnabled()) {
      // Setup the fields that were not possible to setup before to avoid cyclic initialization dependencies
      SReflectiveObject.setEnvironment(Nil.nilObject, Nil.nilObject);
      // SReflectiveObjectEnvInObj.setEnvironment(Nil.nilObject, Nil.nilObject);

      // Load methods and fields into the Mate MOP.
      loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(environmentMO)), environmentMO);
      loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(operationalSemanticsMO)), operationalSemanticsMO);
      loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(messageMO)), messageMO);
      loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(shapeClass)), shapeClass);
      loadClass(Universe.getCurrent().getSourceForClassName(SClass.getName(astNodeClass)), astNodeClass);
    }
  }

  public void initializeSystemClassName() {
    initializeSystemClassName(metaclassClass, "Metaclass");
    initializeSystemClassName(objectClass, "Object");
    initializeSystemClassName(nilClass, "Nil");
    initializeSystemClassName(classClass, "Class");
    initializeSystemClassName(arrayClass, "Array");
    initializeSystemClassName(stringClass, "String");
    initializeSystemClassName(characterClass, "Character");
    initializeSystemClassName(symbolClass, "Symbol");
    initializeSystemClassName(methodClass, "Method");
    initializeSystemClassName(integerClass, "Integer");
    initializeSystemClassName(primitiveClass, "Primitive");
    initializeSystemClassName(doubleClass, "Double");
    initializeSystemClassName(booleanClass, "Boolean");
    initializeSystemClassName(trueClass, "True");
    initializeSystemClassName(falseClass, "False");
    initializeSystemClassName(systemClass, "System");
    initializeSystemClassName(contextClass, "Context");

    if (Universe.getCurrent().vmReflectionEnabled()) {
      initializeSystemClassName(environmentMO, "EnvironmentMO");
      initializeSystemClassName(operationalSemanticsMO, "OperationalSemanticsMO");
      initializeSystemClassName(messageMO, "MessageMO");
      initializeSystemClassName(shapeClass, "Shape");
      initializeSystemClassName(contextClass, "Context");
      initializeSystemClassName(astNodeClass, "ASTNode");
    }
  }

  public void initializeSystemClassName(final DynamicObject klass, final String name) {
    SClass.setName(klass, symbolFor(name));
    SClass.setName(SObject.getSOMClass(klass), symbolFor(name + " class"));
  }

  @TruffleBoundary
  public boolean hasGlobal(final SSymbol name) {
    return globals.containsKey(name);
  }

  @TruffleBoundary
  public DynamicObject getGlobal(final SSymbol name) {
    return globals.get(name);
  }

  public void setGlobal(final String name, final DynamicObject value) {
    setGlobal(symbolFor(name), value);
  }

  @TruffleBoundary
  public void setGlobal(final SSymbol name, final DynamicObject value) {
      globals.put(name, value);
  }

  @TruffleBoundary
  public SSymbol symbolFor(final String string) {
    String interned = string.intern();
    // Lookup the symbol in the symbol table
    SSymbol result = symbolTable.get(interned);
    if (result != null) { return result; }
    return newSymbol(interned);
  }

  private SSymbol newSymbol(final String string) {
    SSymbol result = new SSymbol(string);
    symbolTable.put(string, result);
    return result;
  }

  public static DynamicObject newSystemClass(final DynamicObject superClass) {
    DynamicObject classClassSuperClass;
    if (superClass != Nil.nilObject) {
      classClassSuperClass = SObject.getSOMClass(superClass);
    } else {
      classClassSuperClass =  Nil.nilObject;
    }

    DynamicObject classClass = SClass.createSClass(metaclassClass, new SSymbol("Fake for initialization"), classClassSuperClass,
        SArray.create(new Object[0]), SArray.create(new Object[0]));
    return SClass.createSClass(classClass, new SSymbol("Fake for initialization"), superClass, SArray.create(new Object[0]),
        SArray.create(new Object[0]));
  }

  public static DynamicObject newMetaclassClass(final String name) {
    DynamicObject result = SClass.createWithoutClass(new SSymbol("Fake for initialization"));
    SObject.setClass(result, SClass.createEmptyClass(result, new SSymbol("Fake for initialization")));
    return result;
  }

  /*
   *  If systemClass is null a new class object is created, if not the methods are loaded into systemClass.
   *  Used mainly for system initialization.
   */
  @TruffleBoundary
  public DynamicObject loadClass(final Source source, final DynamicObject systemClass) {
    // Try loading the class from all different paths
    // Load the class from a file and return the loaded class
    DynamicObject result = som.compiler.SourcecodeCompiler.compileClass(source,
        systemClass, this, structuralProbe);
    setGlobal(source.getName(), result);
    loadPrimitives(result);
    loadPrimitives(SObject.getSOMClass(result));
    if (Universe.getCurrent().vmReflectionEnabled()) {
      Universe.getCurrent().mateify(result);
      Universe.getCurrent().mateify(SObject.getSOMClass(result));
    }
    if (Universe.getCurrent().printAST()) {
      Disassembler.dump(SObject.getSOMClass(result));
      Disassembler.dump(result);
    }
    return result;
  }

  private void loadPrimitives(final DynamicObject result) {
    if (primitives.getVMPrimitivesForClassNamed(SClass.getName(result)) != null) {
      for (DynamicObject prim : primitives.getVMPrimitivesForClassNamed(SClass.getName(result))) {
        SClass.addInstancePrimitive(result, prim, false);
      }
    }
  }

  private void loadBlockClass(final int numberOfArguments) {
    // Compute the name of the block class with the given number of
    // arguments
    SSymbol name = symbolFor("Block" + numberOfArguments);
    assert getGlobal(name) == null;

    // Get the block class for blocks with the given number of arguments
    DynamicObject result = loadClass(Universe.getCurrent().getSourceForClassName(name), null);

    // Insert the block class into the dictionary of globals
    setGlobal(name, result);

    blockClasses[numberOfArguments] = result;
  }

  @TruffleBoundary
  public DynamicObject loadShellClass(final String stmt) throws IOException {
    // Load the class from a stream and return the loaded class
    DynamicObject result = som.compiler.SourcecodeCompiler.compileClass(stmt, null, this, structuralProbe);
    if (Universe.getCurrent().printAST()) { Disassembler.dump(result); }
    return result;
  }

  public DynamicObject getBlockClass(final int numberOfArguments) {
    DynamicObject result = blockClasses[numberOfArguments];
    assert result != null || numberOfArguments == 0;
    return result;
  }

  public DynamicObject newObject(final DynamicObject instanceClass) {
    CompilerAsserts.neverPartOfCompilation("Basic create without factory caching");
    DynamicObjectFactory factory = SClass.getFactory(instanceClass);
    return factory.newInstance(layoutClass.buildArguments());
  }

  public DynamicObject getTrueObject()   { return trueObject; }
  public DynamicObject getFalseObject()  { return falseObject; }
  public DynamicObject getTrueClass()   { return trueClass; }
  public DynamicObject getFalseClass()  { return falseClass; }
  public DynamicObject getSystemObject() { return systemObject; }
  public DynamicObject getSystemClass() { return systemClass; }

  public Primitives getPrimitives() { return primitives; }
}
