package som.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;

public final class EmptyPrim extends UnaryExpressionNode {

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    return executeEvaluated(frame, null);
  }

  @Override
  public Object executeGenericWithReceiver(final VirtualFrame frame, final Object receiver) {
    return executeGeneric(frame);
  }

  @Override
  public Object executeEvaluated(final VirtualFrame frame, final Object receiver) {
    Universe.println("Warning: undefined primitive called");
    return null;
  }

  public static EmptyPrim create() {
    return new EmptyPrim();
  }

  @Override
  public ExpressionNode getReceiver() {
    return null;
  }
}
