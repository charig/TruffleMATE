package som.interpreter.nodes.nary;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.SArguments;
import som.interpreter.TruffleCompiler;
import som.interpreter.nodes.ExpressionNode;
import som.vmobjects.SSymbol;

public class EagerBinaryPrimitiveNode extends EagerPrimitive {

  @Child private ExpressionNode receiver;
  @Child private ExpressionNode argument;
  @Child private BinaryExpressionNode primitive;

  public EagerBinaryPrimitiveNode(
      final SSymbol selector,
      final ExpressionNode receiver,
      final ExpressionNode argument,
      final BinaryExpressionNode primitive) {
    super(selector);
    this.receiver  = receiver;
    this.argument  = argument;
    this.primitive = primitive;
    this.adoptChildren();
  }

  @Override
  public ExpressionNode getReceiver() {
    return receiver;
  }
  @Override
  protected BinaryExpressionNode getPrimitive() { return primitive; }

  @Override
  public Object executeGenericWithReceiver(final VirtualFrame frame, final Object receiver) {
    Object arg  = argument.executeGeneric(frame);
    return executeEvaluated(frame, receiver, arg);
  }

  public Object executeEvaluated(final VirtualFrame frame,
    final Object receiver, final Object argument) {
    try {
      return primitive.executeEvaluated(frame, receiver, argument);
    } catch (UnsupportedSpecializationException e) {
      TruffleCompiler.transferToInterpreterAndInvalidate("Eager Primitive with unsupported specialization.");
      return replaceWithGenericSend(SArguments.getExecutionLevel(frame)).doPreEvaluated(frame,
          new Object[] {receiver, argument});
    }
  }

  public ExpressionNode getArgument() {
    return argument;
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    return executeEvaluated(frame, args[0], args[1]);
  }

  @Override
  protected ExpressionNode[] getArgumentNodes() {
    return new ExpressionNode[] {receiver, argument};
  }
}
