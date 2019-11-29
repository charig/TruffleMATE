package som.interpreter.nodes.dispatch;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.SArguments;
import som.interpreter.Types;
import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SClass;
import som.vmobjects.SSymbol;


public class UninitializedDispatchNode extends AbstractDispatchNode {
  @Child protected GenericDispatchNode generic;
  protected int activations = 0;
  static final int WARMUP_LIMIT = 5;
  protected final SSymbol selector;

  public UninitializedDispatchNode(final SourceSection source, final SSymbol selector) {
    super(source);
    this.selector = selector;
    generic = this.genericDispatchNode(source, selector);
    this.adoptChildren();
  }

  private AbstractDispatchNode specialize(final VirtualFrame frame, final Object[] arguments) {
    // Determine position in dispatch node chain, i.e., size of inline cache
    Node i = this;
    int chainDepth = 0;
    while (i.getParent() instanceof AbstractDispatchNode) {
      i = i.getParent();
      chainDepth++;
    }
    AbstractDispatchNode first = (AbstractDispatchNode) i;
    Object rcvr = arguments[0];
    assert rcvr != null;

    if (chainDepth < INLINE_CACHE_SIZE) {
      DynamicObject rcvrClass = Types.getClassOf(rcvr);
      DynamicObject method = SClass.lookupInvokable(rcvrClass, selector);
      UninitializedDispatchNode newChainEnd = this.uninitializedNode(this.sourceSection, selector);
      DispatchGuard guard = DispatchGuard.create(rcvr);
      AbstractCachedDispatchNode node;
      if (method != null) {
        boolean shouldSplit = selector.getString().equals("new") ? true : false;
        node = this.cacheNode(guard, method, newChainEnd, shouldSplit, SArguments.getExecutionLevel(frame));
      } else {
        node = new CachedDnuNode(rcvrClass, guard, selector, newChainEnd, SArguments.getExecutionLevel(frame));
      }
      replace(node);
      return node;
    }

    // the chain is longer than the maximum defined by INLINE_CACHE_SIZE and
    // thus, this callsite is considered to be megaprophic, and we generalize
    // it.
    GenericDispatchNode genericReplacement = this.genericDispatchNode(this.sourceSection, selector);
    GenericMessageSendNode sendNode = (GenericMessageSendNode) first.getParent();
    sendNode.replaceDispatchListHead(genericReplacement);
    return genericReplacement;
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame,
      final DynamicObject environment, final ExecutionLevel exLevel, final Object[] arguments) {
    if (activations < WARMUP_LIMIT) {
      activations++;
      return generic.executeDispatch(frame, environment, exLevel, arguments);
    } else {
      transferToInterpreterAndInvalidate("Initialize a dispatch node.");
      return specialize(frame, arguments).
        executeDispatch(frame, environment, exLevel, arguments);
    }
  }

  @Override
  public int lengthOfDispatchChain() {
    return 0;
  }

  protected CachedDispatchNode cacheNode(final DispatchGuard guard, final DynamicObject methodToCall,
      final UninitializedDispatchNode newChainEnd, final Boolean shouldSplit, final ExecutionLevel level) {
    return new CachedDispatchNode(guard, methodToCall, newChainEnd, shouldSplit, level);
  }

  protected UninitializedDispatchNode uninitializedNode(final SourceSection section, final SSymbol selector) {
    return new UninitializedDispatchNode(this.sourceSection, selector);
  }

  protected GenericDispatchNode genericDispatchNode(final SourceSection section, final SSymbol selector) {
    return new GenericDispatchNode(this.sourceSection, selector);
  }
}
