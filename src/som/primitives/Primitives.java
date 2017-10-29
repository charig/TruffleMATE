/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
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

package som.primitives;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import som.VmSettings;
import som.compiler.MethodGenerationContext;
import som.interpreter.Primitive;
import som.interpreter.SArguments;
import som.interpreter.SomLanguage;
import som.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.EagerlySpecializableNode;
import som.interpreter.nodes.nary.ExpressionWithTagsNode;
import som.interpreter.nodes.specialized.AndMessageNodeFactory;
import som.interpreter.nodes.specialized.IfMessageNodeFactory;
import som.interpreter.nodes.specialized.IfTrueIfFalseMessageNodeFactory;
import som.interpreter.nodes.specialized.IntDownToDoMessageNodeFactory;
import som.interpreter.nodes.specialized.IntToByDoMessageNodeFactory;
import som.interpreter.nodes.specialized.IntToDoMessageNodeFactory;
import som.interpreter.nodes.specialized.NotMessageNodeFactory;
import som.interpreter.nodes.specialized.OrMessageNodeFactory;
import som.interpreter.nodes.specialized.whileloops.WhilePrimitiveNodeFactory;
import som.interpreter.nodes.specialized.whileloops.WhileWithStaticBlocksNode.WhileWithStaticBlocksNodeFactory;
import som.primitives.Primitive.NoChild;
import som.primitives.arithmetic.AdditionPrimFactory;
import som.primitives.arithmetic.BitAndPrimFactory;
import som.primitives.arithmetic.BitXorPrimFactory;
import som.primitives.arithmetic.CosPrimFactory;
import som.primitives.arithmetic.DividePrimFactory;
import som.primitives.arithmetic.DoubleDivPrimFactory;
import som.primitives.arithmetic.GreaterThanOrEqualPrimFactory;
import som.primitives.arithmetic.GreaterThanPrimFactory;
import som.primitives.arithmetic.LessThanOrEqualPrimFactory;
import som.primitives.arithmetic.LessThanPrimFactory;
import som.primitives.arithmetic.ModuloPrimFactory;
import som.primitives.arithmetic.MultiplicationPrimFactory;
import som.primitives.arithmetic.RemainderPrimFactory;
import som.primitives.arithmetic.SinPrimFactory;
import som.primitives.arithmetic.SqrtPrimFactory;
import som.primitives.arithmetic.SubtractionPrimFactory;
import som.primitives.arrays.AtPrimFactory;
import som.primitives.arrays.AtPutPrimFactory;
import som.primitives.arrays.CopyPrimFactory;
import som.primitives.arrays.DoIndexesPrimFactory;
import som.primitives.arrays.DoPrimFactory;
import som.primitives.arrays.NewPrimFactory;
import som.primitives.arrays.PutAllNodeFactory;
import som.primitives.reflection.PerformInSuperclassPrimFactory;
import som.primitives.reflection.PerformPrimFactory;
import som.primitives.reflection.PerformWithArgumentsInSuperclassPrimFactory;
import som.primitives.reflection.PerformWithArgumentsPrimFactory;
import som.vm.ObjectMemory;
import som.vm.Universe;
import som.vmobjects.SClass;
import som.vmobjects.SSymbol;

public class Primitives {
  private Map<SSymbol, Specializer<? extends ExpressionNode>>  eagerPrimitives;
  private Map<SSymbol, List<DynamicObject>> vmPrimitives;

  @SuppressWarnings("unchecked")
  public Specializer<EagerlySpecializableNode> getEagerSpecializer(final SSymbol selector,
      final Object[] arguments, final ExpressionNode[] argumentNodes) {
    Specializer<? extends ExpressionNode> specializer = eagerPrimitives.get(selector);
    if (specializer != null && specializer.mate() && !Universe.getCurrent().vmReflectionEnabled()) {
      return null;
    }
    if (specializer != null && specializer.matches(arguments, argumentNodes)) {
      return (Specializer<EagerlySpecializableNode>) specializer;
    }
    return null;
  }

  public List<DynamicObject> getVMPrimitivesForClassNamed(final SSymbol classname) {
    return vmPrimitives.get(classname);
  }

  public Primitives(final ObjectMemory om) {
    eagerPrimitives = new HashMap<>();
    vmPrimitives = new HashMap<>();
    initialize(om);
  }

  /**
   * A Specializer defines when a node can be used as a eager primitive and how
   * it is to be instantiated.
   */
  public static class Specializer<T> {
    protected Universe vm;
    protected final som.primitives.Primitive prim;
    protected final NodeFactory<T> fact;
    private final NodeFactory<? extends ExpressionNode> extraChildFactory;

    @SuppressWarnings("unchecked")
    public Specializer(final som.primitives.Primitive prim, final NodeFactory<T> fact) {
      this.prim = prim;
      this.fact = fact;

      if (prim.extraChild() == NoChild.class) {
        extraChildFactory = null;
      } else {
        try {
          extraChildFactory = (NodeFactory<? extends ExpressionNode>) prim.extraChild().getMethod("getInstance").invoke(null);
        } catch (IllegalAccessException | IllegalArgumentException
            | InvocationTargetException | NoSuchMethodException
            | SecurityException e) {
          throw new RuntimeException(e);
        }
      }
    }

    public void initialize(final Universe vm) {
      // assert vm != null;
      this.vm = vm;
    }

    public boolean noWrapper() {
      return prim.noWrapper();
    }

    public boolean mate() {
      return prim.mate();
    }

    public boolean matches(final Object[] args, final ExpressionNode[] argNodes) {
      if (prim.disabled() && VmSettings.DYNAMIC_METRICS) {
        return false;
      }

      if (prim.receiverType().length == 0) {
        // no constraints, so, it matches
        return true;
      }

      for (Class<?> c : prim.receiverType()) {
        if (c.isInstance(args[0])) {
          return true;
        }
      }
      return false;
    }

    public T create(final Object[] arguments,
        final ExpressionNode[] argNodes, final SourceSection section,
        final boolean eagerWrapper, final Frame frame) {
      assert arguments == null || arguments.length == argNodes.length;
      int numArgs = argNodes.length + 2 +
          (extraChildFactory != null ? 1 : 0) +
          (prim.requiresArguments() ? 1 : 0) +
          (prim.requiresExecutionLevel() ? 1 : 0);

      Object[] ctorArgs = new Object[numArgs];
      ctorArgs[0] = eagerWrapper;
      ctorArgs[1] = section;
      int offset = 2;

      if (prim.requiresContext()) {
        assert vm != null : "vm i.e. the context was not yet initialized";
        ctorArgs[offset] = vm;
        offset += 1;
      }

      if (prim.requiresArguments()) {
        ctorArgs[offset] = arguments;
        offset += 1;
      }

      if (prim.requiresExecutionLevel()) {
        ctorArgs[offset] = SArguments.getExecutionLevel(frame);
        offset += 1;
      }

      for (int i = 0; i < argNodes.length; i += 1) {
        ctorArgs[i + offset] = eagerWrapper ? null : argNodes[i];
      }

      if (extraChildFactory != null) {
        ctorArgs[ctorArgs.length - 1] = extraChildFactory.createNode(false, null, null);
      }
      return fact.createNode(ctorArgs);
    }
  }

  public static DynamicObject constructPrimitive(final SSymbol signature,
      final Specializer<? extends ExpressionNode> specializer) {
    CompilerAsserts.neverPartOfCompilation("constructPrimitive");
    int numArgs = signature.getNumberOfSignatureArguments();

    Source s = SomLanguage.getSyntheticSource("primitive", specializer.fact.getClass().getSimpleName());

    MethodGenerationContext mgen = new MethodGenerationContext(null);
    ExpressionWithTagsNode[] args = new ExpressionWithTagsNode[numArgs];
    for (int i = 0; i < numArgs; i++) {
      args[i] = new LocalArgumentReadNode(i, s.createSection(1));
    }

    ExpressionNode primNode = specializer.create(null, args, s.createSection(1), false, null);

    Primitive primMethodNode = new Primitive(primNode, mgen.getCurrentLexicalScope().getFrameDescriptor(),
        (ExpressionNode) primNode.deepCopy(), null);
    DynamicObject primitive = Universe.newMethod(signature, primMethodNode, true, new DynamicObject[0]);
    primMethodNode.setMethod(primitive);
    return primitive;
  }

  public static DynamicObject constructEmptyPrimitive(final SSymbol signature) {
    CompilerAsserts.neverPartOfCompilation("constructEmptyPrimitive");
    MethodGenerationContext mgen = new MethodGenerationContext(null);

    ExpressionWithTagsNode primNode = EmptyPrim.create(new LocalArgumentReadNode(0, null));
    Primitive primMethodNode = new Primitive(primNode, mgen.getCurrentLexicalScope().getFrameDescriptor(),
        (ExpressionWithTagsNode) primNode.deepCopy(), null);
    DynamicObject method = Universe.newMethod(signature, primMethodNode, true, new DynamicObject[0]);
    primMethodNode.setMethod(method);
    return method;
  }

  public static DynamicObject installPrimitive(final SSymbol signature,
      final Specializer<? extends ExpressionNode> specializer, final DynamicObject holder) {
    DynamicObject prim = constructPrimitive(signature, specializer);
    SClass.addInstancePrimitive(holder, prim, false);
    return prim;
  }

  private static som.primitives.Primitive[] getPrimitiveAnnotation(
      final NodeFactory<? extends ExpressionNode> primFact) {
    Class<?> nodeClass = primFact.getNodeClass();
    return nodeClass.getAnnotationsByType(som.primitives.Primitive.class);
  }

  /**
   * Setup the lookup data structures for vm primitive registration as well as
   * eager primitive replacement.
   */
  private void initialize(final ObjectMemory om) {
    /* I send new because I am not using primitives requiring context.
     A refactoring for the polyglot engine initialization is still needed to pass
     the context properly */
    initializeSpecializers(null);

    for (Entry<NodeFactory<? extends ExpressionNode>, som.primitives.Primitive[]> e : primitives.entrySet()) {
      for (som.primitives.Primitive prim : e.getValue()) {
        Specializer<? extends ExpressionNode> specializer = getSpecializer(prim, e.getKey());
        String classname = prim.klass();
        if (!("".equals(classname))) {
          SSymbol klass = om.symbolFor(classname);
          SSymbol signature = om.symbolFor(prim.selector());
          List<DynamicObject> content;
          if (vmPrimitives.containsKey(klass)) {
            content = vmPrimitives.get(klass);
          } else {
            content = new ArrayList<DynamicObject>();
            vmPrimitives.put(klass, content);
          }
          content.add(constructPrimitive(signature, specializer));
        }

        if ((!("".equals(prim.selector())) && prim.eagerSpecializable())) {
          SSymbol msgSel = om.symbolFor(prim.selector());
          assert !eagerPrimitives.containsKey(msgSel) : "clash of selectors and eager specialization";
          eagerPrimitives.put(msgSel, specializer);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> Specializer<T> getSpecializer(final som.primitives.Primitive prim, final NodeFactory<T> factory) {
    try {
      return prim.specializer().
          getConstructor(som.primitives.Primitive.class, NodeFactory.class).
          newInstance(prim, factory);
    } catch (InstantiationException | IllegalAccessException |
        IllegalArgumentException | InvocationTargetException |
        NoSuchMethodException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private static final Map<NodeFactory<? extends ExpressionNode>, som.primitives.Primitive[]> primitives;
  private static final Map<som.primitives.Primitive, Specializer<? extends ExpressionNode>> specializers;

  static {
    primitives = new HashMap<>();
    specializers = new HashMap<>();

    findAllPrimitivesAndSpecializers();
  }

  private static void findAllPrimitivesAndSpecializers() {
    List<NodeFactory<? extends ExpressionNode>> primFacts = getFactories();

    for (NodeFactory<? extends ExpressionNode> primFact : primFacts) {
      som.primitives.Primitive[] prims = getPrimitiveAnnotation(primFact);
      if (prims != null) {
        primitives.put(primFact, prims);
        for (som.primitives.Primitive prim : prims) {
          @SuppressWarnings("rawtypes")
          Class<? extends Specializer> spezCls = prim.specializer();
          assert !specializers.containsKey(prim);

          Specializer<? extends ExpressionNode> spez = instantiateSpecializer(primFact, prim, spezCls);
          specializers.put(prim, spez);
        }
      }
    }
  }

  private static void initializeSpecializers(final Universe vm) {
    for (Specializer<?> spez : specializers.values()) {
      spez.initialize(vm);
    }
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static Specializer<? extends ExpressionNode> instantiateSpecializer(
      final NodeFactory<? extends ExpressionNode> primFact,
      final som.primitives.Primitive prim,
      final Class<? extends Specializer> spezCls) {
    try {
      return spezCls.
          getConstructor(som.primitives.Primitive.class, NodeFactory.class).
          newInstance(prim, primFact);
    } catch (InstantiationException | IllegalAccessException |
        IllegalArgumentException | InvocationTargetException |
        NoSuchMethodException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<NodeFactory<? extends ExpressionNode>> getFactories() {
    List<NodeFactory<? extends ExpressionNode>> allFactories = new ArrayList<>();
    allFactories.addAll(BlockPrimsFactory.getFactories());
    allFactories.addAll(CharacterPrimsFactory.getFactories());
    allFactories.addAll(ClassPrimsFactory.getFactories());
    allFactories.addAll(CompilationPrimsFactory.getFactories());
    allFactories.addAll(ContextPrimsFactory.getFactories());
    allFactories.addAll(DoublePrimsFactory.getFactories());
    allFactories.addAll(ExceptionsPrimsFactory.getFactories());
    allFactories.addAll(FilePluginPrimsFactory.getFactories());
    allFactories.addAll(IntegerPrimsFactory.getFactories());
    allFactories.addAll(MatePrimsFactory.getFactories());
    allFactories.addAll(MethodPrimsFactory.getFactories());
    allFactories.addAll(ObjectPrimsFactory.getFactories());
    allFactories.addAll(ShapePrimsFactory.getFactories());
    allFactories.addAll(StringPrimsFactory.getFactories());
    allFactories.addAll(SystemPrimsFactory.getFactories());
    allFactories.addAll(WhilePrimitiveNodeFactory.getFactories());
    // allFactories.addAll(ObjectSystemPrimsFactory.getFactories());

    allFactories.add(AdditionPrimFactory.getInstance());
    allFactories.add(AndMessageNodeFactory.getInstance());
    allFactories.add(AsStringPrimFactory.getInstance());
    allFactories.add(AtPrimFactory.getInstance());
    allFactories.add(AtPutPrimFactory.getInstance());
    allFactories.add(BitAndPrimFactory.getInstance());
    allFactories.add(BitXorPrimFactory.getInstance());
    allFactories.add(CopyPrimFactory.getInstance());
    allFactories.add(CosPrimFactory.getInstance());
    allFactories.add(DividePrimFactory.getInstance());
    allFactories.add(DoIndexesPrimFactory.getInstance());
    allFactories.add(DoPrimFactory.getInstance());
    allFactories.add(DoubleDivPrimFactory.getInstance());
    allFactories.add(EqualsEqualsPrimFactory.getInstance());
    allFactories.add(EqualsPrimFactory.getInstance());
    allFactories.add(GlobalPrimFactory.getInstance());
    allFactories.add(GreaterThanPrimFactory.getInstance());
    allFactories.add(GreaterThanOrEqualPrimFactory.getInstance());
    allFactories.add(IfMessageNodeFactory.getInstance());
    allFactories.add(IfTrueIfFalseMessageNodeFactory.getInstance());
    allFactories.add(InvokeOnPrimFactory.getInstance());
    allFactories.add(IntToDoMessageNodeFactory.getInstance());
    allFactories.add(IntDownToDoMessageNodeFactory.getInstance());
    allFactories.add(IntToByDoMessageNodeFactory.getInstance());
    allFactories.add(LengthPrimFactory.getInstance());
    allFactories.add(LessThanOrEqualPrimFactory.getInstance());
    allFactories.add(LessThanPrimFactory.getInstance());
    allFactories.add(ModuloPrimFactory.getInstance());
    allFactories.add(MultiplicationPrimFactory.getInstance());
    allFactories.add(NewPrimFactory.getInstance());
    allFactories.add(NewObjectPrimFactory.getInstance());
    allFactories.add(NotMessageNodeFactory.getInstance());
    allFactories.add(OrMessageNodeFactory.getInstance());
    allFactories.add(PerformInSuperclassPrimFactory.getInstance());
    allFactories.add(PerformPrimFactory.getInstance());
    allFactories.add(PerformWithArgumentsInSuperclassPrimFactory.getInstance());
    allFactories.add(PerformWithArgumentsPrimFactory.getInstance());
    allFactories.add(PutAllNodeFactory.getInstance());
    allFactories.add(RemainderPrimFactory.getInstance());
    // allFactories.add(ExpPrimFactory.getInstance());
    // allFactories.add(LogPrimFactory.getInstance());
    allFactories.add(SinPrimFactory.getInstance());
    allFactories.add(SqrtPrimFactory.getInstance());
    allFactories.add(SubtractionPrimFactory.getInstance());
    // allFactories.add(ToArgumentsArrayNodeFactory.getInstance());
    allFactories.add(UnequalsPrimFactory.getInstance());
    allFactories.add(new WhileWithStaticBlocksNodeFactory());
    return allFactories;
  }
}
