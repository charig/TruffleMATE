package som.primitives;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import tools.dym.Tags.NewObject;

public class NewObjectPrims {

  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  @Primitive(className = "Class", primitive = "basicNew", selector = "basicNew")
  public abstract static class NewObjectPrim extends UnaryExpressionNode {

    @Specialization(guards = "receiver == cachedClass", assumptions = "shape.getValidAssumption()")
    public final DynamicObject cachedClass(final DynamicObject receiver,
        @Cached("receiver") final DynamicObject cachedClass,
        @Cached("getFactory(cachedClass)") final DynamicObjectFactory factory,
        @Cached("factory.getShape()") final Shape shape,
        @Cached(value = "initArgsFor(shape)", dimensions = 1) final Object[] arguments) {
      return factory.newInstance(arguments);
    }

    @TruffleBoundary
    @Specialization(replaces = "cachedClass")
    public DynamicObject uncached(final DynamicObject receiver) {
      return SClass.getFactory(receiver).newInstance(initArgsFor(SClass.getFactory(receiver).getShape()));
    }

    @TruffleBoundary
    protected static Object[] initArgsFor(final Shape shape) {
      Object[] defaultArgs = Universe.getCurrent().getInstanceArgumentsBuilder().buildArguments();
      Object[] arguments = new Object[shape.getPropertyCount() + defaultArgs.length];
      int i = 0;
      for (Object arg: defaultArgs) {
        arguments[i] = arg;
        i++;
      }
      for (Property property : shape.getProperties()) {
        arguments[i] = Universe.getCurrent().defaultFieldValue(property);
        i++;
      }
      return arguments;
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

  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  @Primitive(className = "Class", primitive = "basicNew:", selector = "basicNew:")
  public abstract static class NewObjectWithEnvironmentPrim extends BinaryExpressionNode {
    private static final SObject layoutClass = Universe.getCurrent().getInstanceArgumentsBuilder();

    @Specialization(guards = "receiver == cachedClass")
    public final DynamicObject cachedClass(final DynamicObject receiver,
        final DynamicObject environment,
        @Cached("receiver") final DynamicObject cachedClass,
        @Cached("environment") final DynamicObject cachedEnvironment,
        @Cached("getFactory(cachedClass)") final DynamicObjectFactory factory) {
      return factory.newInstance(layoutClass.buildArguments());
    }

    @TruffleBoundary
    @Specialization(replaces = "cachedClass")
    public DynamicObject uncached(final DynamicObject receiver, final DynamicObject environment) {
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
}
