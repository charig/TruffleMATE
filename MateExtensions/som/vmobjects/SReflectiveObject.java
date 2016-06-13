/**
 * Copyright (c) 2015 Guido Chari, gchari@dc.uba.ar
 * LaFHIS lab, Universidad de Buenos Aires, Buenos Aires, Argentina
 * http://www.lafhis.dc.uba.ar
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

import som.vm.Universe;
import som.vm.constants.Nil;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;

public class SReflectiveObject extends SObject {
  public static final SSymbol ENVIRONMENT = Universe.current().symbolFor("environment");
  public static final SReflectiveObjectObjectType SREFLECTIVE_OBJECT_TYPE = new SReflectiveObjectObjectType(Nil.nilObject);
  
  protected static HashMap<DynamicObject, SReflectiveObjectObjectType> SREFLECTIVE_OBJECT_TYPES = 
      new HashMap<DynamicObject, SReflectiveObjectObjectType>();
  
  protected static final Shape SREFLECTIVE_OBJECT_SHAPE = 
      INIT_NIL_SHAPE.createSeparateShape(INIT_NIL_SHAPE.getSharedData())
      .changeType(SREFLECTIVE_OBJECT_TYPE);
      
  public static final DynamicObjectFactory SREFLECTIVE_OBJECT_FACTORY = SREFLECTIVE_OBJECT_SHAPE.createFactory();
  
  public static final Shape createObjectShapeForClass(final DynamicObject clazz) {
    return LAYOUT.createShape(SREFLECTIVE_OBJECT_TYPE, clazz);
  }
  
  public static final DynamicObject getEnvironment(final DynamicObject obj) {
    return getEnvironment(obj.getShape());
  }
  
  public static final DynamicObject getEnvironment(final Shape shape) {
    //Todo: Remove the if when all objects use the new @layout annotation
    if (InvokableLayoutImpl.INSTANCE.isInvokable(shape.getObjectType())) return Nil.nilObject;
    return ((SReflectiveObjectObjectType)shape.getObjectType()).getEnvironment();
  }
  
  //@TruffleBoundary
  public static SReflectiveObjectObjectType objectTypeFor(DynamicObject environment, DynamicObject target){
      SReflectiveObjectObjectType type = SREFLECTIVE_OBJECT_TYPES.get(environment);
      if (type == null){
        CompilerDirectives.transferToInterpreter();
        /*
         * Hack so objects with metaobjects do not lose the property for being classes. 
         * Needs a fix so that the same metaobject can be assigned to instances and classes. 
         * It still was no needed.
         */
        if (SClass.isSClass(target)){
          type = new SClass.SClassObjectType(environment);
        } else {
          type = new SReflectiveObjectObjectType(environment);
        }
        SREFLECTIVE_OBJECT_TYPES.put(environment, type);
      }
      return type;
  }

  public static final void setEnvironment(final DynamicObject obj, final DynamicObject value) {
    SReflectiveObjectObjectType type = objectTypeFor(value, obj);
    obj.setShapeAndResize(obj.getShape(), obj.getShape().changeType(type));
  }
  
  public static class SReflectiveObjectObjectType extends ObjectType {
    public final DynamicObject environment;
    
    public SReflectiveObjectObjectType(DynamicObject metaobj){
      super();
      this.environment = metaobj;
    }
    
    @Override
    public String toString(DynamicObject object) {
      String environment;
      if (this.environment == Nil.nilObject){
        environment = "nil";
      } else {
        environment = this.environment.toString();
      }
      return "SReflectiveObject" + 
      "\nClass:" + SClass.getName((DynamicObject)object.getShape().getSharedData()) + 
      "\nEnvironment: " + environment;
    }
    
    public DynamicObject getEnvironment(){
      return this.environment;
    }
  }
  
  public static boolean isSReflectiveObject(final DynamicObject obj) {
    return obj.getShape().getObjectType() instanceof SReflectiveObjectObjectType;
  }
  
  public static boolean isSReflectiveObject(ObjectType type) {
    return type == SREFLECTIVE_OBJECT_TYPE;
  }
}
