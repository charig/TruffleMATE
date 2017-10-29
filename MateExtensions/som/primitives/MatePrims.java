package som.primitives;

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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

public final class MatePrims {

  @GenerateNodeFactory
  @Primitive(klass = "Shape Class", selector = "newWithFieldsCount:",
             eagerSpecializable = false, mate = true)
  public abstract static class MateNewShapePrim extends BinaryExpressionNode {
    public MateNewShapePrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }

    @Specialization
    public final SAbstractObject doSClass(final DynamicObject receiver, final long fieldsCount) {
      return new SShape((int) fieldsCount);
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "Object", selector = "changeShape:", mate = true)
  public abstract static class MateChangeShapePrim extends BinaryExpressionNode {
    public MateChangeShapePrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }

    @TruffleBoundary
    @Specialization
    public final DynamicObject doSObject(final DynamicObject receiver, final SShape newShape) {
      receiver.setShapeAndResize(receiver.getShape(), newShape.getShape());
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "Object", selector = "shape", mate = true)
  public abstract static class MateGetShapePrim extends UnaryExpressionNode {
    public MateGetShapePrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }

    @Specialization
    public final SShape doSObject(final DynamicObject receiver) {
      return new SShape(receiver.getShape());
    }
  }


  @GenerateNodeFactory
  @Primitive(klass = "Class", selector = "updateShapeForInstancesWith:",
             eagerSpecializable = false, mate = true)
  public abstract static class MateUpdateShapeForInstancesPrim extends BinaryExpressionNode {
    public MateUpdateShapeForInstancesPrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }

    @Specialization
    public final DynamicObject doSObject(final DynamicObject clazz, final SShape shape) {
      // Todo: Take into account that this would not work if the factory was already compiled in a fast path.
      SClass.setInstancesFactory(clazz, shape.getShape().createFactory());
      return clazz;
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "Class", selector = "getShapeForInstances",
             eagerSpecializable = false, mate = true)
  public abstract static class MateGetShapeForInstancesPrim extends UnaryExpressionNode {
    public MateGetShapeForInstancesPrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }

    @Specialization
    public final SShape doSObject(final DynamicObject clazz) {
      return new SShape(SClass.getFactory(clazz).getShape());
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "Object", selector = "installEnvironment:", mate = true)
  public abstract static class InstallEnvironmentPrim extends BinaryExpressionNode {
    public InstallEnvironmentPrim(final boolean eagWrap, final SourceSection source) {
      super(eagWrap, source);
    }

    @Specialization(guards = "receiverIsSystemObject(receiver)")
    public final DynamicObject doSystemObject(final DynamicObject receiver, final DynamicObject environment) {
      Universe.getCurrent().setGlobalEnvironment(environment);
      return environment;
    }

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
  @Primitive(klass = "Object", selector = "setHiddenField:value:",
             eagerSpecializable = false, mate = true)
  public abstract static class MateSetHiddenFieldPrim extends TernaryExpressionNode {
    public MateSetHiddenFieldPrim(final boolean eagWrap, final SourceSection source) {
      super(eagWrap, source);
    }

    @Specialization
    public final DynamicObject doSObject(final DynamicObject receiver, final MockJavaObject key,
        final DynamicObject value) {
      receiver.set(key.getMockedObject(), value);
      return value;
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "Object", selector = "getField:",
             eagerSpecializable = false, mate = true)
  public abstract static class MateGetFieldPrim extends BinaryExpressionNode {
    public MateGetFieldPrim(final boolean eagWrap, final SourceSection source) {
      super(eagWrap, source);
    }

    @Specialization
    public final Object doSObject(final DynamicObject receiver, final MockJavaObject key) {
      // TODO: We should create a property with Nil as defaultValue.
      Object value = receiver.get(key.getMockedObject());
      return value == null ? Nil.nilObject : value;
    }
  }
}
