package som.interpreter.nodes.dispatch;

import som.interpreter.SArguments;
import som.interpreter.nodes.ISuperReadNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

/**
 * Super sends are special, they lead to a lexically defined receiver class.
 * So, it's always the cached receiver.
 */
public abstract class SuperDispatchNode extends AbstractDispatchNode {

  protected SuperDispatchNode(SourceSection source) {
    super(source);
  }

  public static SuperDispatchNode create(final SourceSection source, final SSymbol selector,
      final ISuperReadNode superNode) {
    CompilerAsserts.neverPartOfCompilation("SuperDispatchNode.create1");
    return new UninitializedDispatchNode(source, selector, superNode.getHolderClass(),
        superNode.isClassSide());
  }

  private static final class UninitializedDispatchNode extends SuperDispatchNode implements ISuperReadNode{
    private final SSymbol selector;
    private final SSymbol holderClass;
    private final boolean classSide;

    private UninitializedDispatchNode(final SourceSection source, final SSymbol selector,
        final SSymbol holderClass, final boolean classSide) {
      super(source);
      this.selector    = selector;
      this.holderClass = holderClass;
      this.classSide   = classSide;
    }

    private CachedDispatchNode specialize(ExecutionLevel level) {
      CompilerAsserts.neverPartOfCompilation("SuperDispatchNode.create2");
      DynamicObject method = SClass.lookupInvokable(getLexicalSuperClass(), selector);

      if (method == null) {
        throw new RuntimeException("Currently #dnu with super sent is not yet implemented. ");
      }
      DirectCallNode superMethodNode = Truffle.getRuntime().createDirectCallNode(
          SInvokable.getCallTarget(method, level));
      return replace(new CachedDispatchNode(this.sourceSection, superMethodNode));
    }

    @Override
    public Object executeDispatch(
        final VirtualFrame frame, final DynamicObject environment, final ExecutionLevel exLevel, final Object[] arguments) {
      return specialize(SArguments.getExecutionLevel(frame)).
          executeDispatch(frame, environment, exLevel, arguments);
    }

    @Override
    public SSymbol getHolderClass() {
      return holderClass;
    }

    @Override
    public boolean isClassSide() {
      return classSide;
    }
  }

  private static final class CachedDispatchNode extends SuperDispatchNode {
    @Child private DirectCallNode cachedSuperMethod;

    private CachedDispatchNode(final SourceSection source, final DirectCallNode superMethod) {
      super(source);
      this.cachedSuperMethod = superMethod;
      this.adoptChildren();
    }

    @Override
    public Object executeDispatch(
        final VirtualFrame frame, final DynamicObject environment, final ExecutionLevel exLevel, final Object[] arguments) {
      return cachedSuperMethod.call(SArguments.createSArguments(environment, exLevel, arguments));
    }
  }

  @Override
  public final int lengthOfDispatchChain() {
    return 1;
  }
}
