package som.interpreter.nodes.literals;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

import som.compiler.MethodGenerationContext;
import som.compiler.Variable.Local;
import som.interpreter.InlinerAdaptToEmbeddedOuterContext;
import som.interpreter.InlinerForLexicallyEmbeddedMethods;
import som.interpreter.Invokable;
import som.interpreter.Method;
import som.interpreter.SplitterForLexicallyEmbeddedCode;
import som.interpreter.nodes.ExpressionNode;
import som.vm.Universe;
import som.vmobjects.InvokableEnvInObjectLayoutImpl;
import som.vmobjects.InvokableLayoutImpl;
import som.vmobjects.MethodEnvInObjectLayoutImpl;
import som.vmobjects.MethodLayoutImpl;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;

public class BlockNode extends LiteralNode {

  protected final DynamicObject blockMethod;
  @CompilationFinal protected DynamicObject blockClass;

  public BlockNode(final DynamicObject blockMethod) {
    this.blockMethod = blockMethod;
  }

  protected void setBlockClass() {
    switch (SInvokable.getNumberOfArguments(blockMethod)) {
      case 1: blockClass = Universe.getCurrent().getBlockClass(1); break;
      case 2: blockClass = Universe.getCurrent().getBlockClass(2); break;
      case 3: blockClass = Universe.getCurrent().getBlockClass(3); break;
      case 4: blockClass = Universe.getCurrent().getBlockClass(4); break;
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
    Invokable clonedInvokable = Universe.getCurrent().environmentInObect() ?
        InvokableEnvInObjectLayoutImpl.INSTANCE.getInvokable(blockMethod).cloneWithNewLexicalContext(inliner.getCurrentScope()) :
        InvokableLayoutImpl.INSTANCE.getInvokable(blockMethod).cloneWithNewLexicalContext(inliner.getCurrentScope());
    replaceAdapted(clonedInvokable);
  }

  @Override
  public void replaceWithLexicallyEmbeddedNode(
      final InlinerForLexicallyEmbeddedMethods inliner) {
    Invokable adapted = Universe.getCurrent().environmentInObect() ?
        ((Method) InvokableEnvInObjectLayoutImpl.INSTANCE.getInvokable(blockMethod)).cloneAndAdaptToEmbeddedOuterContext(inliner) :
        ((Method) InvokableLayoutImpl.INSTANCE.getInvokable(blockMethod)).cloneAndAdaptToEmbeddedOuterContext(inliner);
    replaceAdapted(adapted);
  }

  @Override
  public void replaceWithCopyAdaptedToEmbeddedOuterContext(
      final InlinerAdaptToEmbeddedOuterContext inliner) {

    Invokable adapted = Universe.getCurrent().environmentInObect() ?
        ((Method) InvokableEnvInObjectLayoutImpl.INSTANCE.getInvokable(blockMethod)).cloneAndAdaptToSomeOuterContextBeingEmbedded(inliner) :
        ((Method) InvokableLayoutImpl.INSTANCE.getInvokable(blockMethod)).cloneAndAdaptToSomeOuterContextBeingEmbedded(inliner);
    replaceAdapted(adapted);
  }

  private void replaceAdapted(final Invokable adaptedForContext) {
    DynamicObject adapted = Universe.getCurrent().environmentInObect() ?
        Universe.newMethod(
          MethodEnvInObjectLayoutImpl.INSTANCE.getSignature(blockMethod), adaptedForContext, false,
          MethodEnvInObjectLayoutImpl.INSTANCE.getEmbeddedBlocks(blockMethod)) :
        Universe.newMethod(
          MethodLayoutImpl.INSTANCE.getSignature(blockMethod), adaptedForContext, false,
          MethodLayoutImpl.INSTANCE.getEmbeddedBlocks(blockMethod));
    replace(createNode(adapted));
  }

  protected BlockNode createNode(final DynamicObject adapted) {
    return new BlockNode(adapted).initialize(getSourceSection());
  }

  @Override
  public ExpressionNode inline(final MethodGenerationContext mgenc,
      final Local... blockArguments) {
    // self doesn't need to be passed
    assert SInvokable.getNumberOfArguments(blockMethod) - 1 == blockArguments.length;
    return Universe.getCurrent().environmentInObect() ?
        InvokableEnvInObjectLayoutImpl.INSTANCE.getInvokable(blockMethod).inline(mgenc, blockArguments) :
        InvokableLayoutImpl.INSTANCE.getInvokable(blockMethod).inline(mgenc, blockArguments);
  }

  @Override
  public Node asMateNode() {
    Universe.getCurrent().mateifyMethod(blockMethod);
    return super.asMateNode();
  }

  public static final class BlockNodeWithContext extends BlockNode {

    public BlockNodeWithContext(final DynamicObject blockMethod) {
      super(blockMethod);
    }

    public BlockNodeWithContext(final BlockNodeWithContext node) {
      this(node.blockMethod);
      initialize(node.getSourceSection());
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
      return new BlockNodeWithContext(adapted).initialize(getSourceSection());
    }
  }
}
