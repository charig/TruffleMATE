package som.interpreter.nodes;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import som.compiler.Variable.Local;
import som.interpreter.InlinerAdaptToEmbeddedOuterContext;
import som.interpreter.InlinerForLexicallyEmbeddedMethods;
import som.interpreter.SplitterForLexicallyEmbeddedCode;
import som.interpreter.nodes.LocalVariableNode.LocalVariableReadNode;
import som.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import som.interpreter.nodes.LocalVariableNodeFactory.LocalVariableReadNodeGen;
import som.interpreter.nodes.LocalVariableNodeFactory.LocalVariableWriteNodeGen;
import som.interpreter.nodes.NonLocalVariableNode.NonLocalVariableReadNode;
import som.interpreter.nodes.NonLocalVariableNode.NonLocalVariableWriteNode;
import som.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableReadNodeGen;
import som.interpreter.nodes.NonLocalVariableNodeFactory.NonLocalVariableWriteNodeGen;


public abstract class UninitializedVariableNode extends ContextualNode {
  protected final Local variable;

  public UninitializedVariableNode(final Local variable, final int contextLevel) {
    super(contextLevel);
    this.variable = variable;
  }

  public static class UninitializedVariableReadNode extends UninitializedVariableNode {
    public UninitializedVariableReadNode(final Local variable,
        final int contextLevel) {
      super(variable, contextLevel);
    }

    public UninitializedVariableReadNode(final UninitializedVariableReadNode node,
        final FrameSlot inlinedVarSlot) {
      this(node.variable.cloneForInlining(inlinedVarSlot), node.contextLevel);
      initialize(node.getSourceSection());
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedVariableReadNode");

      if (contextLevel > 0) {
        NonLocalVariableReadNode node = NonLocalVariableReadNodeGen.create(
            contextLevel, variable.getSlot()).initialize(getSourceSection());
        return replace(node).executeGeneric(frame);
      } else {
        // assert frame.getFrameDescriptor().findFrameSlot(variable.getSlotIdentifier()) == variable.getSlot();
        return replace(this.specializedNode()).executeGeneric(frame);
      }
    }

    protected LocalVariableReadNode specializedNode() {
      return LocalVariableReadNodeGen.create(variable).initialize(getSourceSection());
    }

    @Override
    public void replaceWithIndependentCopyForInlining(final SplitterForLexicallyEmbeddedCode inliner) {
      FrameSlot varSlot = inliner.getFrameSlot(this, variable.getSlotIdentifier());
      assert varSlot != null;
      replace(new UninitializedVariableReadNode(this, varSlot));
    }

    @Override
    public void replaceWithLexicallyEmbeddedNode(
        final InlinerForLexicallyEmbeddedMethods inliner) {
      UninitializedVariableReadNode inlined;

      if (contextLevel == 0) {
        // might need to add new frame slot in outer method
        inlined = inliner.getLocalRead(variable.getSlotIdentifier(),
            getSourceSection());
      } else {
        inlined = new UninitializedVariableReadNode(variable, contextLevel - 1).
            initialize(getSourceSection());
      }
      replace(inlined);
    }

    @Override
    public void replaceWithCopyAdaptedToEmbeddedOuterContext(
        final InlinerAdaptToEmbeddedOuterContext inliner) {
      // if the context level is 1, the variable is in the outer context,
      // which just got inlined, so, we need to adapt the slot id
      UninitializedVariableReadNode node;
      if (inliner.appliesTo(contextLevel)) {
        node = new UninitializedVariableReadNode(this,
            inliner.getOuterSlot(variable.getSlotIdentifier()));
        replace(node);
        return;
      } else if (inliner.needToAdjustLevel(contextLevel)) {
        node = new UninitializedVariableReadNode(variable, contextLevel - 1).initialize(getSourceSection());
        replace(node);
        return;
      }
    }

    @Override
    public Node asMateNode() {
      return new MateUninitializedVariableNode.MateUninitializedVariableReadNode(this);
    }
  }

  public static class UninitializedVariableWriteNode extends UninitializedVariableNode {
    @Child protected ExpressionNode exp;

    public UninitializedVariableWriteNode(final Local variable,
        final int contextLevel, final ExpressionNode exp) {
      super(variable, contextLevel);
      this.exp = exp;
      this.adoptChildren();
    }

    public UninitializedVariableWriteNode(final UninitializedVariableWriteNode node,
        final FrameSlot inlinedVarSlot) {
      this(node.variable.cloneForInlining(inlinedVarSlot),
          node.contextLevel, node.exp);
      initialize(node.getSourceSection());
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      transferToInterpreterAndInvalidate("UninitializedVariableWriteNode");

      if (accessesOuterContext()) {
        NonLocalVariableWriteNode node = NonLocalVariableWriteNodeGen.create(
            contextLevel, variable.getSlot(), exp).initialize(getSourceSection());
        return replace(node).executeGeneric(frame);
      } else {
        // not sure about removing this assertion :(((
        // assert frame.getFrameDescriptor().findFrameSlot(variable.getSlotIdentifier()) == variable.getSlot();
        return replace(this.specializedNode()).executeGeneric(frame);
      }
    }

    protected LocalVariableWriteNode specializedNode() {
      return LocalVariableWriteNodeGen.create(variable, exp).initialize(getSourceSection());
    }

    @Override
    public void replaceWithIndependentCopyForInlining(final SplitterForLexicallyEmbeddedCode inliner) {
      FrameSlot varSlot = inliner.getFrameSlot(this, variable.getSlotIdentifier());
      assert varSlot != null;
      replace(new UninitializedVariableWriteNode(this, varSlot));
    }

    @Override
    public void replaceWithCopyAdaptedToEmbeddedOuterContext(
        final InlinerAdaptToEmbeddedOuterContext inliner) {
      // if the context level is 1, the variable is in the outer context,
      // which just got inlined, so, we need to adapt the slot id
      UninitializedVariableWriteNode node;
      if (inliner.appliesTo(contextLevel)) {
        node = new UninitializedVariableWriteNode(this,
            inliner.getOuterSlot(variable.getSlotIdentifier()));
        replace(node);
        return;
      } else if (inliner.needToAdjustLevel(contextLevel)) {
        node = new UninitializedVariableWriteNode(variable, contextLevel - 1,
            exp).initialize(getSourceSection());
        replace(node);
        return;
      }
    }

    @Override
    public String toString() {
      return "UninitVarWrite(" + variable.toString() + ")";
    }

    @Override
    public void replaceWithLexicallyEmbeddedNode(
        final InlinerForLexicallyEmbeddedMethods inliner) {
      UninitializedVariableWriteNode inlined;

      if (contextLevel == 0) {
        // might need to add new frame slot in outer method
        inlined = inliner.getLocalWrite(variable.getSlotIdentifier(),
            exp, getSourceSection());
      } else {
        inlined = new UninitializedVariableWriteNode(variable, contextLevel - 1,
            exp).initialize(getSourceSection());
      }
      replace(inlined);
    }

    @Override
    public Node asMateNode() {
      return new MateUninitializedVariableNode.MateUninitializedVariableWriteNode(this);
    }
  }
}
