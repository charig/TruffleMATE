package som.primitives;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import tools.dym.Tags.NewObject;


@GenerateNodeFactory
@ImportStatic(SClass.class)
@Primitive(klass = "Class", selector = "basicNew")
public abstract class NewObjectPrim extends UnaryExpressionNode {
  private static final SObject layoutClass = Universe.getCurrent().getInstanceArgumentsBuilder();

  public NewObjectPrim(final boolean eagWrap, final SourceSection source) {
    super(eagWrap, source);
  }

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
  protected boolean isTaggedWithIgnoringEagerness(final Class<?> tag) {
    if (tag == NewObject.class) {
      return true;
    } else {
      return super.isTaggedWithIgnoringEagerness(tag);
    }
  }
}
