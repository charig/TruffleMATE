package som.interpreter.nodes.specialized;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.SArguments;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.specialized.AndMessageNode.AndOrSplzr;
import som.interpreter.nodes.specialized.OrMessageNode.OrSplzr;
import som.interpreter.nodes.specialized.OrMessageNodeFactory.OrBoolMessageNodeFactory;
import som.primitives.Primitive;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import tools.dym.Tags.ControlFlowCondition;
import tools.dym.Tags.OpComparison;


@Primitive(selector = "or:",  specializer = OrSplzr.class)
@Primitive(selector = "||",   specializer = OrSplzr.class)
@GenerateNodeFactory
public abstract class OrMessageNode extends BinaryExpressionNode {
  private final DynamicObject blockMethod;
  @Child private DirectCallNode blockValueSend;

  public static final class OrSplzr extends AndOrSplzr {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public OrSplzr(final Primitive prim, final NodeFactory<BinaryExpressionNode> fact) {
      super(prim, fact, (NodeFactory) OrBoolMessageNodeFactory.getInstance());
    }
  }

  public OrMessageNode(final SBlock arg, final SourceSection source, final ExecutionLevel level) {
    super(true, source);
    blockMethod = arg.getMethod();
    blockValueSend = Truffle.getRuntime().createDirectCallNode(
        SInvokable.getCallTarget(blockMethod, level));
  }

  protected final boolean isSameBlock(final SBlock argument) {
    return argument.getMethod() == blockMethod;
  }

  @Specialization(guards = "isSameBlock(argument)")
  public final boolean doOr(final VirtualFrame frame, final boolean receiver,
      final SBlock argument) {
    if (receiver) {
      return true;
    } else {
      return (boolean) blockValueSend.call(new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), argument});
    }
  }

  @GenerateNodeFactory
  public abstract static class OrBoolMessageNode extends BinaryExpressionNode {
    public OrBoolMessageNode(final SourceSection source) {
      super(false, source);
    }

    @Specialization
    public final boolean doOr(final VirtualFrame frame, final boolean receiver,
        final boolean argument) {
      return receiver || argument;
    }
  }

  @Override
  protected boolean isTaggedWithIgnoringEagerness(final Class<?> tag) {
    if (tag == ControlFlowCondition.class) {
      return true;
    } else if (tag == OpComparison.class) {
      return true;
    } else {
      return super.isTaggedWithIgnoringEagerness(tag);
    }
  }
}
