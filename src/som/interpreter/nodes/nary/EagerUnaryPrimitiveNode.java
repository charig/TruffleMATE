package som.interpreter.nodes.nary;

import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import som.interpreter.SArguments;
import som.interpreter.TruffleCompiler;
import som.interpreter.nodes.ExpressionNode;
import som.vm.NotYetImplementedException;
import som.vmobjects.SSymbol;


public class EagerUnaryPrimitiveNode extends EagerPrimitive {

  @Child private ExpressionNode receiver;
  @Child private UnaryExpressionNode primitive;

  @Override
  public ExpressionNode getReceiver() { return receiver; }
  @Override
  protected ExpressionNode getPrimitive() { return primitive; }

  public EagerUnaryPrimitiveNode(final SSymbol selector,
      final ExpressionNode receiver, final UnaryExpressionNode primitive) {
    super(selector);
    this.receiver  = receiver;
    this.primitive = primitive;
    this.adoptChildren();
  }

  @Override
  public Object executeGenericWithReceiver(final VirtualFrame frame, final Object receiver) {
    return executeEvaluated(frame, receiver);
  }

  public Object executeEvaluated(final VirtualFrame frame,
      final Object receiver) {
    try {
      return primitive.executeEvaluated(frame, receiver);
    } catch (UnsupportedSpecializationException e) {
      TruffleCompiler.transferToInterpreterAndInvalidate("Eager Primitive with unsupported specialization.");
      return replaceWithGenericSend(SArguments.getExecutionLevel(frame)).doPreEvaluated(frame, new Object[] {receiver});
    }
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    return executeEvaluated(frame, args[0]);
  }

  @Override
  protected void onReplace(final Node newNode, final CharSequence reason) {
    if (newNode instanceof ExpressionWithTagsNode) {
      ((ExpressionWithTagsNode) newNode).tagMark = primitive.tagMark;
    } else if (newNode instanceof WrapperNode) {
      assert ((WrapperNode) newNode).getDelegateNode() == this : "Wrapping should not also do specialization or other changes, I think";
    } else {
      throw new NotYetImplementedException();
    }
  }

  @Override
  protected ExpressionNode[] getArgumentNodes() {
    return new ExpressionNode[] {receiver};
  }
}
