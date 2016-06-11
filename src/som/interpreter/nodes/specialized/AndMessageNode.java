package som.interpreter.nodes.specialized;

import som.interpreter.SArguments;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;


@GenerateNodeFactory
public abstract class AndMessageNode extends BinaryExpressionNode {

  private final DynamicObject blockMethod;
  @Child private DirectCallNode blockValueSend;

  public AndMessageNode(final SBlock arg, final SourceSection source, ExecutionLevel level) {
    super(source);
    blockMethod = arg.getMethod();
    blockValueSend = Truffle.getRuntime().createDirectCallNode(
        SInvokable.getCallTarget(blockMethod, level));
  }

  public AndMessageNode(final AndMessageNode copy) {
    super(copy.getSourceSection());
    blockMethod    = copy.blockMethod;
    blockValueSend = copy.blockValueSend;
  }

  protected final boolean isSameBlock(final SBlock argument) {
    return argument.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlock(argument)")
  public final boolean doAnd(final VirtualFrame frame, final boolean receiver,
      final SBlock argument) {
    if (receiver == false) {
      return false;
    } else {
      return (boolean) blockValueSend.call(frame, new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), argument});
    }
  }

  @GenerateNodeFactory
  public abstract static class AndBoolMessageNode extends BinaryExpressionNode {

    public AndBoolMessageNode(final SourceSection source) {
      super(source);
    }

    @Specialization
    public final boolean doAnd(final VirtualFrame frame, final boolean receiver, final boolean argument) {
      return receiver && argument;
    }
  }
}
