package som.primitives;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vm.constants.Globals;
import som.vm.constants.Nil;
import som.vmobjects.MockJavaObject;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SClass;
import som.vmobjects.SReflectiveObject;
import som.vmobjects.SShape;

public final class MatePrims {

  @GenerateNodeFactory
  @Primitive(className = "Shape Class", primitive = "newWithFieldsCount:", selector = "newWithFieldsCount:",
             mate = true)
  public abstract static class MateNewShapePrim extends BinaryExpressionNode {
    @Specialization
    public final SAbstractObject doSClass(final DynamicObject receiver, final long fieldsCount) {
      return new SShape((int) fieldsCount);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", selector = "changeShape:", primitive = "changeShape:", mate = true)
  public abstract static class MateChangeShapePrim extends BinaryExpressionNode {
    @TruffleBoundary
    @Specialization
    public final DynamicObject doSObject(final DynamicObject receiver, final SShape newShape) {
      receiver.setShapeAndResize(receiver.getShape(), newShape.getShape());
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "shape", selector = "shape", mate = true)
  public abstract static class MateGetShapePrim extends UnaryExpressionNode {
    @Specialization
    public final SShape doSObject(final DynamicObject receiver) {
      return new SShape(receiver.getShape());
    }
  }


  @GenerateNodeFactory
  @Primitive(className = "Class", primitive = "updateShapeForInstancesWith:", mate = true)
  public abstract static class MateUpdateShapeForInstancesPrim extends BinaryExpressionNode {
    @Specialization
    public final DynamicObject doSObject(final DynamicObject clazz, final SShape shape) {
      // Todo: Take into account that this would not work if the factory was already compiled in a fast path.
      SClass.setInstancesFactory(clazz, shape.getShape().createFactory());
      return clazz;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Class", primitive = "getShapeForInstances", mate = true)
  public abstract static class MateGetShapeForInstancesPrim extends UnaryExpressionNode {
    @Specialization
    public final SShape doSObject(final DynamicObject clazz) {
      return new SShape(SClass.getFactory(clazz).getShape());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "installEnvironment:", selector = "installEnvironment:", mate = true)
  public abstract static class InstallEnvironmentPrim extends BinaryExpressionNode {
    @Specialization(guards = "receiverIsSystemObject(receiver)")
    public final DynamicObject doSystemObject(final DynamicObject receiver, final DynamicObject environment) {
      Universe.getCurrent().setGlobalEnvironment(environment);
      return environment;
    }

    @TruffleBoundary
    @Specialization
    public final Object doSObject(final DynamicObject receiver, final DynamicObject environment) {
      SReflectiveObject.setEnvironment(receiver, environment);
      return receiver;
    }

    public static final boolean receiverIsSystemObject(final DynamicObject receiver) {
      return receiver == Globals.systemObject;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "setHiddenField:value:", mate = true)
  public abstract static class MateSetHiddenFieldPrim extends TernaryExpressionNode {
    @Specialization
    public final DynamicObject doSObject(final DynamicObject receiver, final MockJavaObject key,
        final DynamicObject value) {
      receiver.set(key.getMockedObject(), value);
      return value;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "getField:", mate = true)
  public abstract static class MateGetFieldPrim extends BinaryExpressionNode {
    @Specialization
    public final Object doSObject(final DynamicObject receiver, final MockJavaObject key) {
      // TODO: We should create a property with Nil as defaultValue.
      Object value = receiver.get(key.getMockedObject());
      return value == null ? Nil.nilObject : value;
    }
  }
}
