package som.interpreter.nodes.dispatch;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;
import som.interpreter.SArguments;
import som.primitives.BlockPrims.ValuePrimitiveNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;


public final class UninitializedValuePrimDispatchNode
    extends AbstractDispatchNode {

  private AbstractDispatchNode specialize(final VirtualFrame frame, final SBlock rcvr) {
    transferToInterpreterAndInvalidate("Initialize a dispatch node.");

    // Determine position in dispatch node chain, i.e., size of inline cache
    Node i = this;
    int chainDepth = 0;
    while (i.getParent() instanceof AbstractDispatchNode) {
      i = i.getParent();
      chainDepth++;
    }
    ValuePrimitiveNode primitiveNode = (ValuePrimitiveNode) i.getParent();

    if (chainDepth < INLINE_CACHE_SIZE) {
      DynamicObject method = rcvr.getMethod();

      assert method != null;
      UninitializedValuePrimDispatchNode uninitialized = new UninitializedValuePrimDispatchNode();
      CachedDispatchNode node = new CachedDispatchNode(
          DispatchGuard.createForBlock(rcvr), SInvokable.getCallTarget(method, SArguments.getExecutionLevel(frame)), uninitialized);
      return replace(node);
    } else {
      GenericBlockDispatchNode generic = new GenericBlockDispatchNode();
      primitiveNode.adoptNewDispatchListHead(generic);
      return generic;
    }
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final DynamicObject environment, final ExecutionLevel exLevel, final Object[] arguments) {
    return specialize(frame, (SBlock) arguments[0]).
        executeDispatch(frame, environment, exLevel, arguments);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 0;
  }
}
