package som.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import som.instrumentation.DispatchNodeWrapper;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SInvokable;


public abstract class AbstractDispatchNode extends Node implements DispatchChain, InstrumentableNode {
  public static final int INLINE_CACHE_SIZE = 6;
  protected final SourceSection sourceSection;

  protected AbstractDispatchNode(final SourceSection source) {
    super();
    this.sourceSection = source;
  }

  @Override
  public WrapperNode createWrapper(final ProbeNode probeNode) {
    return new DispatchNodeWrapper(this, probeNode);
  }

  @Override
  public final boolean isInstrumentable() {
    return true;
  }

  /**
   * For wrapped nodes only.
   */
  protected AbstractDispatchNode(final AbstractDispatchNode wrappedNode) {
    super();
    this.sourceSection = null;
  }

  @Override
  public SourceSection getSourceSection() {
    return sourceSection;
  }

  public abstract Object executeDispatch(
      VirtualFrame frame, DynamicObject environment, ExecutionLevel exLevel, Object[] arguments);

  public abstract static class AbstractCachedDispatchNode
      extends AbstractDispatchNode {

    @Child protected DirectCallNode       cachedMethod;
    @Child protected AbstractDispatchNode nextInCache;

    public AbstractCachedDispatchNode(final DynamicObject methodToCall,
        final AbstractDispatchNode nextInCache, final ExecutionLevel level) {
      super(nextInCache.sourceSection);
      CallTarget callTarget;
      if (methodToCall != null) {
        callTarget = SInvokable.getCallTarget(methodToCall, level);
      } else {
        callTarget = null;
      }
      this.cachedMethod = Truffle.getRuntime().createDirectCallNode(callTarget);;
      this.nextInCache  = nextInCache;
      this.adoptChildren();
    }

    @Override
    public final int lengthOfDispatchChain() {
      return 1 + nextInCache.lengthOfDispatchChain();
    }

    public DirectCallNode getCallNode() {
      return cachedMethod;
    }
  }

  @Override
  public NodeCost getCost() {
    return NodeCost.NONE;
  }

}
