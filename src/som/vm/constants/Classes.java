package som.vm.constants;

import som.vm.ObjectMemory;
import com.oracle.truffle.api.object.DynamicObject;

public class Classes {
  public static final DynamicObject  objectClass;
  public static final DynamicObject  classClass;
  public static final DynamicObject  metaclassClass;
  public static final DynamicObject  blockClass;
  public static final DynamicObject  nilClass;
  public static final DynamicObject  integerClass;
  public static final DynamicObject  arrayClass;
  public static final DynamicObject  methodClass;
  public static final DynamicObject  symbolClass;
  public static final DynamicObject  primitiveClass;
  public static final DynamicObject  stringClass;
  public static final DynamicObject  characterClass;
  public static final DynamicObject  doubleClass;
  public static final DynamicObject  booleanClass;
  public static final DynamicObject  contextClass;
  public static final DynamicObject  trueClass;
  public static final DynamicObject  falseClass;
  public static final DynamicObject  systemClass;
  // These classes can be statically preinitialized.

  static {
    // Allocate the Metaclass classes
    metaclassClass = ObjectMemory.newMetaclassClass("Metaclass");
    // Allocate the rest of the system classes
    objectClass     = ObjectMemory.newSystemClass(Nil.nilObject);
    nilClass        = ObjectMemory.newSystemClass(objectClass);
    classClass      = ObjectMemory.newSystemClass(objectClass);
    blockClass      = ObjectMemory.newSystemClass(objectClass);
    arrayClass      = ObjectMemory.newSystemClass(objectClass);
    stringClass     = ObjectMemory.newSystemClass(objectClass);
    characterClass  = ObjectMemory.newSystemClass(objectClass);
    symbolClass     = ObjectMemory.newSystemClass(stringClass);
    methodClass     = ObjectMemory.newSystemClass(objectClass);
    integerClass    = ObjectMemory.newSystemClass(objectClass);
    primitiveClass  = ObjectMemory.newSystemClass(objectClass);
    doubleClass     = ObjectMemory.newSystemClass(objectClass);
    booleanClass    = ObjectMemory.newSystemClass(objectClass);
    trueClass       = ObjectMemory.newSystemClass(booleanClass);
    falseClass      = ObjectMemory.newSystemClass(booleanClass);
    systemClass     = ObjectMemory.newSystemClass(objectClass);
    contextClass    = ObjectMemory.newSystemClass(objectClass);
  }
}
