/**
 * Copyright (c) 2013 Stefan Marr, stefan.marr@vub.ac.be
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
package som.interpreter;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.object.DynamicObject;

import som.vm.constants.Classes;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.MateClasses;
import som.vm.constants.Nil;
import som.vmobjects.MockJavaObject;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SArray;
import som.vmobjects.SBlock;
import som.vmobjects.SClass;
import som.vmobjects.SFile;
import som.vmobjects.SObject;
import som.vmobjects.SShape;
import som.vmobjects.SSymbol;

@TypeSystem({   boolean.class,
                   long.class,
             BigInteger.class,
                 double.class,
                   char.class,
                 String.class,
                 SBlock.class,
                SSymbol.class,
                 SArray.class,
                 SShape.class,
         MockJavaObject.class,
        SAbstractObject.class,
          DynamicObject.class,
               Object[].class}) // Object[] is only for argument passing
public class Types {

  @TruffleBoundary
  public static DynamicObject getClassOf(final Object obj) {
    CompilerAsserts.neverPartOfCompilation("Types.getClassOf");
    assert obj != null;

    if (obj instanceof SAbstractObject) {
      return ((SAbstractObject) obj).getSOMClass();
    } else if (obj instanceof DynamicObject) {
      return SObject.getSOMClass((DynamicObject) obj);
    } else if (obj instanceof Boolean) {
      if ((boolean) obj) {
        return Classes.trueClass;
      } else {
        return Classes.falseClass;
      }
    } else if (obj instanceof Long || obj instanceof BigInteger) {
      return Classes.integerClass;
    } else if (obj instanceof String) {
      return Classes.stringClass;
    } else if (obj instanceof Character) {
      return Classes.characterClass;
    } else if (obj instanceof Double) {
      return Classes.doubleClass;
    } else if (obj instanceof FrameInstance) {
      return MateClasses.contextClass;
    } else if (obj instanceof SFile) {
      return Classes.objectClass;
    } else if (obj instanceof ExecutionLevel) {
      return Classes.objectClass;
    }

    TruffleCompiler.transferToInterpreter("Should not be reachable");
    throw new RuntimeException("We got an object that should be covered by the above check: " + obj.toString());
  }

  /** Return String representation of obj to be used in debugger. */
  public static String toDebuggerString(final Object obj) {
    if (obj instanceof Boolean) {
      if ((boolean) obj) {
        return "true";
      } else {
        return "false";
      }
    }
    if (obj == Nil.nilObject) {
      return "nil";
    }
    if (obj instanceof String) {
      return (String) obj;
    }
    if (obj instanceof SAbstractObject || obj instanceof Number || obj instanceof Thread) {
      return obj.toString();
    }
    return "a " + SClass.getName(SObject.getSOMClass((DynamicObject) obj)).getString();
  }
}
