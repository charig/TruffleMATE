package som.interpreter.nodes.dispatch;

import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import som.vm.constants.ExecutionLevel;
import som.vmobjects.SSymbol;

public class MateUninitializedDispatchNode extends UninitializedDispatchNode {
  public MateUninitializedDispatchNode(final SourceSection source, final SSymbol selector) {
    super(source, selector);
  }

  @Override
  protected CachedDispatchNode cacheNode(final DispatchGuard guard, final DynamicObject methodCall,
      final UninitializedDispatchNode newChainEnd, final Boolean shouldSplit, final ExecutionLevel level) {
    return new MateCachedDispatchNode(guard, methodCall, newChainEnd, shouldSplit, level);
  }

  @Override
  protected UninitializedDispatchNode uninitializedNode(final SourceSection section, final SSymbol selector) {
    return new MateUninitializedDispatchNode(this.sourceSection, selector);
  }

  @Override
  protected GenericDispatchNode genericDispatchNode(final SourceSection section, final SSymbol selector) {
    return new MateGenericDispatchNode(this.sourceSection, selector);
  }

  @Override
  public NodeCost getCost() {
    return NodeCost.NONE;
  }
}
