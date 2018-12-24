/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
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
package som.compiler;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ValueProfile;

import som.vm.Symbols;
import som.vm.Universe;
import som.vm.constants.Classes;
import som.vmobjects.SArray;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

public final class ClassGenerationContext {
  private static final ValueProfile storageType = ValueProfile.createClassProfile();

  private SSymbol             name;
  private SSymbol             superName;
  private boolean             classSide;
  private final List<SSymbol> instanceFields  = new ArrayList<SSymbol>();
  private final List<DynamicObject> instanceMethods = new ArrayList<DynamicObject>();
  private final List<SSymbol> classFields     = new ArrayList<SSymbol>();
  private final List<DynamicObject> classMethods    = new ArrayList<DynamicObject>();

  public void setName(final SSymbol name) {
    this.name = name;
  }

  public SSymbol getName() {
    return name;
  }

  public void setSuperName(final SSymbol superName) {
    this.superName = superName;
  }

  public void setInstanceFieldsOfSuper(final SArray fieldNames) {
    for (int i = 0; i < fieldNames.getObjectStorage(storageType).length; i++) {
      instanceFields.add((SSymbol) fieldNames.getObjectStorage(storageType)[i]);
    }
  }

  public void setClassFieldsOfSuper(final SArray fieldNames) {
    for (int i = 0; i < fieldNames.getObjectStorage(storageType).length; i++) {
      classFields.add((SSymbol) fieldNames.getObjectStorage(storageType)[i]);
    }
  }

  public void addInstanceMethod(final DynamicObject meth) {
    instanceMethods.add(meth);
  }

  public void setClassSide(final boolean b) {
    classSide = b;
  }

  public void addClassMethod(final DynamicObject meth) {
    classMethods.add(meth);
  }

  public void addInstanceField(final SSymbol field) {
    instanceFields.add(field);
  }

  public void addClassField(final SSymbol field) {
    classFields.add(field);
  }

  public boolean hasField(final SSymbol field) {
    return (isClassSide() ? classFields : instanceFields).contains(field);
  }

  public byte getFieldIndex(final SSymbol field) {
    if (isClassSide()) {
      return (byte) classFields.indexOf(field);
    } else {
      return (byte) instanceFields.indexOf(field);
    }
  }

  public boolean isClassSide() {
    return classSide;
  }

  @TruffleBoundary
  public DynamicObject assemble() {
    // build class class name
    String ccname = name.getString() + " class";

    // Load the super class
    DynamicObject superClass = Universe.getCurrent().loadClass(superName);

    // Allocate the class of the resulting class
    DynamicObject resultClass = SClass.createSClass(Classes.metaclassClass, Symbols.symbolFor(ccname), SObject.getSOMClass(superClass),
        SArray.create(classFields.toArray(new Object[0])), SArray.create(classMethods.toArray(new Object[0])));

    // Allocate the resulting class
    DynamicObject result = SClass.createSClass(resultClass, name, superClass, SArray.create(instanceFields.toArray(new Object[0])),
        SArray.create(instanceMethods.toArray(new Object[0])));
    return result;
  }

  @TruffleBoundary
  public void assembleSystemClass(final DynamicObject systemClass) {
    for (Object invokable : instanceMethods) {
      SInvokable.setHolder((DynamicObject) invokable, systemClass);
    }
    SClass.setInstanceInvokables(systemClass,
        SArray.create(instanceMethods.toArray(new Object[0])));
    SClass.setInstanceFields(systemClass,
        SArray.create(instanceFields.toArray(new Object[0])));
    // class-bound == class-instance-bound
    DynamicObject superMClass = SObject.getSOMClass(systemClass);
    for (Object invokable : instanceMethods) {
      SInvokable.setHolder((DynamicObject) invokable, systemClass);
    }
    SClass.setInstanceInvokables(superMClass,
        SArray.create(classMethods.toArray(new Object[0])));
    SClass.setInstanceFields(superMClass,
        SArray.create(classFields.toArray(new Object[0])));
  }

  @Override
  public String toString() {
    return "ClassGenC(" + name.getString() + ")";
  }
}
