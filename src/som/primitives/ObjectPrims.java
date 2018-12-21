package som.primitives;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import som.interpreter.Types;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.reflection.IndexDispatch;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

public final class ObjectPrims {

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "instVarAt:", selector = "instVarAt:")
  public abstract static class InstVarAtPrim extends BinaryExpressionNode {

    @Child private IndexDispatch dispatch;

    public InstVarAtPrim() {
      dispatch = IndexDispatch.create();
    }

    @Specialization
    public final Object doSObject(final DynamicObject receiver, final long idx) {
      return dispatch.executeDispatch(receiver, (int) idx - 1);
    }

    @Override
    public final Object executeEvaluated(final VirtualFrame frame,
      final Object receiver, final Object firstArg) {
      assert receiver instanceof DynamicObject;
      assert firstArg instanceof Long;

      DynamicObject rcvr = (DynamicObject) receiver;
      long idx     = (long) firstArg;
      return doSObject(rcvr, idx);
    }

    @Override
    public ReflectiveOp reflectiveOperation() {
      return ReflectiveOp.LayoutPrimReadField;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "instVarAt:put:", selector = "instVarAt:put:", noWrapper = true)
  public abstract static class InstVarAtPutPrim extends TernaryExpressionNode {
    @Child private IndexDispatch dispatch;

    public InstVarAtPutPrim() {
      dispatch = IndexDispatch.create();
    }

    @Specialization
    public final Object doSObject(final DynamicObject receiver, final long idx, final Object val) {
      dispatch.executeDispatch(receiver, (int) idx - 1, val);
      return val;
    }

    @Override
    public final Object executeEvaluated(final VirtualFrame frame,
      final Object receiver, final Object firstArg, final Object secondArg) {
      assert receiver instanceof DynamicObject;
      assert firstArg instanceof Long;
      assert secondArg != null;

      DynamicObject rcvr = (DynamicObject) receiver;
      long idx     = (long) firstArg;
      return doSObject(rcvr, idx, secondArg);
    }

    @Override
    public ReflectiveOp reflectiveOperation() {
      return ReflectiveOp.LayoutPrimWriteField;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "instVarNamed:", selector = "instVarNamed:")
  public abstract static class InstVarNamedPrim extends BinaryExpressionNode {
    // TODO: Specialize for optimization
    @TruffleBoundary
    @Specialization
    public final Object doSObject(final DynamicObject receiver, final SSymbol fieldName) {
      // CompilerAsserts.neverPartOfCompilation("InstVarNamedPrim");
      return receiver.get(SClass.lookupFieldIndex(SObject.getSOMClass(receiver), fieldName), Nil.nilObject);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "instVarNamed:put:", selector = "instVarNamed:put:")
  public abstract static class InstVarNamedPutPrim extends TernaryExpressionNode {
    // TODO: Specialize for optimization

    @TruffleBoundary
    @Specialization
    public final Object doSObject(final DynamicObject receiver, final SSymbol fieldName, final Object val) {
      receiver.define(SClass.lookupFieldIndex(SObject.getSOMClass(receiver), fieldName), val);
      return val;
    }
  }


  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "halt")
  public abstract static class HaltPrim extends UnaryExpressionNode {

    @Specialization
    public final Object doSAbstractObject(final Object receiver) {
      Universe.errorPrintln("BREAKPOINT");
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "class")
  public abstract static class ClassPrim extends UnaryExpressionNode {

    @Specialization
    public final DynamicObject doDynamicObject(final DynamicObject receiver) {
      return SObject.getSOMClass(receiver);
    }

    @Specialization
    public final DynamicObject doObject(final Object receiver) {
      return Types.getClassOf(receiver);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "shallowCopy")
  public abstract static class ShallowCopyPrim extends UnaryExpressionNode {

    @Specialization
    public final Object doSObject(final DynamicObject receiver) {
      return receiver.copy(receiver.getShape());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "identityHash")
  public abstract static class HashPrim extends UnaryExpressionNode {

    @Specialization
    @TruffleBoundary
    public final long doString(final String receiver) {
      return receiver.hashCode();
    }

    @Specialization
    @TruffleBoundary
    public final long doSSymbol(final SSymbol receiver) {
      return receiver.getString().hashCode();
    }

    @Specialization
    public final long doSObject(final DynamicObject receiver) {
      return receiver.hashCode();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Object", primitive = "objectSize")
  @ImportStatic(SObject.class)
  public abstract static class ObjectSizePrim extends UnaryExpressionNode {

    @Specialization
    public final long doArray(final Object[] receiver) {
      int size = 0;
      size += receiver.length;
      return size;
    }

    @Specialization // (guards = "isSObject(receiver)")
    public final long doSObject(final DynamicObject receiver) {
      int size = 0;
      size += SObject.getNumberOfFields(receiver);
      return size;
    }

    @Specialization
    public final long doSAbstractObject(final Object receiver) {
      return 0; // TODO: allow polymorphism?
    }
  }
}
