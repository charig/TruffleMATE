package som.interpreter.nodes.literals;

import som.compiler.MethodGenerationContext;
import som.compiler.Variable.Local;
import som.interpreter.InlinerAdaptToEmbeddedOuterContext;
import som.interpreter.InlinerForLexicallyEmbeddedMethods;
import som.interpreter.Invokable;
import som.interpreter.Method;
import som.interpreter.SplitterForLexicallyEmbeddedCode;
import som.interpreter.nodes.ExpressionNode;
import som.vm.MateUniverse;
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
  @CompilationFinal protected DynamicObject blockClass;

  public BlockNode(final DynamicObject blockMethod,
      final SourceSection source) {
    super(source);
    if ((Universe.current() instanceof MateUniverse)){
      MateUniverse.current().mateifyMethod(blockMethod);
    }
    this.blockMethod = blockMethod;
  }

  protected void setBlockClass() {
    switch (SInvokable.getNumberOfArguments(blockMethod)) {
      case 1: blockClass = Universe.current().getBlockClass(1); break;
      case 2: blockClass = Universe.current().getBlockClass(2); break;
      case 3: blockClass = Universe.current().getBlockClass(3); break;
      default:
        throw new RuntimeException("We do currently not have support for more than 3 arguments to a block.");
    }
  }

  @Override
  public SBlock executeSBlock(final VirtualFrame frame) {
    if (blockClass == null) {
      CompilerDirectives.transferToInterpreter();
      setBlockClass();
    }
    return Universe.newBlock(blockMethod, blockClass, null);
  }

  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    return executeSBlock(frame);
  }

  @Override
  public void replaceWithIndependentCopyForInlining(final SplitterForLexicallyEmbeddedCode inliner) {
    Invokable clonedInvokable = InvokableLayoutImpl.INSTANCE.getInvokable(blockMethod).
        cloneWithNewLexicalContext(inliner.getCurrentScope());
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
      if (blockClass == null) {
        CompilerDirectives.transferToInterpreter();
        setBlockClass();
      }
      return Universe.newBlock(blockMethod, blockClass, frame.materialize());
    }

    @Override
    protected BlockNode createNode(final DynamicObject adapted) {
      return new BlockNodeWithContext(adapted, getSourceSection());
    }
  }
}