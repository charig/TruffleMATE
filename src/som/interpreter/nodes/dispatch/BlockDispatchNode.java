package som.interpreter.nodes.dispatch;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

import som.interpreter.SArguments;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;

@ReportPolymorphism
public abstract class BlockDispatchNode extends Node {
  public static final int INLINE_CACHE_SIZE = 4;
  public abstract Object executeDispatch(VirtualFrame frame, Object[] arguments);

  protected static final boolean isSameMethod(final Object[] arguments,
      final DynamicObject cached) {
    if (!(arguments[0] instanceof SBlock)) {
      return false;
    }
    return getMethod(arguments) == cached;
  }

  protected static final DynamicObject getMethod(final Object[] arguments) {
    DynamicObject method = ((SBlock) arguments[0]).getMethod();
    assert SMethod.getNumberOfArguments(method) == arguments.length;
    return method;
  }

  protected static final DirectCallNode createCallNode(final Object[] arguments,
      final VirtualFrame frame) {
    DirectCallNode node = Truffle.getRuntime().createDirectCallNode(
        SInvokable.getCallTarget(getMethod(arguments), SArguments.getExecutionLevel(frame)));
    node.forceInlining();
    return node;
  }

  @Specialization(guards = "isSameMethod(arguments, cached)", limit = "INLINE_CACHE_SIZE")
  public Object activateBlock(final VirtualFrame frame, final Object[] arguments,
      @Cached("getMethod(arguments)") final DynamicObject cached,
      @Cached("createCallNode(arguments, frame)") final DirectCallNode call) {
    return call.call(SArguments.createSArguments(SArguments.getEnvironment(frame),
        SArguments.getExecutionLevel(frame), arguments));
  }

  @Child protected IndirectCallNode indirect = Truffle.getRuntime().createIndirectCallNode();

  @Specialization()
  public Object activateBlock(final VirtualFrame frame, final Object[] arguments) {
    return indirect.call(
        SInvokable.getCallTarget(getMethod(arguments), SArguments.getExecutionLevel(frame)),
        SArguments.createSArguments(SArguments.getEnvironment(frame),
            SArguments.getExecutionLevel(frame), arguments));
  }
}
