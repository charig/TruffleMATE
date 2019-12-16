package som.interpreter.objectstorage;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.object.DynamicObject;

import som.interpreter.MateNode;
import som.interpreter.objectstorage.FieldAccessorNode.ReadFieldNode;
import som.matenodes.IntercessionHandling;
import som.vm.Universe;
import som.vm.constants.ReflectiveOp;


public final class MateLayoutFieldReadNode extends ReadFieldNode
    implements MateNode {
  @Child private IntercessionHandling ih;
  @Child private ReadFieldNode read;

  public MateLayoutFieldReadNode(final ReadFieldNode node) {
    super(node.getFieldIndex());
    ih = IntercessionHandling.createForOperation(ReflectiveOp.LayoutReadField);
    read = node;
    this.adoptChildren();
  }

  public Object read(final VirtualFrame frame, final DynamicObject receiver) {
    Object value = ih.doMateSemantics(frame, new Object[] {receiver, (long) this.getFieldIndex()});
    if (value == null) {
     value = read.executeRead(receiver);
    }
    return value;
  }

  @Override
  public Object executeRead(final DynamicObject obj) {
    /*Should never enter here*/
    assert (false);
    Universe.errorExit("Mate enters an unexpected method");
    return null;
  }

  @Override
  public NodeCost getCost() {
    return NodeCost.NONE;
  }
}
