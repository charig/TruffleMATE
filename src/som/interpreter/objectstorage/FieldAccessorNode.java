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
import com.oracle.truffle.api.object.TypedLocation;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.object.basic.BasicLocations.DoubleLocationDecorator;
import com.oracle.truffle.object.basic.BasicLocations.IntLocationDecorator;
import com.oracle.truffle.object.basic.BasicLocations.LongLocationDecorator;

import som.interpreter.ReflectiveNode;
import som.interpreter.objectstorage.FieldAccessorNodeFactory.ReadFieldNodeGen;
import som.interpreter.objectstorage.FieldAccessorNodeFactory.WriteFieldNodeGen;
import som.vm.constants.Nil;
import som.vmobjects.SClass;
import som.vmobjects.SObject;

public abstract class FieldAccessorNode extends Node implements ReflectiveNode {
  protected static final int LIMIT = 5;
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

  protected static final Assumption createAssumption() {
    return Truffle.getRuntime().createAssumption();
  }

  protected static final Class<?> locationType(final Location location) {
    return ((TypedLocation) location).getClass();
  }

  protected static final boolean isIntLocation(final Class<?> locationClass) {
    return locationClass.equals(IntLocationDecorator.class);
  }

  protected static final boolean isLongLocation(final Class<?> locationClass) {
    return locationClass.equals(LongLocationDecorator.class);
  }

  protected static final boolean isDoubleLocation(final Class<?> locationClass) {
    return locationClass.equals(DoubleLocationDecorator.class);
  }

  protected static final BranchProfile createBranchProfile() {
    return BranchProfile.create();
  }

  @Introspectable
  public abstract static class ReadFieldNode extends FieldAccessorNode {
    public ReadFieldNode(final int fieldIndex) {
      super(fieldIndex);
    }

    public abstract Object executeRead(DynamicObject obj);

    @Specialization(guards = {"self.getShape() == cachedShape", "location != null", "isIntLocation(locationType)"},
        assumptions = "cachedShape.getValidAssumption()",
        limit = "LIMIT")
    protected final Object readSetFieldInt(final DynamicObject self,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location,
        @Cached("locationType(location)") final Class<?> locationType,
        @Cached("createBranchProfile()") final BranchProfile profile) {
      int value = (int) location.get(self, cachedShape);
      if (value == Integer.MIN_VALUE) {
        profile.enter();
        return Nil.nilObject;
      } else {
        return value;
      }
    }

    @Specialization(guards = {"self.getShape() == cachedShape", "location != null", "isLongLocation(locationType)"},
        assumptions = "cachedShape.getValidAssumption()",
        limit = "LIMIT")
    protected final Object readSetFieldLong(final DynamicObject self,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location,
        @Cached("locationType(location)") final Class<?> locationType,
        @Cached("createBranchProfile()") final BranchProfile profile) {
      long value = (long) location.get(self, cachedShape);
      if (value == Long.MIN_VALUE) {
        profile.enter();
        return Nil.nilObject;
      } else {
        return value;
      }
    }

    @Specialization(guards = {"self.getShape() == cachedShape", "location != null", "isDoubleLocation(locationType)"},
        assumptions = "cachedShape.getValidAssumption()",
        limit = "LIMIT")
    protected final Object readSetFieldDouble(final DynamicObject self,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location,
        @Cached("locationType(location)") final Class<?> locationType,
        @Cached("createBranchProfile()") final BranchProfile profile) {
      double value = (double) location.get(self, cachedShape);
      if (value == Double.NaN) {
        profile.enter();
        return Nil.nilObject;
      } else {
        return value;
      }
    }

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

    @TruffleBoundary
    @Specialization(replaces = {"readSetField", "readUnsetField"}, guards = "receiver.getShape().isValid()")
    public final Object readFieldUncached(final DynamicObject receiver) {
      return receiver.get(fieldIndex, Nil.nilObject);
    }

    @Specialization(guards = "!receiver.getShape().isValid()")
    public final Object updateShape(final DynamicObject receiver) {
        CompilerDirectives.transferToInterpreter();
        SObject.updateLayoutToMatchClass(receiver);
        return executeRead(receiver);
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
        assumptions = {"cachedShape.getValidAssumption()"},
        limit = "LIMIT", rewriteOn = {IncompatibleLocationException.class, FinalLocationException.class})
    public final Object writeFieldCached(final DynamicObject self,
        final Object value,
        @Cached("self.getShape()") final Shape cachedShape,
        @Cached("getLocation(self)") final Location location,
        @Cached("locationType(location)") final Class<?> locationType,
        @Cached("createBranchProfile()") final BranchProfile profile1,
        @Cached("createBranchProfile()") final BranchProfile profile2) throws IncompatibleLocationException, FinalLocationException {
        if (location.canSet(self, value)) {
          if (isIntLocation(locationType) && (int) value == Integer.MIN_VALUE) {
            profile2.enter();
            // TODO: Generalize to long
          }
          if (isLongLocation(locationType) && (long) value == Long.MIN_VALUE) {
            profile2.enter();
            // TODO: Generalize to Double
          }
          if (isDoubleLocation(locationType) && (double) value == Double.NaN) {
            profile2.enter();
            // TODO: Generalize to Object nan
          }
          location.set(self, value);
        } else {
          profile1.enter();
          Shape generalizedshape = defineProperty(self, cachedShape, value);
          Location newLocation = generalizedshape.getProperty(fieldIndex).getLocation();
          newLocation.set(self, value, cachedShape, generalizedshape);
        }
        return value;
    }


    // @TruffleBoundary
    @Specialization(guards = {"self.getShape() == oldShape", "oldLocation == null"},
        assumptions = {"newShape.getValidAssumption()"},
        limit = "LIMIT",
        rewriteOn = IncompatibleLocationException.class)
    public final Object writeUnwrittenField(final DynamicObject self,
        final Object value,
        @Cached("self.getShape()") final Shape oldShape,
        @Cached("getLocation(self)") final Location oldLocation,
        @Cached("defineProperty(self, self.getShape(), value)") final Shape newShape,
        @Cached("newShape.getProperty(fieldIndex).getLocation()") final Location newLocation) throws IncompatibleLocationException {
      newLocation.set(self, value, self.getShape(), newShape);
      return value;
    }

    @TruffleBoundary
    @Specialization(guards = {"self.getShape().isValid()"}, replaces = {"writeFieldCached", "writeUnwrittenField"})
    public final Object writeUncached(final DynamicObject self, final Object value) {
      Shape oldShape = self.getShape();
      self.define(fieldIndex, value);
      upgradeShapeIfNeccessary(oldShape, self.getShape(), SObject.getSOMClass(self));
      return value;
    }

    @TruffleBoundary
    @Specialization(guards = {"!self.getShape().isValid()"})
    public final Object updateShape(final DynamicObject self, final Object value) {
      CompilerDirectives.transferToInterpreter();
      SObject.updateLayoutToMatchClass(self);
      return write(self, value);
    }

    @TruffleBoundary
    protected Shape defineProperty(final DynamicObject self, final Shape oldShape, final Object value) {
      if (!self.getShape().isValid()) {
        SObject.updateLayoutToMatchClass(self);
      }
      Shape newShape = self.getShape().defineProperty(fieldIndex, value, 0);
      assert (newShape.isValid());
      upgradeShapeIfNeccessary(self.getShape(), newShape, SObject.getSOMClass(self));
      return newShape;
    }

    @TruffleBoundary
    protected void upgradeShapeIfNeccessary(final Shape oldShape, final Shape newShape, final DynamicObject klass) {
      if (newShape != oldShape) {
        assert (newShape.isRelated(oldShape));
        SClass.setInstancesFactory(klass, newShape.createFactory());
        oldShape.getValidAssumption().invalidate();
      }
    }
  }
}
