package som.interpreter.nodes.specialized;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import bd.primitives.Specializer;
import som.interpreter.SArguments;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.literals.BlockNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.specialized.AndMessagesNodes.AndMessageNode.AndOrSplzr;
import som.interpreter.nodes.specialized.AndMessagesNodesFactory.AndBoolMessageNodeFactory;
import som.vm.Universe;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;
import tools.dym.Tags.ControlFlowCondition;
import tools.dym.Tags.OpComparison;


public class AndMessagesNodes {

  @GenerateNodeFactory
  public abstract static class AndBoolMessageNode extends BinaryExpressionNode {

    @Specialization
    public final boolean doAnd(final VirtualFrame frame, final boolean receiver, final boolean argument) {
      return receiver && argument;
    }
  }

  @GenerateNodeFactory
  @Primitive(selector = "and:", noWrapper = true, specializer = AndOrSplzr.class)
  @Primitive(selector = "&&",   noWrapper = true, specializer = AndOrSplzr.class)
  public abstract static class AndMessageNode extends BinaryExpressionNode {
    private final DynamicObject blockMethod;
    @Child private DirectCallNode blockValueSend;

    public static class AndOrSplzr extends Specializer<Universe, ExpressionNode, SSymbol> {
      protected final NodeFactory<BinaryExpressionNode> boolFact;

      @SuppressWarnings({ "unchecked", "rawtypes" })
      public AndOrSplzr(final Primitive prim, final NodeFactory<ExpressionNode> fact) {
        this(prim, fact, (NodeFactory) AndBoolMessageNodeFactory.getInstance());
      }

      protected AndOrSplzr(final Primitive prim, final NodeFactory<ExpressionNode> msgFact,
          final NodeFactory<BinaryExpressionNode> boolFact) {
        super(prim, msgFact);
        this.boolFact = boolFact;
      }

      @Override
      public final boolean matches(final Object[] args, final ExpressionNode[] argNodes) {
        return args[0] instanceof Boolean &&
            (args[1] instanceof Boolean ||
                unwrapIfNecessary(argNodes[1]) instanceof BlockNode);
      }

      @Override
      public final ExpressionNode create(final Object[] arguments,
          final ExpressionNode[] argNodes, final SourceSection section,
          final boolean eagerWrapper, final Universe vm) {
        BinaryExpressionNode node;
        if (unwrapIfNecessary(argNodes[1]) instanceof BlockNode) {
          node = (BinaryExpressionNode) fact.createNode(arguments[1],
              SArguments.getExecutionLevel(vm.getTruffleRuntime().getCurrentFrame().getFrame(FrameAccess.READ_ONLY)),
              argNodes[0], argNodes[1]);
        } else {
          assert arguments[1] instanceof Boolean;
          node = boolFact.createNode(argNodes[0], argNodes[1]);
        }
        node.initialize(section, eagerWrapper);
        return node;
      }
    }

    public AndMessageNode(final SBlock arg, final ExecutionLevel level) {
      blockMethod = arg.getMethod();
      blockValueSend = Truffle.getRuntime().createDirectCallNode(
          SInvokable.getCallTarget(blockMethod, level));
      blockValueSend.forceInlining();
      this.adoptChildren();
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
        return (boolean) blockValueSend.call(new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), argument});
      }
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == ControlFlowCondition.class) {
        return true;
      } else if (tag == OpComparison.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }
}
