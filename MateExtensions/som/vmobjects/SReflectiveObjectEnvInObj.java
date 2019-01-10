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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.dsl.Layout;

import som.vm.constants.Nil;

public class SReflectiveObjectEnvInObj extends SObject {
  @Layout
  public interface SReflectiveObjectEnvInObjLayout extends SObjectLayout {
    // DynamicObject createSReflectiveObjectEnvInObj(DynamicObjectFactory factory, DynamicObject environment);
    DynamicObjectFactory createSReflectiveObjectEnvInObjShape(DynamicObject klass);
    DynamicObject getEnvironment(DynamicObject object);
    void setEnvironmentUnsafe(DynamicObject object, DynamicObject value);
    boolean isSReflectiveObjectEnvInObj(DynamicObject object);
    boolean isSReflectiveObjectEnvInObj(ObjectType objectType);
    Object[] build(DynamicObject environment);
  }

  // Only needed for system initialization
  public static final DynamicObjectFactory SREFLECTIVE_OBJECT_ENVINOBJ_FACTORY =
     SReflectiveObjectEnvInObjLayoutImpl.INSTANCE.createSReflectiveObjectEnvInObjShape(Nil.nilObject);

  public static final DynamicObject getEnvironment(final DynamicObject obj) {
    return SReflectiveObjectEnvInObjLayoutImpl.INSTANCE.getEnvironment(obj);
  }

  @Override
  public final Object[] buildArguments() {
    return SReflectiveObjectEnvInObjLayoutImpl.INSTANCE.build(Nil.nilObject);
  }

  public final Object[] buildArguments(final DynamicObject environment) {
    return SReflectiveObjectEnvInObjLayoutImpl.INSTANCE.build(environment);
  }

  public static final void setEnvironment(final DynamicObject obj, final DynamicObject environmnet) {
    SReflectiveObjectEnvInObjLayoutImpl.INSTANCE.setEnvironmentUnsafe(obj, environmnet);
  }

  public static boolean isSReflectiveObject(final DynamicObject obj) {
    return SReflectiveObjectEnvInObjLayoutImpl.INSTANCE.isSReflectiveObjectEnvInObj(obj);
  }

  public static boolean isSReflectiveObject(final ObjectType type) {
    return SReflectiveObjectEnvInObjLayoutImpl.INSTANCE.isSReflectiveObjectEnvInObj(type);
  }

  public static DynamicObjectFactory createObjectShapeFactoryForClass(final DynamicObject clazz) {
    return SReflectiveObjectEnvInObjLayoutImpl.INSTANCE.createSReflectiveObjectEnvInObjShape(clazz);
  }
}
