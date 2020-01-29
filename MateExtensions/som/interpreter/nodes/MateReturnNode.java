package som.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;

import som.interpreter.MateNode;
import som.interpreter.SArguments;
import som.interpreter.nodes.nary.ExpressionWithTagsNode;
import som.matenodes.IntercessionHandling;
import som.vm.constants.ReflectiveOp;

public class MateReturnNode extends ExpressionWithTagsNode
    implements MateNode {
  @Child IntercessionHandling ih;
  @Child ExpressionNode expression;

  public MateReturnNode(final ExpressionNode node) {
    expression = node;
    ih = IntercessionHandling.createForOperation(ReflectiveOp.ExecutorReturn);
    this.adoptChildren();
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object value = expression.executeGeneric(frame);
    Object valueRedefined = ih.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame), value});
    if (valueRedefined == null) {
      return value;
    }
    return valueRedefined;
  }

  @Override
  public NodeCost getCost() {
    return expression.getCost();
  }
}
