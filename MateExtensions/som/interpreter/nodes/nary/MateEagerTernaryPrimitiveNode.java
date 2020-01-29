package som.interpreter.nodes.nary;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.AbstractMessageSpecializationsFactory;
import som.interpreter.nodes.ExpressionNode;
import som.matenodes.IntercessionHandling;
import som.vm.Universe;
import som.vmobjects.SSymbol;


public class MateEagerTernaryPrimitiveNode extends EagerTernaryPrimitiveNode {
  @Child private IntercessionHandling messageSend;
  @Child private IntercessionHandling primitiveActivation;

  public MateEagerTernaryPrimitiveNode(final SSymbol selector, final ExpressionNode receiver, final ExpressionNode argument1, final ExpressionNode argument2,
      final TernaryExpressionNode primitive) {
    super(selector, receiver, argument1, argument2, primitive);
    messageSend = IntercessionHandling.createForMessageLookup(this.getSelector());
    primitiveActivation = IntercessionHandling.createForMethodActivation(selector);
    this.adoptChildren();
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = this.getReceiver().executeGeneric(frame);
    Object arg1 = this.getFirstArg().executeGeneric(frame);
    Object arg2 = this.getSecondArg().executeGeneric(frame);
    return this.doPreEvaluated(frame, new Object[] {rcvr, arg1, arg2});
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    Object value = messageSend.doMateSemantics(frame, args);
    if (value == null) {
     value = executeEvaluated(frame, args[0], args[1], args[2]);
    }
    return value;
  }

  @Override
  public Object executeEvaluated(final VirtualFrame frame,
      final Object receiver, final Object argument1, final Object argument2) {
    Object[] realArgs = (Object[]) primitiveActivation.doMateSemantics(frame, new Object[]{receiver, argument1, argument2});
    if (realArgs == null) {
      return super.executeEvaluated(frame, receiver, argument1, argument2);
    } else {
      return super.executeEvaluated(frame, realArgs[2], realArgs[3], realArgs[4]);
    }
  }

  @Override
  protected AbstractMessageSpecializationsFactory getFactory() {
    return Universe.getCurrent().mateSpecializationFactory;
  }
}
