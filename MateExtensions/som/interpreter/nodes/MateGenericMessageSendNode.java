package som.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;

import som.interpreter.MateNode;
import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.interpreter.nodes.dispatch.AbstractDispatchNode;
import som.matenodes.IntercessionHandling;
import som.vmobjects.SSymbol;


public class MateGenericMessageSendNode extends GenericMessageSendNode
    implements MateNode {
  @Child private IntercessionHandling ih;

  protected MateGenericMessageSendNode(final SSymbol selector,
      final ExpressionNode[] arguments,
      final AbstractDispatchNode dispatchNode) {
    super(selector, arguments, dispatchNode);
    if (arguments.length > 0 && this.isSuperSend()) {
      ih = IntercessionHandling.createForSuperMessageLookup(this.getSelector(), (ISuperReadNode) this.argumentNodes[0]);
    } else {
      ih = IntercessionHandling.createForMessageLookup(this.getSelector());
    }
    this.adoptChildren();
  }

  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    Object[] arguments = evaluateArguments(frame);
    Object value = ih.doMateSemantics(frame, arguments);
    if (value == null) {
      value = super.doPreEvaluated(frame, arguments);
    }
    return value;
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    Object value = ih.doMateSemantics(frame, arguments);
    if (value == null) {
      value = super.doPreEvaluated(frame, arguments);
    }
    return value;
  }

  @Override
  public NodeCost getCost() {
    return NodeCost.NONE;
  }
}
