package som.interpreter.nodes.specialized;

import som.interpreter.Invokable;
import som.interpreter.SArguments;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;


public abstract class IntToDoMessageNode extends TernaryExpressionNode {

  private final DynamicObject blockMethod;
  @Child private DirectCallNode valueSend;

  public IntToDoMessageNode(final ExpressionNode orignialNode,
      final SBlock block, ExecutionLevel level) {
    super(orignialNode.getSourceSection());
    blockMethod = block.getMethod();
    valueSend = Truffle.getRuntime().createDirectCallNode(
                    SInvokable.getCallTarget(blockMethod, level));
  }

  public IntToDoMessageNode(final IntToDoMessageNode node) {
    super(node.getSourceSection());
    this.blockMethod = node.blockMethod;
    this.valueSend   = node.valueSend;
  }

  protected final boolean isSameBlockLong(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlockLong(block)")
  public final long doIntToDo(final VirtualFrame frame, final long receiver, final long limit, final SBlock block) {
    try {
      doLooping(frame, receiver, limit, block);
    } finally {
      if (CompilerDirectives.inInterpreter() && (limit - receiver) > 0) {
        reportLoopCount(limit - receiver);
      }
    }
    return receiver;
  }

  protected final boolean isSameBlockDouble(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlockDouble(block)")
  public final long doIntToDo(final VirtualFrame frame, final long receiver, final double dLimit, final SBlock block) {
    long limit = (long) dLimit;
    try {
      doLooping(frame, receiver, limit, block);
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount((int) limit - receiver);
      }
    }
    return receiver;
  }

  protected void doLooping(final VirtualFrame frame, final long receiver,
      long limit, final SBlock block) {
    if (receiver <= limit) {
      valueSend.call(frame, new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), block, receiver});
    }
    for (long i = receiver + 1; i <= limit; i++) {
      valueSend.call(frame, new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), block, i});
    }
  }

  private void reportLoopCount(final long count) {
    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = getParent();
    while (current != null && !(current instanceof RootNode)) {
      current = current.getParent();
    }
    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(count);
    }
  }
}
