package som.interpreter.nodes.dispatch;

import som.interpreter.SArguments;
import som.interpreter.nodes.dispatch.AbstractMethodDispatchNode.AbstractMethodCachedDispatchNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;


public final class CachedMethodDispatchNode extends AbstractMethodCachedDispatchNode {

  private final DynamicObject cachedSomMethod;

  public CachedMethodDispatchNode(final DynamicObject method,
      final AbstractMethodDispatchNode nextInCache, ExecutionLevel level) {
    super(SInvokable.getCallTarget(method, level), nextInCache);
    this.cachedSomMethod = method;
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame, final DynamicObject environment, final ExecutionLevel exLevel, final DynamicObject method, final Object[] arguments) {
    if (method == cachedSomMethod) {
      return cachedMethod.call(frame, SArguments.createSArguments(environment, exLevel, arguments));
    } else {
      return nextInCache.executeDispatch(frame, environment, exLevel, method, arguments);
    }
  }
}
