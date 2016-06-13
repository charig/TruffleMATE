package som.primitives;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.PreevaluatedExpression;
import som.interpreter.nodes.dispatch.InvokeOnCache;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.primitives.arrays.ToArgumentsArrayNode;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SArray;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;


public final class MethodPrims {

  @GenerateNodeFactory
  public abstract static class SignaturePrim extends UnaryExpressionNode {
    @Specialization
    public final SAbstractObject doSMethod(final DynamicObject receiver) {
      return SInvokable.getSignature(receiver);
    }
  }

  @GenerateNodeFactory
  public abstract static class HolderPrim extends UnaryExpressionNode {
    @Specialization
    public final DynamicObject doSMethod(final DynamicObject receiver) {
      return SInvokable.getHolder(receiver);
    }
  }

  @GenerateNodeFactory
  @NodeChildren({
    @NodeChild(value = "receiver", type = ExpressionNode.class),
    @NodeChild(value = "target",  type = ExpressionNode.class),
    @NodeChild(value = "somArr", type = ExpressionNode.class),
    @NodeChild(value = "argArr", type = ToArgumentsArrayNode.class,
               executeWith = {"somArr", "target"})})
  public abstract static class InvokeOnPrim extends ExpressionNode
    implements PreevaluatedExpression {
    @Child private InvokeOnCache callNode;
    
    public abstract ExpressionNode getReceiver();
    public abstract ExpressionNode getTarget();
    public abstract ExpressionNode getSomArr();
    public abstract ToArgumentsArrayNode getArgArr();

    public InvokeOnPrim() {
      super(null);
      callNode = InvokeOnCache.create();
    }
    public InvokeOnPrim(final InvokeOnPrim node) { this(); }

    public abstract Object executeEvaluated(final VirtualFrame frame,
        final DynamicObject receiver, final Object target, final SArray somArr);

    @Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] args) {
      return executeEvaluated(frame, (DynamicObject) args[0], args[1], (SArray) args[2]);
    }

    @Specialization
    public final Object doInvoke(final VirtualFrame frame,
        final DynamicObject receiver, final Object target, final SArray somArr,
        final Object[] argArr) {
      return callNode.executeDispatch(frame, receiver, argArr);
    }
    
    public Object[] evaluateArguments(final VirtualFrame frame){
      return this.getArgArr().executedEvaluated(this.getSomArr().executeGeneric(frame), this.getTarget().executeGeneric(frame));
    }
  }
}
