package som.interpreter.nodes.dispatch;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.SArguments;
import som.interpreter.Types;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SArray;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SSymbol;

public class GenericDispatchNode extends AbstractDispatchNode {
  @Child protected IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();
  protected final SSymbol selector;
  protected final BranchProfile dnu = BranchProfile.create();

  public GenericDispatchNode(final SourceSection source, final SSymbol selector) {
    super(source);
    this.selector = selector;
  }

  @Override
  public Object executeDispatch(final VirtualFrame frame,
      final DynamicObject environment, final ExecutionLevel exLevel, final Object[] arguments) {
    Object rcvr = arguments[0];
    DynamicObject rcvrClass = Types.getClassOf(rcvr);
    DynamicObject method = SClass.lookupInvokable(rcvrClass, selector);

    CallTarget target;
    Object[] args;

    if (method != null) {
      target = SInvokable.getCallTarget(method, exLevel);
      args = this.getArguments(frame, environment, exLevel, arguments);
    } else {
      // Won't use DNU caching here, because it is already a megamorphic node
      dnu.enter();
      SArray argumentsArray = SArguments.getArgumentsWithoutReceiver(arguments);
      args = new Object[] {environment, exLevel, arguments[SArguments.RCVR_ARGUMENTS_OFFSET], selector, argumentsArray};
      target = SMethod.getCallTarget(CachedDnuNode.getDnuMethod(rcvrClass), exLevel);
    }
    return call.call(target, args);
  }

  protected Object[] getArguments(final VirtualFrame frame, final DynamicObject environment, final ExecutionLevel exLevel, final Object[] arguments) {
    return SArguments.createSArguments(environment, exLevel, arguments);
  }

  @Override
  public int lengthOfDispatchChain() {
    return 1000;
  }
}
