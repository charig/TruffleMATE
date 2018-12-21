package som.primitives;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import som.interpreter.nodes.PreevaluatedExpression;
import som.interpreter.nodes.dispatch.InvokeOnCache;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.primitives.arrays.ToArgumentsArrayNode;
import som.primitives.arrays.ToArgumentsArrayNodeFactory;
import som.vmobjects.SArray;

@GenerateNodeFactory
@Primitive(className = "Method", primitive = "invokeOn:with:", selector = "invokeOn:with:", noWrapper = true)
@Primitive(className = "Primitive", primitive = "invokeOn:with:", noWrapper = true)
public abstract class InvokeOnPrim extends TernaryExpressionNode
  implements PreevaluatedExpression {

  @Child private InvokeOnCache callNode = InvokeOnCache.create();

  public abstract Object executeEvaluated(VirtualFrame frame,
      DynamicObject receiver, Object target, SArray somArr);

  @Override
  public final Object doPreEvaluated(final VirtualFrame frame,
      final Object[] args) {
    return executeEvaluated(frame, (DynamicObject) args[0], args[1], (SArray) args[2]);
  }

  public static final ToArgumentsArrayNode createArgArrayNode() {
    return ToArgumentsArrayNodeFactory.getInstance().createNode(false, null);
  }

  @Specialization
  public final Object doInvoke(final VirtualFrame frame,
      final DynamicObject receiver, final Object target, final SArray somArr,
      @Cached("createArgArrayNode()") final ToArgumentsArrayNode toArgArray) {
    Object[] arguments = toArgArray.executedEvaluated(somArr, target);
    return callNode.executeDispatch(frame, receiver, arguments);
  }
}
