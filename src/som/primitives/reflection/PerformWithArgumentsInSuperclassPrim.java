package som.primitives.reflection;

import som.interpreter.SArguments;
import som.interpreter.nodes.nary.QuaternaryExpressionNode;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;


@GenerateNodeFactory
public abstract class PerformWithArgumentsInSuperclassPrim extends QuaternaryExpressionNode {
  @Child private IndirectCallNode call;
  public PerformWithArgumentsInSuperclassPrim() {
    super(null);
    call = Truffle.getRuntime().createIndirectCallNode();
  }

  @Specialization
  public final Object doSAbstractObject(final VirtualFrame frame,
      final Object receiver, final SSymbol selector,
      final Object[] argArr, final DynamicObject clazz) {
    CompilerAsserts.neverPartOfCompilation("PerformWithArgumentsInSuperclassPrim.doSAbstractObject()");
    DynamicObject invokable = SClass.lookupInvokable(clazz, selector);
    return call.call(frame, SInvokable.getCallTarget(invokable, SArguments.getExecutionLevel(frame)), SArguments.createSArguments(SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), mergeReceiverWithArguments(receiver, argArr)));
  }

  // TODO: remove duplicated code, also in symbol dispatch, ideally removing by optimizing this implementation...
  @ExplodeLoop
  private static Object[] mergeReceiverWithArguments(final Object receiver, final Object[] argsArray) {
    Object[] arguments = new Object[argsArray.length + 1];
    arguments[0] = receiver;
    for (int i = 0; i < argsArray.length; i++) {
      arguments[i + 1] = argsArray[i];
    }
    return arguments;
  }
}
