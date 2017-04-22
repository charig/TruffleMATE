package som.interpreter.nodes.literals;

import som.compiler.MethodGenerationContext;
import som.compiler.Variable.Local;
import som.interpreter.InlinerAdaptToEmbeddedOuterContext;
import som.interpreter.InlinerForLexicallyEmbeddedMethods;
import som.interpreter.Invokable;
import som.interpreter.Method;
import som.interpreter.SplitterForLexicallyEmbeddedCode;
import som.interpreter.nodes.ExpressionNode;
import som.vm.Universe;
import som.vmobjects.InvokableLayoutImpl;
import som.vmobjects.MethodLayoutImpl;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

public class BlockNode extends LiteralNode {

  protected final DynamicObject blockMethod;

  public BlockNode(final DynamicObject blockMethod,
      final SourceSection source) {
    super(source);
    if ((Universe.getCurrent().vmReflectionEnabled())) {
      Universe.getCurrent().mateifyMethod(blockMethod);
    }
    this.blockMethod = blockMethod;
  }

  @Override
  public SBlock executeSBlock(final VirtualFrame frame) {
    return Universe.newBlock(blockMethod, null);
  }

  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    return executeSBlock(frame);
  }

  @Override
  public void replaceWithIndependentCopyForInlining(final SplitterForLexicallyEmbeddedCode inliner) {
    Invokable clonedInvokable = InvokableLayoutImpl.INSTANCE.getInvokable(blockMethod).
        cloneWithNewLexicalContext(inliner.getCurrentScope(), true);
    replaceAdapted(clonedInvokable);
  }

  @Override
  public void replaceWithLexicallyEmbeddedNode(
      final InlinerForLexicallyEmbeddedMethods inliner) {
    Invokable adapted = ((Method) InvokableLayoutImpl.INSTANCE.getInvokable(blockMethod)).
        cloneAndAdaptToEmbeddedOuterContext(inliner);
    replaceAdapted(adapted);
  }

  @Override
  public void replaceWithCopyAdaptedToEmbeddedOuterContext(
      final InlinerAdaptToEmbeddedOuterContext inliner) {
    Invokable adapted = ((Method) InvokableLayoutImpl.INSTANCE.getInvokable(blockMethod)).
        cloneAndAdaptToSomeOuterContextBeingEmbedded(inliner);
    replaceAdapted(adapted);
  }

  private void replaceAdapted(final Invokable adaptedForContext) {
    DynamicObject adapted = Universe.newMethod(
        MethodLayoutImpl.INSTANCE.getSignature(blockMethod), adaptedForContext, false,
        MethodLayoutImpl.INSTANCE.getEmbeddedBlocks(blockMethod));
    replace(createNode(adapted));
  }

  protected BlockNode createNode(final DynamicObject adapted) {
    return new BlockNode(adapted, getSourceSection());
  }

  @Override
  public ExpressionNode inline(final MethodGenerationContext mgenc,
      final Local... blockArguments) {
    // self doesn't need to be passed
    assert SInvokable.getNumberOfArguments(blockMethod) - 1 == blockArguments.length;
    return InvokableLayoutImpl.INSTANCE.getInvokable(blockMethod).inline(mgenc, blockArguments);
  }

  public static final class BlockNodeWithContext extends BlockNode {

    public BlockNodeWithContext(final DynamicObject blockMethod,
        final SourceSection source) {
      super(blockMethod, source);
    }

    public BlockNodeWithContext(final BlockNodeWithContext node) {
      this(node.blockMethod, node.getSourceSection());
    }

    @Override
    public SBlock executeSBlock(final VirtualFrame frame) {
      return Universe.newBlock(blockMethod, frame.materialize());
    }

    @Override
    protected BlockNode createNode(final DynamicObject adapted) {
      return new BlockNodeWithContext(adapted, getSourceSection());
    }
  }
}
