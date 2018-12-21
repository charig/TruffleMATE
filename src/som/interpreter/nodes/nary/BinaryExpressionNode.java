package som.interpreter.nodes.nary;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ProbeNode;

import som.instrumentation.FixedSizeExpressionWrapper;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.PreevaluatedExpression;
import som.vm.Universe;
import som.vmobjects.SSymbol;


@NodeChildren({
  @NodeChild(value = "receiver", type = ExpressionNode.class),
  @NodeChild(value = "argument", type = ExpressionNode.class)})
public abstract class BinaryExpressionNode extends EagerlySpecializableNode
    implements ExpressionWithReceiver, PreevaluatedExpression {

  public abstract ExpressionNode getArgument();

  public abstract Object executeEvaluated(VirtualFrame frame,
      Object receiver, Object argument);

  @Override
  public WrapperNode createWrapper(final ProbeNode probeNode) {
    return new FixedSizeExpressionWrapper(this, probeNode);
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1]);
  }

  @Override
  public EagerPrimitive wrapInEagerWrapper(final SSymbol selector,
      final ExpressionNode[] arguments, final Universe vm) {
    EagerPrimitive result =  vm.specializationFactory().binaryPrimitiveFor(selector, arguments[0], arguments[1], this);
    result.initialize(sourceSection);
    return result;
  }
}
