package som.interpreter.nodes.nary;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.interpreter.nodes.AbstractMessageSpecializationsFactory;
import som.interpreter.nodes.ExpressionNode;
import som.matenodes.IntercessionHandling;
import som.vm.Universe;
import som.vmobjects.SSymbol;


public class MateEagerUnaryPrimitiveNode extends EagerUnaryPrimitiveNode {
  @Child private IntercessionHandling messageSend;
  @Child private IntercessionHandling primitiveActivation;

  public MateEagerUnaryPrimitiveNode(final SSymbol selector, final ExpressionNode receiver,
      final UnaryExpressionNode primitive) {
    super(selector, receiver, primitive);
    messageSend = IntercessionHandling.createForMessageLookup(this.getSelector());
    primitiveActivation = IntercessionHandling.createForMethodActivation(selector);
    this.adoptChildren();
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = this.getReceiver().executeGeneric(frame);
    return this.doPreEvaluated(frame, new Object[] {rcvr});
  }

  @Override
  public Object doPreEvaluated(final VirtualFrame frame, final Object[] args) {
    Object value = messageSend.doMateSemantics(frame, args);
    if (value == null) {
     value = executeEvaluated(frame, args[0]);
    }
    return value;
  }

  @Override
  public Object executeEvaluated(final VirtualFrame frame, final Object receiver) {
    Object[] realArgs = (Object[]) primitiveActivation.doMateSemantics(frame, new Object[]{receiver});
    if (realArgs == null) {
      return super.executeEvaluated(frame, receiver);
    } else {
      return super.executeEvaluated(frame, realArgs[2]);
    }
  }

  @Override
  protected AbstractMessageSpecializationsFactory getFactory() {
    return Universe.getCurrent().mateSpecializationFactory;
  }
}
