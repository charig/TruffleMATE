package som.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;

import som.vm.NotYetImplementedException;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SBlock;


public final class WhileWithDynamicBlocksNode extends AbstractWhileNode {
  private final DynamicObject conditionMethod;
  private final DynamicObject bodyMethod;

  public static WhileWithDynamicBlocksNode create(final SBlock rcvr,
      final SBlock arg, final boolean predicateBool, final ExecutionLevel level) {
    return new WhileWithDynamicBlocksNode(rcvr, arg, predicateBool, level);
  }

  public WhileWithDynamicBlocksNode(final SBlock rcvr, final SBlock arg,
      final boolean predicateBool, final ExecutionLevel level) {
    super(rcvr, arg, predicateBool, level);
    conditionMethod = rcvr.getMethod();
    bodyMethod = arg.getMethod();
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    CompilerAsserts.neverPartOfCompilation("WhileWithDynamicBlocksNode.generic");
    throw new NotYetImplementedException();
  }

  @Override
  public Object executeGenericWithReceiver(final VirtualFrame frame, final Object receiver) {
    CompilerAsserts.neverPartOfCompilation("WhileWithDynamicBlocksNode.generic");
    throw new NotYetImplementedException();
  }

  @Override
  protected DynamicObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition,
      final SBlock loopBody) {
    assert loopCondition.getMethod() == conditionMethod;
    assert loopBody.getMethod()      == bodyMethod;
    return doWhileUnconditionally(frame, loopCondition, loopBody);
  }
}
