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

package som.vmobjects;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.dsl.Layout;

import som.interpreter.Invokable;
import som.interpreter.SArguments;
import som.interpreter.nodes.ExpressionNode;
import som.vm.Universe;
import som.vm.constants.Classes;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.Nil;
import som.vmobjects.SReflectiveObject.SReflectiveObjectLayout;
import som.vmobjects.SReflectiveObjectEnvInObj.SReflectiveObjectEnvInObjLayout;

public class SInvokable {

  @Layout
  public interface InvokableLayout extends SReflectiveObjectLayout {
    SSymbol getSignature(DynamicObject object);
    Invokable getInvokable(DynamicObject object);
    RootCallTarget getCallTarget(DynamicObject object);
    DynamicObject getHolder(DynamicObject object);
    Invokable getInvokableMeta(DynamicObject object);
    RootCallTarget getCallTargetMeta(DynamicObject object);
    void setHolderUnsafe(DynamicObject object, DynamicObject value);
    DynamicObject createInvokable(DynamicObjectFactory factory, SSymbol signature, Invokable invokable, RootCallTarget callTarget, Invokable invokableMeta, RootCallTarget callTargetMeta, DynamicObject holder);
    DynamicObjectFactory createInvokableShape(DynamicObject klass, DynamicObject environment);
    boolean isInvokable(DynamicObject object);
    boolean isInvokable(ObjectType objectType);
  }

  @Layout
  public interface InvokableEnvInObjectLayout extends SReflectiveObjectEnvInObjLayout {
    SSymbol getSignature(DynamicObject object);
    Invokable getInvokable(DynamicObject object);
    RootCallTarget getCallTarget(DynamicObject object);
    DynamicObject getHolder(DynamicObject object);
    Invokable getInvokableMeta(DynamicObject object);
    RootCallTarget getCallTargetMeta(DynamicObject object);
    void setHolderUnsafe(DynamicObject object, DynamicObject value);
    DynamicObject createInvokableEnvInObject(DynamicObjectFactory factory, DynamicObject environment, SSymbol signature, Invokable invokable, RootCallTarget callTarget, Invokable invokableMeta, RootCallTarget callTargetMeta, DynamicObject holder);
    DynamicObjectFactory createInvokableEnvInObjectShape(DynamicObject klass);
    boolean isInvokableEnvInObject(DynamicObject object);
    boolean isInvokableEnvInObject(ObjectType objectType);
  }

  private static final DynamicObjectFactory INVOKABLES_FACTORY = Universe.getCurrent().environmentInObect() ?
      InvokableEnvInObjectLayoutImpl.INSTANCE.createInvokableEnvInObjectShape(Classes.primitiveClass) :
      InvokableLayoutImpl.INSTANCE.createInvokableShape(Classes.primitiveClass, Nil.nilObject);

  public static DynamicObject create(final SSymbol signature, final Invokable invokable) {
    Invokable invokableMeta = (Invokable) invokable.deepCopy();
    // TODO: Fix the issue that deepCopy on invokable seems not to be called and then
    // remove the following line and rollback uninitializedBody to a protected field.
    invokableMeta.uninitializedBody = (ExpressionNode) invokableMeta.uninitializedBody.deepCopy();
    return Universe.getCurrent().environmentInObect() ?
        InvokableEnvInObjectLayoutImpl.INSTANCE.createInvokableEnvInObject(INVOKABLES_FACTORY, Nil.nilObject, signature, invokable, invokable.createCallTarget(),
            invokableMeta, invokableMeta.createCallTarget(), Nil.nilObject) :
        InvokableLayoutImpl.INSTANCE.createInvokable(INVOKABLES_FACTORY, signature, invokable, invokable.createCallTarget(),
            invokableMeta, invokableMeta.createCallTarget(), Nil.nilObject);
  }

  public static final RootCallTarget getCallTarget(final DynamicObject invokable, final ExecutionLevel level) {
    if (level == ExecutionLevel.Meta) {
      return Universe.getCurrent().environmentInObect() ?
          InvokableEnvInObjectLayoutImpl.INSTANCE.getCallTargetMeta(invokable) :
          InvokableLayoutImpl.INSTANCE.getCallTargetMeta(invokable);
    }
    return Universe.getCurrent().environmentInObect() ?
        InvokableEnvInObjectLayoutImpl.INSTANCE.getCallTarget(invokable) :
        InvokableLayoutImpl.INSTANCE.getCallTarget(invokable);
  }

  public static final int getNumberOfArguments(final DynamicObject invokable) {
    return getSignature(invokable).getNumberOfSignatureArguments();
  }

  public static final Invokable getInvokable(final DynamicObject invokable) {
    return Universe.getCurrent().environmentInObect() ?
        InvokableEnvInObjectLayoutImpl.INSTANCE.getInvokable(invokable) :
        InvokableLayoutImpl.INSTANCE.getInvokable(invokable);
  }

  public static final SSymbol getSignature(final DynamicObject invokable) {
    return Universe.getCurrent().environmentInObect() ?
        InvokableEnvInObjectLayoutImpl.INSTANCE.getSignature(invokable) :
        InvokableLayoutImpl.INSTANCE.getSignature(invokable);
  }

  public static final DynamicObject getHolder(final DynamicObject invokable) {
    return Universe.getCurrent().environmentInObect() ?
        InvokableEnvInObjectLayoutImpl.INSTANCE.getHolder(invokable) :
        InvokableLayoutImpl.INSTANCE.getHolder(invokable);
  }

  public static void setHolder(final DynamicObject invokable, final DynamicObject value) {
    if (SMethod.isSMethod(invokable)) {
      SMethod.setHolder(invokable, value);
    } else {
      if (Universe.getCurrent().environmentInObect()) {
        InvokableEnvInObjectLayoutImpl.INSTANCE.setHolderUnsafe(invokable, value);
      } else {
        InvokableLayoutImpl.INSTANCE.setHolderUnsafe(invokable, value);
      }
    }
  }

  public static final Object invoke(final DynamicObject invokable, final VirtualFrame frame, final Object... arguments) {
    return getCallTarget(invokable, SArguments.getExecutionLevelFromArrayOfArgs(arguments)).call(arguments);
  }

  public static final Object invoke(final DynamicObject invokable, final VirtualFrame frame, final IndirectCallNode node, final Object... arguments) {
    return node.call(getCallTarget(invokable, SArguments.getExecutionLevelFromArrayOfArgs(arguments)), arguments);
  }

  public static final Object invoke(final DynamicObject invokable, final DynamicObject environment, final ExecutionLevel exLevel, final Object... arguments) {
    return getCallTarget(invokable, exLevel).call(SArguments.createSArguments(environment, exLevel, arguments));
  }

  public static final String toString(final DynamicObject invokable) {
    // TODO: fixme: remove special case if possible, I think it indicates a bug
    if (getHolder(invokable) == Nil.nilObject) {
      return "Method(nil>>" + InvokableLayoutImpl.INSTANCE.getSignature(invokable).toString() + ")";
    }

    return "Method(" + SClass.getName(getHolder(invokable)).getString() + ">>" +
      getSignature(invokable).toString() + ")";
  }

  public static boolean isSInvokable(final DynamicObject obj) {
    return Universe.getCurrent().environmentInObect() ?
        InvokableEnvInObjectLayoutImpl.INSTANCE.isInvokableEnvInObject(obj) :
        InvokableLayoutImpl.INSTANCE.isInvokable(obj);
  }

  public static final class SMethod extends SInvokable {
    @Layout
    public interface MethodLayout extends InvokableLayout {
      DynamicObject[] getEmbeddedBlocks(DynamicObject object);
      DynamicObject createMethod(DynamicObjectFactory factory, SSymbol signature, Invokable invokable,
          RootCallTarget callTarget, Invokable invokableMeta, RootCallTarget callTargetMeta, DynamicObject holder, DynamicObject[] embeddedBlocks);
      DynamicObjectFactory createMethodShape(DynamicObject klass, DynamicObject environment);
      boolean isMethod(DynamicObject object);
      boolean isMethod(ObjectType objectType);
    }

    @Layout
    public interface MethodEnvInObjectLayout extends InvokableEnvInObjectLayout {
      DynamicObject[] getEmbeddedBlocks(DynamicObject object);
      DynamicObject createMethodEnvInObject(DynamicObjectFactory factory, DynamicObject environment, SSymbol signature, Invokable invokable,
          RootCallTarget callTarget, Invokable invokableMeta, RootCallTarget callTargetMeta, DynamicObject holder, DynamicObject[] embeddedBlocks);
      DynamicObjectFactory createMethodEnvInObjectShape(DynamicObject klass);
      boolean isMethodEnvInObject(DynamicObject object);
      boolean isMethodEnvInObject(ObjectType objectType);
    }

    private static final DynamicObjectFactory SMETHOD_FACTORY    = Universe.getCurrent().environmentInObect() ?
        MethodEnvInObjectLayoutImpl.INSTANCE.createMethodEnvInObjectShape(Classes.methodClass) :
        MethodLayoutImpl.INSTANCE.createMethodShape(Classes.methodClass, Nil.nilObject);

    public static DynamicObject create(final SSymbol signature, final Invokable invokable, final DynamicObject[] embeddedBlocks) {
      Invokable invokableMeta = (Invokable) invokable.deepCopy();
      return Universe.getCurrent().environmentInObect() ?
          MethodEnvInObjectLayoutImpl.INSTANCE.createMethodEnvInObject(SMETHOD_FACTORY, Nil.nilObject, signature, invokable,
              invokable.createCallTarget(), invokableMeta, invokableMeta.createCallTarget(), Nil.nilObject, embeddedBlocks) :
          MethodLayoutImpl.INSTANCE.createMethod(SMETHOD_FACTORY, signature, invokable,
          invokable.createCallTarget(), invokableMeta, invokableMeta.createCallTarget(), Nil.nilObject, embeddedBlocks);
    }

    public static void setHolder(final DynamicObject invokable, final DynamicObject value) {
      DynamicObject[] embeddedBlocks;
      if (Universe.getCurrent().environmentInObect()) {
        MethodEnvInObjectLayoutImpl.INSTANCE.setHolderUnsafe(invokable, value);
        embeddedBlocks = MethodEnvInObjectLayoutImpl.INSTANCE.getEmbeddedBlocks(invokable);
      } else {
        MethodLayoutImpl.INSTANCE.setHolderUnsafe(invokable, value);
        embeddedBlocks = MethodLayoutImpl.INSTANCE.getEmbeddedBlocks(invokable);
      }
      for (DynamicObject methods : embeddedBlocks) {
        if (Universe.getCurrent().environmentInObect()) {
          MethodEnvInObjectLayoutImpl.INSTANCE.setHolderUnsafe(methods, value);
        } else {
          MethodLayoutImpl.INSTANCE.setHolderUnsafe(methods, value);
        }
      }
    }

    public static boolean isSMethod(final DynamicObject obj) {
      return Universe.getCurrent().environmentInObect() ?
          MethodEnvInObjectLayoutImpl.INSTANCE.isMethodEnvInObject(obj) :
          MethodLayoutImpl.INSTANCE.isMethod(obj);
    }
  }

  public static final class SPrimitive extends SInvokable {
    public static boolean isSPrimitive(final DynamicObject obj) {
      return SInvokable.isSInvokable(obj) && !SMethod.isSMethod(obj);
    }
  }
}
