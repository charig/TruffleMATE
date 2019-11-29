package som.primitives;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import tools.dym.Tags.NewObject;


@GenerateNodeFactory
@ImportStatic(SClass.class)
@ReportPolymorphism
@Primitive(className = "Class", primitive = "basicNew", selector = "basicNew")
public abstract class NewObjectPrim extends UnaryExpressionNode {
  private static final SObject layoutClass = Universe.getCurrent().getInstanceArgumentsBuilder();

  @Specialization(guards = "receiver == cachedClass")
  public final DynamicObject cachedClass(final DynamicObject receiver,
      @Cached("receiver") final DynamicObject cachedClass,
      @Cached("getFactory(cachedClass)") final DynamicObjectFactory factory) {
    return factory.newInstance(layoutClass.buildArguments());
  }

  @TruffleBoundary
  @Specialization(replaces = "cachedClass")
  public DynamicObject uncached(final DynamicObject receiver) {
    return SClass.getFactory(receiver).newInstance(layoutClass.buildArguments());
  }

  @Override
  protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
    if (tag == NewObject.class) {
      return true;
    } else {
      return super.hasTagIgnoringEagerness(tag);
    }
  }
}
