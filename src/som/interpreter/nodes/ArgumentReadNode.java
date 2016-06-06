package som.interpreter.nodes;

import som.interpreter.FrameOnStackMarker;
import som.interpreter.InlinerAdaptToEmbeddedOuterContext;
import som.interpreter.InlinerForLexicallyEmbeddedMethods;
import som.interpreter.MateVisitors;
import som.interpreter.SArguments;
import som.vm.Universe;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

public abstract class ArgumentReadNode {

  public static class LocalArgumentReadNode extends ExpressionNode {
    protected final int argumentIndex;

    public LocalArgumentReadNode(final int argumentIndex, final SourceSection source) {
      super(source);
      assert argumentIndex >= 0;
      this.argumentIndex = argumentIndex;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return SArguments.arg(frame, argumentIndex);
    }
    
    @Override
    public void replaceWithLexicallyEmbeddedNode(
        final InlinerForLexicallyEmbeddedMethods inliner) {
      replace(inliner.getReplacementForLocalArgument(argumentIndex,
          getSourceSection()));
    }
    
    public ReflectiveOp reflectiveOperation(){
      return ReflectiveOp.ExecutorLocalArg;
    }
    
    @Override
    public Node asMateNode() {
      return new MateArgumentReadNode.MateLocalArgumentReadNode(this);
    }
  }

  public static class NonLocalArgumentReadNode extends ContextualNode {
    protected final int argumentIndex;

    public NonLocalArgumentReadNode(final int argumentIndex,
        final int contextLevel, final SourceSection source) {
      super(contextLevel, source);
      assert contextLevel > 0;
      this.argumentIndex = argumentIndex;
    }

    @Override
    public Object executeGeneric(final VirtualFrame frame) {
      return SArguments.arg(determineContext(frame), argumentIndex);
    }

    @Override
    public void replaceWithLexicallyEmbeddedNode(
        final InlinerForLexicallyEmbeddedMethods inliner) {
      ExpressionNode inlined;
      if (contextLevel == 1) {
        inlined = createLocalNode();
      } else {
        inlined = createNonLocalNode();
      }
      replace(inlined);
    }

    protected NonLocalArgumentReadNode createNonLocalNode() {
      return new NonLocalArgumentReadNode(argumentIndex, contextLevel - 1,
          getSourceSection());
    }

    protected LocalArgumentReadNode createLocalNode() {
      return new LocalArgumentReadNode(argumentIndex, getSourceSection());
    }

    @Override
    public void replaceWithCopyAdaptedToEmbeddedOuterContext(
        final InlinerAdaptToEmbeddedOuterContext inliner) {
      // this should be the access to a block argument
      if (inliner.appliesTo(contextLevel)) {
        assert !(this instanceof NonLocalSuperReadNode);
        ExpressionNode node = inliner.getReplacementForBlockArgument(argumentIndex, getSourceSection());
        replace(node);
        return;
      } else if (inliner.needToAdjustLevel(contextLevel)) {
        // in the other cases, we just need to adjust the context level
        NonLocalArgumentReadNode node = createNonLocalNode();
        replace(node);
        return;
      }
    }
    
    public ReflectiveOp reflectiveOperation(){
      return ReflectiveOp.ExecutorNonLocalArg;
    }

    @Override
    public Node asMateNode() {
      return new MateArgumentReadNode.MateNonLocalArgumentReadNode(this);
    }
  }

  public static class LocalSuperReadNode extends LocalArgumentReadNode
      implements ISuperReadNode {

    private final SSymbol holderClass;
    private final boolean classSide;

    public LocalSuperReadNode(final SSymbol holderClass,
        final boolean classSide, final SourceSection source) {
      super(SArguments.RCVR_ARGUMENTS_OFFSET, source);
      this.holderClass = holderClass;
      this.classSide   = classSide;
    }

    @Override
    public SSymbol getHolderClass() {
      return holderClass;
    }

    @Override
    public boolean isClassSide() {
      return classSide;
    }
    
    public ReflectiveOp reflectiveOperation(){
      return ReflectiveOp.ExecutorLocalSuperArg;
    }
    
    @Override
    public Node asMateNode() {
      return new MateArgumentReadNode.MateLocalSuperReadNode(this);
    }
  }

  public static class NonLocalSuperReadNode extends
      NonLocalArgumentReadNode implements ISuperReadNode {

    private final SSymbol holderClass;
    private final boolean classSide;

    public NonLocalSuperReadNode(final int contextLevel,
        final SSymbol holderClass, final boolean classSide,
        final SourceSection source) {
      super(0, contextLevel, source);
      this.holderClass = holderClass;
      this.classSide   = classSide;
    }

    @Override
    public SSymbol getHolderClass() {
      return holderClass;
    }

    @Override
    protected NonLocalArgumentReadNode createNonLocalNode() {
      return new NonLocalSuperReadNode(contextLevel - 1, holderClass,
          classSide, getSourceSection());
    }

    @Override
    protected LocalArgumentReadNode createLocalNode() {
      return new LocalSuperReadNode(holderClass, classSide, getSourceSection());
    }

    @Override
    public boolean isClassSide() {
      return classSide;
    }
    
    public ReflectiveOp reflectiveOperation(){
      return ReflectiveOp.ExecutorNonLocalSuperArg;
    }
    
    @Override
    public Node asMateNode() {
      return new MateArgumentReadNode.MateNonLocalSuperReadNode(this);
    }
  }
  
  public static class ThisContextNode extends ExpressionNode {
    public ThisContextNode(final SourceSection source) {
      super(source);
    }

    @Override
    public FrameInstance executeGeneric(final VirtualFrame frame) {
      TruffleRuntime runtime = ((Universe)((ExpressionNode)this).getRootNode().getExecutionContext()).getTruffleRuntime(); 
      FrameInstance currentFrame;
      if (SArguments.getExecutionLevel(frame) == ExecutionLevel.Meta){
        currentFrame = runtime.iterateFrames(new MateVisitors.FindFirstBaseLevelFrame());
      } else {
        currentFrame = runtime.getCurrentFrame();
      }
      final Frame materialized = currentFrame.getFrame(FrameAccess.MATERIALIZE, true);
      if (materialized.getFrameDescriptor().findFrameSlot(Universe.frameOnStackSlotName()) == null){
        FrameSlot frameOnStackMarker = materialized.getFrameDescriptor().addFrameSlot(Universe.frameOnStackSlotName(), FrameSlotKind.Object);
        frame.setObject(frameOnStackMarker, new FrameOnStackMarker());
      }
      return currentFrame;
    }
  }
}
