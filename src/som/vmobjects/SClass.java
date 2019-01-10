/**
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package som.vmobjects;

import java.util.HashMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.dsl.Layout;
import com.oracle.truffle.api.profiles.ValueProfile;

import som.vm.Universe;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.Nil;
import som.vmobjects.SInvokable.SPrimitive;
import som.vmobjects.SReflectiveObject.SReflectiveObjectLayout;
import som.vmobjects.SReflectiveObjectEnvInObj.SReflectiveObjectEnvInObjLayout;

public final class SClass {
  @Layout
  // public interface SClassLayout extends SReflectiveObjectEnvInObjLayout {
  public interface SClassLayout extends SReflectiveObjectLayout {
    DynamicObject createSClass(DynamicObjectFactory factory, SSymbol name, DynamicObject superclass, SArray instanceFields, SArray instanceInvokables, @SuppressWarnings("rawtypes") HashMap invokablesTable, DynamicObjectFactory instancesFactory);
    DynamicObjectFactory createSClassShape(DynamicObject klass, DynamicObject environment);
    DynamicObject getSuperclass(DynamicObject object);
    SSymbol getName(DynamicObject object);
    SArray getInstanceFields(DynamicObject object);
    SArray getInstanceInvokables(DynamicObject object);
    @SuppressWarnings("rawtypes")
    HashMap getInvokablesTable(DynamicObject object);
    DynamicObjectFactory getInstancesFactory(DynamicObject object);
    void setInstancesFactoryUnsafe(DynamicObject object, DynamicObjectFactory value);
    void setInstanceFieldsUnsafe(DynamicObject object, SArray value);
    void setInstanceInvokablesUnsafe(DynamicObject object, SArray value);
    void setNameUnsafe(DynamicObject object, SSymbol value); // Required for initialization of system classes
    void setSuperclassUnsafe(DynamicObject object, DynamicObject value);
    boolean isSClass(DynamicObject object);
    boolean isSClass(ObjectType objectType);
  }

  @Layout
  public interface SClassEnvInObjectLayout extends SReflectiveObjectEnvInObjLayout {
    DynamicObject createSClassEnvInObject(DynamicObjectFactory factory, DynamicObject environment, SSymbol name, DynamicObject superclass, SArray instanceFields, SArray instanceInvokables, @SuppressWarnings("rawtypes") HashMap invokablesTable, DynamicObjectFactory instancesFactory);
    DynamicObjectFactory createSClassEnvInObjectShape(DynamicObject klass);
    DynamicObject getSuperclass(DynamicObject object);
    SSymbol getName(DynamicObject object);
    SArray getInstanceFields(DynamicObject object);
    SArray getInstanceInvokables(DynamicObject object);
    @SuppressWarnings("rawtypes")
    HashMap getInvokablesTable(DynamicObject object);
    DynamicObjectFactory getInstancesFactory(DynamicObject object);
    void setInstancesFactoryUnsafe(DynamicObject object, DynamicObjectFactory value);
    void setInstanceFieldsUnsafe(DynamicObject object, SArray value);
    void setInstanceInvokablesUnsafe(DynamicObject object, SArray value);
    void setNameUnsafe(DynamicObject object, SSymbol value); // Required for initialization of system classes
    void setSuperclassUnsafe(DynamicObject object, DynamicObject value);
    boolean isSClassEnvInObject(DynamicObject object);
    boolean isSClassEnvInObject(ObjectType objectType);
  }

  // class which get's its own class set only later (to break up cyclic dependencies)
  private static final DynamicObjectFactory INIT_CLASS_FACTORY =
      Universe.getCurrent().environmentInObect() ?
          SClassEnvInObjectLayoutImpl.INSTANCE.createSClassEnvInObjectShape(Nil.nilObject) :
          SClassLayoutImpl.INSTANCE.createSClassShape(Nil.nilObject, Nil.nilObject);


  public static DynamicObject createSClass(final DynamicObject klass, final SSymbol name, final DynamicObject superclass, final SArray fields, final SArray methods) {
    return createSClass(klass, name, superclass, fields, methods,
        new HashMap<SSymbol, DynamicObject>(), Universe.getCurrent().getInstancesFactory());

  }

  @TruffleBoundary
  public static DynamicObject createSClass(final DynamicObject klass, final SSymbol name, final DynamicObject superclass, final SArray instanceFields, final SArray instanceInvokables, final HashMap<SSymbol, DynamicObject> invokablesTable, final DynamicObjectFactory instancesFactory) {
    DynamicObject resultClass =
        Universe.getCurrent().environmentInObect() ?
            SClassEnvInObjectLayoutImpl.INSTANCE.createSClassEnvInObject(SClassEnvInObjectLayoutImpl.INSTANCE.createSClassEnvInObjectShape(klass), Nil.nilObject, name, superclass, instanceFields, instanceInvokables, invokablesTable, instancesFactory) :
            SClassLayoutImpl.INSTANCE.createSClass(SClassLayoutImpl.INSTANCE.createSClassShape(klass, Nil.nilObject), name, superclass, instanceFields, instanceInvokables, invokablesTable, instancesFactory);
    setInstancesFactory(resultClass, Universe.getCurrent().createObjectShapeFactoryForClass(resultClass));
    for (Object invokable : (Object[]) instanceInvokables.getStoragePlain()) {
      SInvokable.setHolder((DynamicObject) invokable, resultClass);
    }
    return resultClass;
  }

  public static DynamicObject createEmptyClass(final DynamicObject klass, final SSymbol name) {
    return createSClass(klass, name, Nil.nilObject, SArray.create(new Object[0]), SArray.create(new Object[0]));
  }

  public static DynamicObject createWithoutClass(final SSymbol name) {
    CompilerAsserts.neverPartOfCompilation("Class creation");
    Object[] args = Universe.getCurrent().environmentInObect() ?
        new Object[] {
            Nil.nilObject,                             // ENVIRONMENT
            name,                                      // NAME
            Nil.nilObject,                             // SUPERCLASS
            SArray.create(new Object[0]),              // INSTANCE_FIELDS
            SArray.create(new Object[0]),              // INSTANCE_INVOKABLES
            new HashMap<SSymbol, SInvokable>(),        // INVOKABLES_TABLE
            Universe.getCurrent().getInstancesFactory() // OBJECT_FACTORY, temporary value
         } :
        new Object[] {
               name,                                      // NAME
               Nil.nilObject,                             // SUPERCLASS
               SArray.create(new Object[0]),              // INSTANCE_FIELDS
               SArray.create(new Object[0]),              // INSTANCE_INVOKABLES
               new HashMap<SSymbol, SInvokable>(),        // INVOKABLES_TABLE
               Universe.getCurrent().getInstancesFactory() // OBJECT_FACTORY, temporary value
            };
    DynamicObject clazz =  INIT_CLASS_FACTORY.newInstance(args); // OBJECT_FACTORY, temporary value
    setInstancesFactory(clazz, Universe.getCurrent().createObjectShapeFactoryForClass(clazz));
    return clazz;
  }

  public static boolean isSClass(final DynamicObject clazz) {
    return Universe.getCurrent().environmentInObect() ?
        SClassEnvInObjectLayoutImpl.INSTANCE.isSClassEnvInObject(clazz) :
        SClassLayoutImpl.INSTANCE.isSClass(clazz);
  }

  public static DynamicObjectFactory getFactory(final DynamicObject clazz) {
    assert isSClass(clazz);
    return Universe.getCurrent().environmentInObect() ?
        SClassEnvInObjectLayoutImpl.INSTANCE.getInstancesFactory(clazz) :
        SClassLayoutImpl.INSTANCE.getInstancesFactory(clazz);
  }

  public static DynamicObject getSuperClass(final DynamicObject classObj) {
    // CompilerAsserts.neverPartOfCompilation("optimize caller");
    return Universe.getCurrent().environmentInObect() ?
        SClassEnvInObjectLayoutImpl.INSTANCE.getSuperclass(classObj) :
        SClassLayoutImpl.INSTANCE.getSuperclass(classObj);
  }

  public static boolean hasSuperClass(final DynamicObject classObj) {
    return getSuperClass(classObj) != Nil.nilObject;
  }

  public static SSymbol getName(final DynamicObject classObj) {
    // CompilerAsserts.neverPartOfCompilation("optimize caller");
    /* We are using it inside the fast path for checking if the class is a byteArray.
     * We should optimize NewPrim>>doByteSClass to enable again the neverPartOfCompilation
     */
    return Universe.getCurrent().environmentInObect() ?
        SClassEnvInObjectLayoutImpl.INSTANCE.getName(classObj) :
        SClassLayoutImpl.INSTANCE.getName(classObj);
  }

  public static void setName(final DynamicObject classObj, final SSymbol value) {
    if (Universe.getCurrent().environmentInObect()) {
      SClassEnvInObjectLayoutImpl.INSTANCE.setNameUnsafe(classObj, value);
    } else {
      SClassLayoutImpl.INSTANCE.setNameUnsafe(classObj, value);
    }
  }

  public static SArray getInstanceFields(final DynamicObject classObj) {
    return Universe.getCurrent().environmentInObect() ?
        SClassEnvInObjectLayoutImpl.INSTANCE.getInstanceFields(classObj) :
        SClassLayoutImpl.INSTANCE.getInstanceFields(classObj);
  }

  public static SArray getInstanceInvokables(final DynamicObject classObj) {
    return Universe.getCurrent().environmentInObect() ?
      SClassEnvInObjectLayoutImpl.INSTANCE.getInstanceInvokables(classObj) :
      SClassLayoutImpl.INSTANCE.getInstanceInvokables(classObj);
  }

  private static final ValueProfile storageType = ValueProfile.createClassProfile();

  public static int getNumberOfInstanceInvokables(final DynamicObject classObj) {
    // Return the number of instance invokables in this class
    return getInstanceInvokables(classObj).getObjectStorage(storageType).length;
  }

  public static DynamicObject getInstanceInvokable(final DynamicObject classObj, final int index) {
    return (DynamicObject) getInstanceInvokables(classObj).getObjectStorage(storageType)[index];
  }

  public static void setInstanceInvokable(final DynamicObject classObj, final int index, final DynamicObject value) {
    CompilerAsserts.neverPartOfCompilation("setInstanceInvokable");
    getInstanceInvokables(classObj).getObjectStorage(storageType)[index] = value;

    HashMap<SSymbol, DynamicObject> invokablesTable = getInvokablesTable(classObj);

    if (invokablesTable.containsKey(SInvokable.getSignature(value))) {
      invokablesTable.put(SInvokable.getSignature(value), value);
    }
  }

  @SuppressWarnings("unchecked")
  public static HashMap<SSymbol, DynamicObject> getInvokablesTable(
      final DynamicObject classObj) {
    return Universe.getCurrent().environmentInObect() ?
        SClassEnvInObjectLayoutImpl.INSTANCE.getInvokablesTable(classObj) :
        SClassLayoutImpl.INSTANCE.getInvokablesTable(classObj);
  }

  @TruffleBoundary
  public static DynamicObject lookupInvokable(final DynamicObject classObj, final SSymbol selector) {
    DynamicObject invokable;
    HashMap<SSymbol, DynamicObject> invokablesTable = getInvokablesTable(classObj);

    // Lookup invokable and return if found
    invokable = invokablesTable.get(selector);
    if (invokable != null) { return invokable; }

    // Lookup invokable with given signature in array of instance invokables
    for (int i = 0; i < getNumberOfInstanceInvokables(classObj); i++) {
      // Get the next invokable in the instance invokable array
      invokable = getInstanceInvokable(classObj, i);

      // Return the invokable if the signature matches
      if (SInvokable.getSignature(invokable) == selector) {
        invokablesTable.put(selector, invokable);
        return invokable;
      }
    }

    // Traverse the super class chain by calling lookup on the super class
    if (hasSuperClass(classObj)) {
      invokable = lookupInvokable(getSuperClass(classObj), selector);
      if (invokable != null) {
        invokablesTable.put(selector, invokable);
        return invokable;
      }
    }

    // Invokable not found
    return null;
  }

  public static int lookupFieldIndex(final DynamicObject classObj, final SSymbol fieldName) {
    // Lookup field with given name in array of instance fields
    for (int i = getNumberOfInstanceFields(classObj) - 1; i >= 0; i--) {
      // Return the current index if the name matches
      if (fieldName == getInstanceFieldName(classObj, i)) { return i; }
    }

    // Field not found
    return -1;
  }

  private static boolean addInstanceInvokable(final DynamicObject classObj,
      final DynamicObject invokable) {
    CompilerAsserts.neverPartOfCompilation("SClass.addInstanceInvokable(.)");
    SInvokable.setHolder(invokable, classObj);
    // Add the given invokable to the array of instance invokables
    for (int i = 0; i < getNumberOfInstanceInvokables(classObj); i++) {
      // Get the next invokable in the instance invokable array
      DynamicObject lastInvokable = getInstanceInvokable(classObj, i);

      // Replace the invokable with the given one if the signature matches
      if (SInvokable.getSignature(lastInvokable) == SInvokable.getSignature(invokable)) {
        setInstanceInvokable(classObj, i, invokable);
        return false;
      }
    }

    // Append the given method to the array of instance methods
    setInstanceInvokables(
        classObj, getInstanceInvokables(classObj).copyAndExtendWith(invokable));
    return true;
  }

  public static void setSuperclass(final DynamicObject classObj, final DynamicObject klass) {
    if (Universe.getCurrent().environmentInObect()) {
      SClassEnvInObjectLayoutImpl.INSTANCE.setSuperclassUnsafe(classObj, klass);
    } else {
      SClassLayoutImpl.INSTANCE.setSuperclassUnsafe(classObj, klass);
    }
  }

  public static void setInstanceFields(final DynamicObject clazz, final SArray value) {
    if (Universe.getCurrent().environmentInObect()) {
      SClassEnvInObjectLayoutImpl.INSTANCE.setInstanceFieldsUnsafe(clazz, value);
    } else {
      SClassLayoutImpl.INSTANCE.setInstanceFieldsUnsafe(clazz, value);
    }
  }

  public static void setInstanceInvokables(final DynamicObject classObj, final SArray value) {
    if (Universe.getCurrent().environmentInObect()) {
      SClassEnvInObjectLayoutImpl.INSTANCE.setInstanceInvokablesUnsafe(classObj, value);
    } else {
      SClassLayoutImpl.INSTANCE.setInstanceInvokablesUnsafe(classObj, value);
    }
  }

  public static void setInstancesFactory(final DynamicObject clazz, final DynamicObjectFactory factory) {
    if (Universe.getCurrent().environmentInObect()) {
      SClassEnvInObjectLayoutImpl.INSTANCE.setInstancesFactoryUnsafe(clazz, factory);
    } else {
      SClassLayoutImpl.INSTANCE.setInstancesFactoryUnsafe(clazz, factory);
    }
  }

  public static void addInstancePrimitive(final DynamicObject classObj,
      final DynamicObject value, final boolean displayWarning) {
    if (addInstanceInvokable(classObj, value) && displayWarning) {
      Universe.print("Warning: Primitive " + SInvokable.getSignature(value).getString());
      Universe.println(" is not in class definition for class "
          + getName(classObj).getString());
    }
  }

  public static SSymbol getInstanceFieldName(final DynamicObject classObj, final int index) {
    return (SSymbol) getInstanceFields(classObj).getObjectStorage(storageType)[index];
  }

  public static int getNumberOfInstanceFields(final DynamicObject classObj) {
    return getInstanceFields(classObj).getObjectStorage(storageType).length;
  }

  private static boolean includesPrimitives(final DynamicObject clazz) {
    CompilerAsserts.neverPartOfCompilation("SClass.includesPrimitives(.)");
    // Lookup invokable with given signature in array of instance invokables
    for (int i = 0; i < getNumberOfInstanceInvokables(clazz); i++) {
      // Get the next invokable in the instance invokable array
      if (SPrimitive.isSPrimitive(getInstanceInvokable(clazz, i))) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasPrimitives(final DynamicObject classObj) {
    return includesPrimitives(classObj) || includesPrimitives(SObject.getSOMClass(classObj));
  }

  public static void initialize(final DynamicObject clazz) {
    CompilerAsserts.neverPartOfCompilation("Only supposed to be used from TCKTests");
    assert isSClass(clazz);
    DynamicObject init = lookupInvokable(clazz, Universe.getCurrent().symbolFor("initialize"));
    SInvokable.invoke(init, Nil.nilObject, ExecutionLevel.Base, new Object[]{clazz});
  }

  /**
   * Checks whether the classes are the same, including superclass hierarchy
   * and ignoring class identity, i.e., relying on class groups/factories, too.
   */
  public static boolean isKindOf(final DynamicObject clazz, final DynamicObject otherClass) {
    if (otherClass == clazz) {
      return true;
    }
    if (clazz == Nil.nilObject) {
      return false;
    }
    return SClass.isKindOf(SClass.getSuperClass(clazz), otherClass);
  }
}
