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
import com.oracle.truffle.api.object.TypedLocation;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import tools.dym.Tags.NewObject;

public class NewObjectPrims {

  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  @Primitive(className = "Class", primitive = "basicNew", selector = "basicNew")
  public abstract static class NewObjectPrim extends UnaryExpressionNode {
    private static final SObject layoutClass = Universe.getCurrent().getInstanceArgumentsBuilder();

    @Specialization(guards = "receiver == cachedClass", assumptions = "shape.getValidAssumption()")
    public final DynamicObject cachedClass(final DynamicObject receiver,
        @Cached("receiver") final DynamicObject cachedClass,
        @Cached("getFactory(cachedClass)") final DynamicObjectFactory factory,
        @Cached("factory.getShape()") final Shape shape,
        @Cached("initArgsFor(shape)") final Object[] arguments) {
      return factory.newInstance(arguments);
    }

    @TruffleBoundary
    @Specialization(replaces = "cachedClass")
    public DynamicObject uncached(final DynamicObject receiver) {
      return SClass.getFactory(receiver).newInstance(initArgsFor(SClass.getFactory(receiver).getShape()));
    }

    protected static Object[] initArgsFor(final Shape shape) {
      Object[] defaultArgs = Universe.getCurrent().getInstanceArgumentsBuilder().buildArguments();
      Object[] arguments = new Object[shape.getPropertyCount() + defaultArgs.length];
      int i = 0;
      for (Object arg: defaultArgs) {
        arguments[i] = arg;
        i++;
      }
      for (Property property : shape.getProperties()) {
        TypedLocation location = (TypedLocation) property.getLocation();
        if (location.getType() == Object.class) {
          arguments[i] = Nil.nilObject;
        } else if (location.getType() == double.class) {
            arguments[i] = Double.NaN;
        } else if (location.getType() == int.class) {
          arguments[i] = Integer.MIN_VALUE;
        } else if (location.getType() == long.class) {
          arguments[i] = Long.MIN_VALUE;
        } else {
          arguments[i] = Boolean.FALSE;
        }
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
