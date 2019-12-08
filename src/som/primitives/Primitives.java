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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.PrimitiveLoader;
import bd.primitives.Specializer;
import som.compiler.MethodGenerationContext;
import som.interpreter.SomLanguage;
import som.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.ExpressionWithTagsNode;
import som.interpreter.nodes.specialized.AndMessagesNodesFactory.AndMessageNodeFactory;
import som.interpreter.nodes.specialized.IfMessageNodesFactory.IfFalseMessageNodeFactory;
import som.interpreter.nodes.specialized.IfTrueIfFalseMessageNodeFactory;
import som.interpreter.nodes.specialized.IntDownToDoMessageNodeFactory;
import som.interpreter.nodes.specialized.IntToByDoMessageNodeFactory;
import som.interpreter.nodes.specialized.IntToDoMessageNodeFactory;
import som.interpreter.nodes.specialized.NotMessageNodeFactory;
import som.interpreter.nodes.specialized.OrMessagesNodesFactory.OrMessageNodeFactory;
import som.interpreter.nodes.specialized.whileloops.WhileWithStaticBlocksNode.WhileWithStaticBlocksNodeFactory;
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
import som.vm.Symbols;
import som.vm.Universe;
import som.vmobjects.SSymbol;

public class Primitives extends PrimitiveLoader<Universe, ExpressionNode, SSymbol> {
  private EconomicMap<SSymbol, List<DynamicObject>> vmPrimitives;
  private final SomLanguage language;

  public Primitives(final SomLanguage lang) {
    super(Symbols.PROVIDER);
    vmPrimitives = EconomicMap.create();
    this.language = lang;
    initialize(Universe.getCurrent().vmReflectionEnabled());
  }


  public List<DynamicObject> getVMPrimitivesForClassNamed(final SSymbol classname) {
    return vmPrimitives.get(classname);
  }


  public static DynamicObject constructPrimitive(final SSymbol signature,
      final Specializer<Universe, ExpressionNode, SSymbol> specializer, final SomLanguage language) {
    CompilerAsserts.neverPartOfCompilation("constructing primitives should only happen when bootstrapping");
    int numArgs = signature.getNumberOfSignatureArguments();
    Source s = null;
    try {
      s = SomLanguage.getSyntheticSourceTruffle("primitive", specializer.getName());
    } catch (IOException e) {
      e.printStackTrace();
    }
    SourceSection source = s.createSection(1);

    MethodGenerationContext mgen = new MethodGenerationContext(null, language);
    ExpressionWithTagsNode[] args = new ExpressionWithTagsNode[numArgs];
    for (int i = 0; i < numArgs; i++) {
      args[i] = new LocalArgumentReadNode(i).initialize(source);
    }

    ExpressionNode primNode = specializer.create(null, args, source, false, Universe.getCurrent());

    som.interpreter.Primitive primMethodNode = new som.interpreter.Primitive(primNode, mgen.getCurrentLexicalScope().getFrameDescriptor(),
        (ExpressionNode) primNode.deepCopy(), null, language);
    DynamicObject primitive = Universe.newMethod(signature, primMethodNode, true, new DynamicObject[0]);
    primMethodNode.setMethod(primitive);
    return primitive;
  }

  public static DynamicObject constructEmptyPrimitive(final SSymbol signature, final SomLanguage language) {
    CompilerAsserts.neverPartOfCompilation("constructEmptyPrimitive");
    MethodGenerationContext mgen = new MethodGenerationContext(null, language);

    ExpressionWithTagsNode primNode = EmptyPrim.create();
    som.interpreter.Primitive primMethodNode = new som.interpreter.Primitive(primNode, mgen.getCurrentLexicalScope().getFrameDescriptor(),
        (ExpressionWithTagsNode) primNode.deepCopy(), null, language);
    DynamicObject method = Universe.newMethod(signature, primMethodNode, true, new DynamicObject[0]);
    primMethodNode.setMethod(method);
    return method;
  }


  @Override
  protected void registerPrimitive(final Specializer<Universe, ExpressionNode, SSymbol> specializer) {
    String classname = specializer.getPrimitive().className();

    if (!("".equals(classname))) {
      SSymbol klass = Symbols.symbolFor(classname);
      SSymbol signature = Symbols.symbolFor(specializer.getPrimitive().primitive());
      List<DynamicObject> content;
      if (vmPrimitives.containsKey(klass)) {
        content = vmPrimitives.get(klass);
      } else {
        content = new ArrayList<DynamicObject>();
        vmPrimitives.put(klass, content);
      }
      content.add(constructPrimitive(signature, specializer, language));
    }
  }


  private static List<NodeFactory<? extends ExpressionNode>> getFactories() {
    List<NodeFactory<? extends ExpressionNode>> allFactories = new ArrayList<>();
    allFactories.addAll(BlockPrimsFactory.getFactories());
    allFactories.addAll(CharacterPrimsFactory.getFactories());
    allFactories.addAll(ClassPrimsFactory.getFactories());
    allFactories.addAll(ContextPrimsFactory.getFactories());
    allFactories.addAll(DoublePrimsFactory.getFactories());
    allFactories.addAll(ExceptionsPrimsFactory.getFactories());
    allFactories.addAll(FilePluginPrimsFactory.getFactories());
    allFactories.addAll(IntegerPrimsFactory.getFactories());
    allFactories.addAll(MatePrimsFactory.getFactories());
    allFactories.addAll(MethodPrimsFactory.getFactories());
    allFactories.addAll(NewObjectPrimsFactory.getFactories());
    allFactories.addAll(ObjectPrimsFactory.getFactories());
    allFactories.addAll(ShapePrimsFactory.getFactories());
    allFactories.addAll(StringPrimsFactory.getFactories());
    allFactories.addAll(SystemPrimsFactory.getFactories());
    // allFactories.addAll(WhilePrimitiveNodeFactory.getFactories());
    // allFactories.addAll(ObjectSystemPrimsFactory.getFactories());

    allFactories.add(AdditionPrimFactory.getInstance());
    allFactories.add(AndMessageNodeFactory.getInstance());
    allFactories.add(AsStringPrimFactory.getInstance());
    allFactories.add(som.primitives.arrays.AsStringPrimFactory.getInstance());
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
    allFactories.add(IfFalseMessageNodeFactory.getInstance());
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

  @Override
  protected List<Specializer<Universe, ExpressionNode, SSymbol>> getSpecializers() {
    ArrayList<Specializer<Universe, ExpressionNode, SSymbol>> specializers = new ArrayList<Specializer<Universe, ExpressionNode, SSymbol>>();
    addAll(specializers, getFactories());
    return specializers;
  }
}
