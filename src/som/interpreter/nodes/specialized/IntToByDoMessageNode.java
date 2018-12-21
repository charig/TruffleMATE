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
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import bd.primitives.nodes.WithContext;
import som.interpreter.Invokable;
import som.interpreter.SArguments;
import som.interpreter.nodes.nary.QuaternaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import tools.dym.Tags.LoopNode;

//Should have noWrapper = true?
@Primitive(selector = "to:by:do:", disabled = true,
           requiresArguments = true)
@GenerateNodeFactory
public abstract class IntToByDoMessageNode extends QuaternaryExpressionNode
  implements WithContext<IntToByDoMessageNode, Universe> {

  private final DynamicObject blockMethod;
  @Child private DirectCallNode valueSend;

  @Override
  public IntToByDoMessageNode initialize(final Universe vm) {
    valueSend = Truffle.getRuntime().createDirectCallNode(
        SInvokable.getCallTarget(blockMethod,
            SArguments.getExecutionLevel(vm.getTruffleRuntime().getCurrentFrame().getFrame(FrameAccess.READ_ONLY))));
    return this;
  };

  public IntToByDoMessageNode(final Object[] args) {
    blockMethod = ((SBlock) args[3]).getMethod();
  }

  protected final boolean isSameBlockLong(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  protected final boolean isSameBlockDouble(final SBlock block) {
    return block.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlockLong(block)")
  public final long doIntToByDo(final VirtualFrame frame, final long receiver, final long limit, final long step, final SBlock block) {
    try {
      if (step > 0) {
        if (receiver <= limit) {
          valueSend.call(new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), block, receiver});
          for (long i = receiver + step; i <= limit; i += step) {
            valueSend.call(new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), block, i});
          }
        }
      } else {
        if (receiver >= limit) {
          valueSend.call(new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), block, receiver});
          for (long i = receiver + step; i >= limit; i += step) {
            valueSend.call(new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), block, i});
          }
        }
      }
    } finally {
      long count = Math.abs(limit) - Math.abs(receiver);
      if (CompilerDirectives.inInterpreter() && count > 0) {
        reportLoopCount(count);
      }
    }
    return receiver;
  }

  @Specialization(guards = "isSameBlockDouble(block)")
  public final long doIntToByDo(final VirtualFrame frame, final long receiver, final double limit, final long step, final SBlock block) {
    try {
      if (receiver <= limit) {
        valueSend.call(new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), block, receiver});
        for (long i = receiver + step; i <= limit; i += step) {
          valueSend.call(new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), block, i});
        }
      }
    } finally {
      long count = (long) (Math.abs(limit) - Math.abs(receiver));
      if (CompilerDirectives.inInterpreter() && count > 0) {
        reportLoopCount(count);
      }
    }
    return receiver;
  }

  protected final void reportLoopCount(final long count) {
    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = getParent();
    while (current != null && !(current instanceof Invokable)) {
      current = current.getParent();
    }
    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(count);
    }
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
