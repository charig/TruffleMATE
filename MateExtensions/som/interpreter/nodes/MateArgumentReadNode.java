package som.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;

import som.interpreter.MateNode;
import som.interpreter.SArguments;
import som.interpreter.nodes.ArgumentReadNode.LocalArgumentReadNode;
import som.interpreter.nodes.ArgumentReadNode.LocalSuperReadNode;
import som.interpreter.nodes.ArgumentReadNode.NonLocalArgumentReadNode;
import som.interpreter.nodes.ArgumentReadNode.NonLocalSuperReadNode;
import som.matenodes.IntercessionHandling;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SSymbol;

public abstract class MateArgumentReadNode {
  public static class MateLocalArgumentReadNode extends LocalArgumentReadNode
      implements MateNode {
    @Child private IntercessionHandling ih;

    public MateLocalArgumentReadNode(final int argumentIndex) {
      super(argumentIndex);
      ih = IntercessionHandling.createForOperation(ReflectiveOp.ExecutorLocalArg);
      this.adoptChildren();
    }

    public MateLocalArgumentReadNode(final LocalArgumentReadNode node) {
      this(node.argumentIndex);
      this.initialize(node.getSourceSection());
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object value = ih.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame), (long) argumentIndex});
      if (value == null) {
       value = super.executeGeneric(frame);
      }
      return value;
    }

    @Override
    public ExpressionNode asMateNode() {
      return null;
    }
  }

  public static class MateNonLocalArgumentReadNode extends NonLocalArgumentReadNode{
    @Child private IntercessionHandling ih;

    public MateNonLocalArgumentReadNode(final int argumentIndex, final int contextLevel) {
      super(argumentIndex, contextLevel);
      ih = IntercessionHandling.createForOperation(ReflectiveOp.ExecutorNonLocalArg);
      this.adoptChildren();
    }

    public MateNonLocalArgumentReadNode(final NonLocalArgumentReadNode node) {
      this(node.argumentIndex, node.contextLevel);
      initialize(node.getSourceSection());
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object value = ih.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)});
      if (value == null) {
       value = super.executeGeneric(frame);
      }
      return value;
    }

    @Override
    public ExpressionNode asMateNode() {
      return null;
    }
  }

  public static final class MateLocalSuperReadNode extends LocalSuperReadNode implements
      ISuperReadNode{
    @Child private IntercessionHandling ih;

    public MateLocalSuperReadNode(final SSymbol holderClass, final boolean classSide) {
      super(holderClass, classSide);
      ih = IntercessionHandling.createForOperation(ReflectiveOp.ExecutorLocalSuperArg);
      this.adoptChildren();
    }

    public MateLocalSuperReadNode(final LocalSuperReadNode node) {
      this(node.getHolderClass(), node.isClassSide());
      initialize(node.getSourceSection());
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object value = ih.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)});
      if (value == null) {
       value = super.executeGeneric(frame);
      }
      return value;
    }

    @Override
    public ExpressionNode asMateNode() {
      return null;
    }

    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }
  }

  public static final class MateNonLocalSuperReadNode extends NonLocalSuperReadNode implements
      ISuperReadNode {
    @Child private IntercessionHandling ih;

    public MateNonLocalSuperReadNode(final int contextLevel, final SSymbol holderClass,
        final boolean classSide) {
      super(contextLevel, holderClass, classSide);
      ih = IntercessionHandling.createForOperation(ReflectiveOp.ExecutorNonLocalSuperArg);
      this.adoptChildren();
    }

    public MateNonLocalSuperReadNode(final NonLocalSuperReadNode node) {
      this(node.getContextLevel(), node.getHolderClass(), node.isClassSide());
      initialize(node.getSourceSection());
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      Object value = ih.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)});
      if (value == null) {
       value = super.executeGeneric(frame);
      }
      return value;
    }

    @Override
    public ExpressionNode asMateNode() {
      return null;
    }

    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }
  }
}
