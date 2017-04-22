package som.interpreter.nodes.dispatch;

import som.interpreter.SArguments;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SArray;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;


public final class GenericBlockDispatchNode extends AbstractDispatchNode {
  public GenericBlockDispatchNode(SourceSection source) {
    super(source);
  }

  @Child private IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();

  @Override
  public Object executeDispatch(final VirtualFrame frame, final DynamicObject environment, final ExecutionLevel exLevel,
      final Object[] arguments) {
    SBlock rcvr = (SBlock) arguments[0];
    return SInvokable.invoke(rcvr.getMethod(), frame, call, SArguments.createSArguments(environment, exLevel, arguments));
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1000;
  }
}
