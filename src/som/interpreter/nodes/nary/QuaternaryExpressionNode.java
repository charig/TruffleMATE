package som.interpreter.nodes.nary;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.PreevaluatedExpression;
import som.vm.Universe;
import som.vmobjects.SSymbol;


@NodeChildren({
  @NodeChild(value = "receiver",  type = ExpressionNode.class),
  @NodeChild(value = "firstArg",  type = ExpressionNode.class),
  @NodeChild(value = "secondArg", type = ExpressionNode.class),
  @NodeChild(value = "thirdArg",  type = ExpressionNode.class)})
public abstract class QuaternaryExpressionNode extends EagerlySpecializableNode
    implements ExpressionWithReceiver, PreevaluatedExpression {

  public abstract ExpressionNode getFirstArg();
  public abstract ExpressionNode getSecondArg();
  public abstract ExpressionNode getThirdArg();

  public abstract Object executeEvaluated(VirtualFrame frame,
      Object receiver, Object firstArg, Object secondArg,
      Object thirdArg);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] arguments) {
    return executeEvaluated(frame, arguments[0], arguments[1], arguments[2], arguments[3]);
  }

  public Object[] evaluateArguments(final VirtualFrame frame) {
    Object[] arguments = new Object[4];
    arguments[0] = this.getReceiver().executeGeneric(frame);
    arguments[1] = this.getFirstArg().executeGeneric(frame);
    arguments[2] = this.getSecondArg().executeGeneric(frame);
    arguments[3] = this.getThirdArg().executeGeneric(frame);
    return arguments;
  }

  @Override
  public EagerPrimitive wrapInEagerWrapper(final SSymbol selector,
      final ExpressionNode[] arguments, final Universe vm) {
      EagerPrimitive result =  vm.specializationFactory().quaternaryPrimitiveFor(selector,
          arguments[0], arguments[1], arguments[2], arguments[3], this);
      result.initialize(sourceSection);
      return result;
   }
}
