package som.interpreter.nodes;

import som.compiler.Variable.Local;
import som.interpreter.MateNode;
import som.interpreter.nodes.LocalVariableNode.LocalVariableReadNode;
import som.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import som.interpreter.nodes.LocalVariableNodeFactory.LocalVariableReadNodeGen;
import som.interpreter.nodes.LocalVariableNodeFactory.LocalVariableWriteNodeGen;


public abstract class MateUninitializedVariableNode extends UninitializedVariableNode
    implements MateNode {

  public MateUninitializedVariableNode(final Local variable, final int contextLevel) {
    super(variable, contextLevel);
  }

  public static final class MateUninitializedVariableReadNode extends UninitializedVariableReadNode {
    public MateUninitializedVariableReadNode(final UninitializedVariableReadNode node) {
      super(node.variable, node.getContextLevel());
      initialize(node.getSourceSection());
    }

    @Override
    protected LocalVariableReadNode specializedNode() {
      return new MateLocalVariableNode.MateLocalVariableReadNode(
          LocalVariableReadNodeGen.create(variable)).initialize(getSourceSection());
    }

    @Override
    public ExpressionNode asMateNode() {
      return null;
    }
  }

  public static final class MateUninitializedVariableWriteNode extends UninitializedVariableWriteNode {
    public MateUninitializedVariableWriteNode(final UninitializedVariableWriteNode node) {
      super(node.variable, node.getContextLevel(), node.exp);
      initialize(node.getSourceSection());
    }

    @Override
    protected LocalVariableWriteNode specializedNode() {
      return new MateLocalVariableNode.MateLocalVariableWriteNode(
          LocalVariableWriteNodeGen.create(variable, exp)).initialize(getSourceSection());
    }

    @Override
    public ExpressionNode asMateNode() {
      return null;
    }
  }
}
