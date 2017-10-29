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

import som.vm.NotYetImplementedException;
import som.vm.constants.Nil;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.dsl.Layout;


public class SObject {
  @Layout
  public interface SBasicObjectLayout {
    DynamicObject createSBasicObject();
  }

  @Layout(objectTypeSuperclass = MateObjectType.class)
  public interface SObjectLayout {
    // DynamicObject createSObject(DynamicObjectFactory factory);
    // DynamicObject getKlass(DynamicObjectFactory factory);
    DynamicObjectFactory createSObjectShape(DynamicObject klass);
    DynamicObject getKlass(ObjectType objectType);
    DynamicObject getKlass(DynamicObject object);
    void setKlass(DynamicObject object, DynamicObject value);
    boolean isSObject(DynamicObject object);
    boolean isSObject(ObjectType objectType);
    Object[] build();
  }

  // Only needed for system initialization
  public static final DynamicObjectFactory SOBJECT_FACTORY = SObjectLayoutImpl.INSTANCE.createSObjectShape(Nil.nilObject);

  public static DynamicObjectFactory createObjectShapeFactoryForClass(final DynamicObject clazz) {
    return SObjectLayoutImpl.INSTANCE.createSObjectShape(clazz);
  }

  public static boolean isSObject(final DynamicObject obj) {
    return SObjectLayoutImpl.INSTANCE.isSObject(obj);
  }

  public static void setClass(final DynamicObject obj, final DynamicObject value) {
    CompilerAsserts.neverPartOfCompilation("Should not compile changing the class of an object, it should only happen in the slow path?");
    SObjectLayoutImpl.INSTANCE.setKlass(obj, value);
  }

  @TruffleBoundary
  public static DynamicObject getSOMClass(final DynamicObject obj) {
    return SObjectLayoutImpl.INSTANCE.getKlass(obj);
  }

  @TruffleBoundary
  public static DynamicObject getSOMClass(final ObjectType obj) {
    return SObjectLayoutImpl.INSTANCE.getKlass(obj);
  }

  public static final int getNumberOfFields(final DynamicObject obj) {
    throw new NotYetImplementedException();
  }

  public Object[] buildArguments() {
    return SObjectLayoutImpl.INSTANCE.build();
  }
}
