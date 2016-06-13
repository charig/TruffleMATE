package som.primitives.reflection;

import som.interpreter.SArguments;
import som.interpreter.Types;
import som.interpreter.nodes.MessageSendNode;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.interpreter.nodes.PreevaluatedExpression;
import som.primitives.arrays.ToArgumentsArrayNode;
import som.primitives.arrays.ToArgumentsArrayNodeGen;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.MateClasses;
import som.vmobjects.InvokableLayoutImpl;
import som.vmobjects.SArray;
import som.vmobjects.SClass;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;


public abstract class AbstractSymbolDispatch extends Node {
  public static final int INLINE_CACHE_SIZE = 6;

  public abstract Object executeDispatch(VirtualFrame frame, Object receiver,
      SSymbol selector, Object argsArr);

  public static final AbstractMessageSendNode createForPerformNodes(final SSymbol selector) {
    return MessageSendNode.createForPerformNodes(selector);
  }

  public static final ToArgumentsArrayNode createArgArrayNode() {
    return ToArgumentsArrayNodeGen.create(null, null);
  }

  @Specialization(limit = "INLINE_CACHE_SIZE", guards = {"selector == cachedSelector", "argsArr == null"})
  public Object doCachedWithoutArgArr(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final Object argsArr,
      @Cached("selector") final SSymbol cachedSelector,
      @Cached("createForPerformNodes(selector)") final AbstractMessageSendNode cachedSend) {
    Object[] arguments = { receiver };

    PreevaluatedExpression realCachedSend = cachedSend;
    return realCachedSend.doPreEvaluated(frame, arguments);
  }

  @Specialization(limit = "INLINE_CACHE_SIZE", guards = "selector == cachedSelector")
  public Object doCached(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final SArray argsArr,
      @Cached("selector") final SSymbol cachedSelector,
      @Cached("createForPerformNodes(selector)") final AbstractMessageSendNode cachedSend,
      @Cached("createArgArrayNode()") final ToArgumentsArrayNode toArgArray) {
    Object[] arguments = toArgArray.executedEvaluated(argsArr, receiver);

    PreevaluatedExpression realCachedSend = cachedSend;
    return realCachedSend.doPreEvaluated(frame, arguments);
  }

  @Specialization(contains = "doCachedWithoutArgArr", guards = "argsArr == null")
  public Object doUncached(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final Object argsArr,
      @Cached("create()") final IndirectCallNode call) {
    DynamicObject invokable = SClass.lookupInvokable(Types.getClassOf(receiver), selector);

    /*Todo: Analyze what is the best to do here with the Mate arguments*/
    Object[] arguments = { MateClasses.STANDARD_ENVIRONMENT, SArguments.getExecutionLevel(frame), receiver };
    CallTarget target;
    if (SArguments.getExecutionLevel(frame) == ExecutionLevel.Meta){
      target = InvokableLayoutImpl.INSTANCE.getCallTargetMeta(invokable);
    } else {
      target = InvokableLayoutImpl.INSTANCE.getCallTarget(invokable);
    }
    return call.call(frame, target, arguments);
  }

  @Specialization(contains = "doCached")
  public Object doUncached(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final SArray argsArr,
      @Cached("create()") final IndirectCallNode call,
      @Cached("createArgArrayNode()") final ToArgumentsArrayNode toArgArray) {
    DynamicObject invokable = SClass.lookupInvokable(Types.getClassOf(receiver), selector);

    Object[] arguments = toArgArray.executedEvaluated(argsArr, receiver);
    CallTarget target;
    if (SArguments.getExecutionLevel(frame) == ExecutionLevel.Meta){
      target = InvokableLayoutImpl.INSTANCE.getCallTargetMeta(invokable);
    } else {
      target = InvokableLayoutImpl.INSTANCE.getCallTarget(invokable);
    }
    return call.call(frame, target, arguments);
  }
}
