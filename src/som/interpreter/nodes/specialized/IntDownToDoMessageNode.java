package som.interpreter.nodes.specialized;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import bd.primitives.nodes.WithContext;
import som.interpreter.Invokable;
import som.interpreter.SArguments;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.specialized.IntToDoMessageNode.ToDoSplzr;
import som.vm.Universe;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import tools.dym.Tags.LoopNode;

@Primitive(primitive = "downTo:do:", selector = "downTo:do:", noWrapper = true, disabled = true,
           requiresArguments = true, specializer = ToDoSplzr.class)
@GenerateNodeFactory
public abstract class IntDownToDoMessageNode extends TernaryExpressionNode
  implements WithContext<IntDownToDoMessageNode, Universe> {

  private final DynamicObject blockMethod;
  @Child private DirectCallNode valueSend;


  @Override
  public IntDownToDoMessageNode initialize(final Universe vm) {
    valueSend = Truffle.getRuntime().createDirectCallNode(
        SInvokable.getCallTarget(blockMethod,
            SArguments.getExecutionLevel(vm.getTruffleRuntime().getCurrentFrame().getFrame(FrameAccess.READ_ONLY))));
    return this;
  };

  public IntDownToDoMessageNode(final Object[] args) {
    blockMethod = ((SBlock) args[2]).getMethod();
  }

  protected final boolean isSameBlockLong(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlockLong(block)")
  public final long doIntDownToDo(final VirtualFrame frame, final long receiver, final long limit, final SBlock block) {
    try {
      if (receiver >= limit) {
        valueSend.call(new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), block, receiver});
      }
      for (long i = receiver - 1; i >= limit; i--) {
        valueSend.call(new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter() && (receiver - limit) > 0) {
        reportLoopCount(receiver - limit);
      }
    }
    return receiver;
  }

  protected final boolean isSameBlockDouble(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlockDouble(block)")
  public final long doIntDownToDo(final VirtualFrame frame, final long receiver, final double limit, final SBlock block) {
    try {
      if (receiver >= limit) {
        valueSend.call(new Object[] {block, receiver});
      }
      for (long i = receiver - 1; i >= limit; i--) {
        valueSend.call(new Object[] {block, i});
      }
    } finally {
      if (CompilerDirectives.inInterpreter() && (receiver - (int) limit) > 0) {
        reportLoopCount(receiver - (int) limit);
      }
    }
    return receiver;
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

  @Override
  public Node asMateNode() {
    Universe.getCurrent().mateifyMethod(blockMethod);
    return super.asMateNode();
  }

  @Override
  protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
    if (tag == LoopNode.class) {
      return true;
    } else {
      return super.hasTagIgnoringEagerness(tag);
    }
  }
}
