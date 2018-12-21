package som.primitives;

import java.util.EnumSet;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.QuaternaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.constants.Nil;
import som.vmobjects.MockJavaObject;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SArray;
import som.vmobjects.SReflectiveObjectLayoutImpl.SReflectiveObjectType;
import som.vmobjects.SShape;
import som.vmobjects.SSymbol;


public class ShapePrims {
  @GenerateNodeFactory
  @Primitive(className = "Shape class", primitive = "newWithFieldsCount:", mate = true)
  public abstract static class MateNewShapePrim extends BinaryExpressionNode {
    @Specialization
    public final SAbstractObject doSClass(final DynamicObject receiver, final long fieldsCount) {
      return new SShape((int) fieldsCount);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Shape", primitive = "fieldsCount", mate = true)
  public abstract static class MateShapeFieldsCountPrim extends UnaryExpressionNode {
    @Specialization
    public final long doSShape(final SShape shape) {
      return shape.getShape().getPropertyCount();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Shape", primitive = "installEnvironment:", mate = true)
  public abstract static class MateInstallEnvironmentInShapePrim extends BinaryExpressionNode {
    @Specialization
    public final SShape doSObject(final SShape shape, final DynamicObject environment) {
      return new SShape(
          shape.getShape().changeType(
              ((SReflectiveObjectType) shape.getShape().getObjectType()).setEnvironment(environment)));
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Shape", primitive = "installClass:", mate = true)
  public abstract static class MateInstallClassInShapePrim extends BinaryExpressionNode {
    @Specialization
    public final SShape doSObject(final SShape shape, final DynamicObject className) {
      return new SShape(
          shape.getShape().changeType(
              ((SReflectiveObjectType) shape.getShape().getObjectType()).setKlass(className)));
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Shape", primitive = "define:final:hidden:", mate = true)
  public abstract static class DefinePropertyInShapePrim extends QuaternaryExpressionNode {
    @Specialization
    public final SArray doSSymbol(final SShape shape, final SSymbol keyValue, final boolean isFinal, final boolean hidden) {
      Object key;
      if (hidden) {
        key = new HiddenKey(keyValue.getString());
      } else {
        key = keyValue.getString();
      }
      Property environment = Property.create(key,
          shape.getShape().allocator().locationForType(com.oracle.truffle.api.object.DynamicObject.class, EnumSet.of(LocationModifier.NonNull, LocationModifier.Final)),
          0);
      return SArray.create(new Object[]{new MockJavaObject(key, Nil.nilObject),
          new SShape(shape.getShape().addProperty(environment))});
    }

    @Specialization
    public final SArray doLong(final SShape shape, final long key, final boolean isFinal, final boolean hidden) {
      // If key is an instance variable index it is not hidden nor final
      Property environment = Property.create(key,
          shape.getShape().allocator().locationForType(com.oracle.truffle.api.object.DynamicObject.class,
              EnumSet.of(LocationModifier.NonNull)),
          0);
      return SArray.create(new Object[]{key,
          new SShape(shape.getShape().addProperty(environment))});
    }
  }
}
