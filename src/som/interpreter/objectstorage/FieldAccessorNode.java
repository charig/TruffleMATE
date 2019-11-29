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
import som.vm.Universe;
import som.vm.constants.Nil;


public abstract class FieldAccessorNode extends Node implements ReflectiveNode {
  protected static final int LIMIT = 6;
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

  @Introspectable
  public abstract static class ReadFieldNode extends FieldAccessorNode {
    public ReadFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public abstract Object executeRead(DynamicObject obj);

    @Specialization(guards = {"self.getShape() == cachedShape", "location != null"},
        assumptions = "cachedShape.getValidAssumption()",
        limit = "1")
    protected final Object readSetFieldWarmup(final DynamicObject self,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location) {
      return location.get(self, cachedShape);
    }

    @Specialization(guards = {"self.getShape() == cachedShape", "location == null"},
        assumptions = "cachedShape.getValidAssumption()",
        limit = "1")
    protected final Object readUnsetFieldWarmup(final DynamicObject self,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location) {
      return Nil.nilObject;
    }

    @Specialization(guards = {"self.getShape() == cachedShape", "location != null"},
        assumptions = "cachedShape.getValidAssumption()", replaces = {"readSetFieldWarmup"},
        limit = "LIMIT")
    protected final Object readSetField(final DynamicObject self,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location) {
      return location.get(self, cachedShape);
    }

    @Specialization(guards = {"self.getShape() == cachedShape", "location == null"},
        assumptions = "cachedShape.getValidAssumption()", replaces = {"readUnsetFieldWarmup"},
        limit = "LIMIT")
    protected final Object readUnsetField(final DynamicObject self,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location) {
      return Nil.nilObject;
    }

    @TruffleBoundary
    @Specialization(replaces = {"readSetField", "readUnsetField"}, guards = "receiver.getShape().isValid()")
    public final Object readFieldUncached(final DynamicObject receiver) {
      return receiver.get(fieldIndex, Nil.nilObject);
    }

    @Specialization(guards = "!receiver.getShape().isValid()")
    public final Object updateShape(final DynamicObject receiver) {
        CompilerDirectives.transferToInterpreter();
        receiver.updateShape();
        return readFieldUncached(receiver);
    }

  }

  @Introspectable
  public abstract static class WriteFieldNode extends FieldAccessorNode {
    public WriteFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public Object write(final DynamicObject obj, final Object value) {
      return execute(obj, value);
    }

    public abstract Object execute(DynamicObject obj, Object value);

    @Specialization(guards = {"self.getShape() == cachedShape", "location != null"},
        assumptions = {"locationAssignable", "cachedShape.getValidAssumption()"},
        limit = "1", rewriteOn = {IncompatibleLocationException.class, FinalLocationException.class})
    public final Object writeFieldCachedWarmup(final DynamicObject self,
        final Object value,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self, value)") final Location location,
        @Cached("createAssumption()") final Assumption locationAssignable) throws IncompatibleLocationException, FinalLocationException {
      location.set(self, value);
      return value;
    }

    @TruffleBoundary
    @Specialization(guards = {"self.getShape() == oldShape", "oldLocation == null"},
        assumptions = {"oldShape.getValidAssumption()", "newShape.getValidAssumption()"},
        limit = "1",
        rewriteOn = IncompatibleLocationException.class)
    public final Object writeUnwrittenFieldWarmup(final DynamicObject self,
        final Object value,
        @Cached("self.getShape()") final Shape oldShape,
        @Cached("getLocation(self, value)") final Location oldLocation,
        @Cached("defineProperty(oldShape, value)") final Shape newShape,
        @Cached("newShape.getProperty(fieldIndex).getLocation()") final Location newLocation) throws IncompatibleLocationException {
        newLocation.set(self, value, oldShape, newShape);
        //SClass.setInstancesFactory(SObject.getSOMClass(self), newShape.createFactory());
        return value;
    }


    @Specialization(guards = {"self.getShape() == cachedShape", "location != null"},
        assumptions = {"locationAssignable", "cachedShape.getValidAssumption()"},
        limit = "LIMIT", replaces = "writeFieldCachedWarmup",
        rewriteOn = {IncompatibleLocationException.class, FinalLocationException.class})
    public final Object writeFieldCached(final DynamicObject self,
        final Object value,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self, value)") final Location location,
        @Cached("createAssumption()") final Assumption locationAssignable) throws IncompatibleLocationException, FinalLocationException {
      location.set(self, value);
      return value;
    }

    @TruffleBoundary
    @Specialization(guards = {"self.getShape() == oldShape", "oldLocation == null"},
        assumptions = {"oldShape.getValidAssumption()", "newShape.getValidAssumption()"},
        replaces = "writeUnwrittenFieldWarmup", limit = "LIMIT",
        rewriteOn = IncompatibleLocationException.class)
    public final Object writeUnwrittenField(final DynamicObject self,
        final Object value,
        @Cached("self.getShape()") final Shape oldShape,
        @Cached("getLocation(self, value)") final Location oldLocation,
        @Cached("defineProperty(oldShape, value)") final Shape newShape,
        @Cached("newShape.getProperty(fieldIndex).getLocation()") final Location newLocation) throws IncompatibleLocationException {
        newLocation.set(self, value, oldShape, newShape);
        //SClass.setInstancesFactory(SObject.getSOMClass(self), newShape.createFactory());
        return value;
    }

    @TruffleBoundary
    @Specialization(guards = {"self.getShape().isValid()"}, replaces = {"writeFieldCached", "writeUnwrittenField"})
    public final Object writeUncached(final DynamicObject self, final Object value) {
      self.define(fieldIndex, value);
      return value;
    }

    @TruffleBoundary
    @Specialization(guards = {"!self.getShape().isValid()"})
    public final void updateShape(final DynamicObject self, final Object value) {
        CompilerDirectives.transferToInterpreter();
        self.updateShape();
        this.writeUncached(self, value);
    }

    protected Shape defineProperty(final Shape oldShape, final Object value) {
      Shape newShape = oldShape.defineProperty(fieldIndex, value, 0);
      //oldShape.getValidAssumption().invalidate();
      return newShape;
    }

    protected boolean updateShape(final DynamicObject obj) {
      return obj.updateShape();
    }
  }
}
