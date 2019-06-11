package som.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;

import som.interpreter.SArguments;
import som.interpreter.nodes.dispatch.AbstractDispatchNode.AbstractCachedDispatchNode;
import som.vm.constants.ExecutionLevel;


public class CachedDispatchNode extends AbstractCachedDispatchNode {
  final ConditionProfile morphicness = ConditionProfile.createBinaryProfile();
  protected final DispatchGuard guard;

  public CachedDispatchNode(final DispatchGuard guard, final DynamicObject methodToCall,
      final AbstractDispatchNode nextInCache, final boolean shouldSplit, final ExecutionLevel level) {
    super(methodToCall, nextInCache, level);
    this.guard = guard;
    /*if (VmSettings.DYNAMIC_METRICS) {
      this.cachedMethod = insert(new InstrumentableDirectCallNode(cachedMethod,
          nextInCache.getSourceSection()));
    }*/
    if (shouldSplit) {
      cachedMethod.cloneCallTarget();
    }
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame,
      final DynamicObject environment, final ExecutionLevel exLevel, final Object[] arguments) {
    Object rcvr = arguments[0];
    try {
      if (morphicness.profile(guard.entryMatches(rcvr))) {
        return cachedMethod.call(SArguments.createSArguments(environment, exLevel, arguments));
      } else {
        return nextInCache.executeDispatch(frame, environment, exLevel, arguments);
      }
    } catch (InvalidAssumptionException e) {
      CompilerDirectives.transferToInterpreter();
      return replace(nextInCache).
          executeDispatch(frame, environment, exLevel, arguments);
    }
  }
}
