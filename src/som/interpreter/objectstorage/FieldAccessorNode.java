package som.interpreter.objectstorage;


import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;

import som.interpreter.ReflectiveNode;
import som.interpreter.objectstorage.FieldAccessorNodeFactory.ReadFieldNodeGen;
import som.interpreter.objectstorage.FieldAccessorNodeFactory.WriteFieldNodeGen;
import som.vm.constants.Nil;


public abstract class FieldAccessorNode extends Node implements ReflectiveNode {
  protected static final int LIMIT = 4;
  protected final int fieldIndex;

  public static ReadFieldNode createRead(final int fieldIndex) {
    return ReadFieldNodeGen.create(fieldIndex);
  }

  public static WriteFieldNode createWrite(final int fieldIndex) {
    return WriteFieldNodeGen.create(fieldIndex);
  }

  protected FieldAccessorNode(final int fieldIndex) {
    this.fieldIndex = fieldIndex;
  }

  public final int getFieldIndex() {
    return fieldIndex;
  }

  protected Location getLocation(final DynamicObject obj) {
    Property property = obj.getShape().getProperty(fieldIndex);
    if (property != null) {
      return property.getLocation();
    } else {
      return null;
    }
  }

  protected Location getLocation(final DynamicObject obj, final Object value) {
    Location location = getLocation(obj);
    if (location != null && location.canSet(obj, value)) {
      return location;
    } else {
      return null;
    }
  }

  protected static final Assumption createAssumption() {
    return Truffle.getRuntime().createAssumption();
  }

  protected boolean updateShape(final DynamicObject obj) {
    return obj.updateShape();
  }

  @Introspectable
  public abstract static class ReadFieldNode extends FieldAccessorNode {
    public ReadFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public abstract Object executeRead(DynamicObject obj);

    @Specialization(guards = {"self.getShape() == cachedShape", "location != null"},
        assumptions = "cachedShape.getValidAssumption()",
        limit = "LIMIT")
    protected final Object readSetField(final DynamicObject self,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location) {
      return location.get(self, cachedShape);
    }

    @Specialization(guards = {"self.getShape() == cachedShape", "location == null"},
        assumptions = "cachedShape.getValidAssumption()",
        limit = "LIMIT")
    protected final Object readUnsetField(final DynamicObject self,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location) {
      return Nil.nilObject;
    }

    @Specialization(guards = "updateShape(object)")
    public Object updateShapeAndWrite(final DynamicObject object) {
        return executeRead(object);
    }

    @Specialization(replaces = {"readSetField", "readUnsetField"})
    public final Object readFieldUncached(final DynamicObject receiver) {
      return receiver.get(fieldIndex, Nil.nilObject);
    }
  }

  @Introspectable
  public abstract static class WriteFieldNode extends FieldAccessorNode {
    public WriteFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public Object write(final DynamicObject obj, final Object value) {
      return executeWithGeneralized(obj, value, false);
    }

    public abstract Object executeWithGeneralized(DynamicObject obj, Object value, boolean generalized);

    @Specialization(guards = {"self.getShape() == cachedShape", "location != null"},
        assumptions = {"cachedShape.getValidAssumption()"},
        limit = "LIMIT", rewriteOn = {IncompatibleLocationException.class, FinalLocationException.class})
    public final Object writeFieldCached(final DynamicObject self,
        final Object value, final boolean generalized,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self, value)") final Location location) throws IncompatibleLocationException, FinalLocationException {
      location.set(self, value);
      return value;
    }

    @Specialization(guards = {"self.getShape() == oldShape", "oldLocation == null"},
        assumptions = {"oldShape.getValidAssumption()", "newShape.getValidAssumption()"},
        limit = "LIMIT",
        rewriteOn = IncompatibleLocationException.class)
    public final Object writeUnwrittenField(final DynamicObject self,
        final Object value, final boolean generalized,
        @Cached("self.getShape()") final Shape oldShape,
        @Cached("getLocation(self, value)") final Location oldLocation,
        @Cached("defineProperty(oldShape, value, generalized)") final Shape newShape,
        @Cached("newShape.getProperty(fieldIndex).getLocation()") final Location newLocation) throws IncompatibleLocationException {
        newLocation.set(self, value, oldShape, newShape);
        return value;
    }

    @TruffleBoundary
    @Specialization(replaces = {"writeFieldCached", "writeUnwrittenField"},
      guards = {"self.getShape().isValid()"})
    public final Object writeUncached(final DynamicObject self, final Object value, final boolean generalize) {
      self.define(fieldIndex, value);
      return value;
    }

    @Specialization(guards = {"!self.getShape().isValid()"})
    public Object updateShapeAndWrite(final DynamicObject object, final Object value, final boolean generalize) {
      CompilerDirectives.transferToInterpreter();
      object.updateShape();
      return executeWithGeneralized(object, value, generalize);
    }

    private static final Object SOME_OBJECT = new Object();

    protected Shape defineProperty(final Shape oldShape, Object value, final boolean generalize) {
      if (generalize) {
          value = SOME_OBJECT;
      }
      Shape newShape = oldShape.defineProperty(fieldIndex, value, 0);
      oldShape.getValidAssumption().invalidate();
      return newShape;
    }
  }
}
