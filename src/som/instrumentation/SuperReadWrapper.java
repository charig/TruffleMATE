package som.instrumentation;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode.WrapperNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.nodes.NodeCost;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.ISuperReadNode;
import som.vmobjects.SSymbol;

/*I needed to implement a special wrapper for the superRead nodes because
 * dispatching/specialization for super sends depend on the argument node being a ISuperReadNode*/
public final class SuperReadWrapper extends ExpressionNode implements WrapperNode, ISuperReadNode {

  @Child private ExpressionNode delegateNode;
  @Child private ProbeNode      probeNode;

  public SuperReadWrapper(final ExpressionNode delegateNode,
      final ProbeNode probeNode) {
    this.delegateNode = delegateNode;
    this.probeNode = probeNode;
  }

  @Override
  public ExpressionNode getDelegateNode() {
    return delegateNode;
  }

  @Override
  public ProbeNode getProbeNode() {
    return probeNode;
  }

  @Override
  public NodeCost getCost() {
    return NodeCost.NONE;
  }
  @Override
  public SSymbol getHolderClass() {
    return ((ISuperReadNode) getDelegateNode()).getHolderClass();
  }

  @Override
  public boolean isClassSide() {
    return ((ISuperReadNode) getDelegateNode()).isClassSide();
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    return getDelegateNode().executeGeneric(frame);
  }
}
