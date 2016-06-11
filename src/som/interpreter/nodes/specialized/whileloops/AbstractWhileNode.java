package som.interpreter.nodes.specialized.whileloops;

import som.interpreter.Invokable;
import som.interpreter.SArguments;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.literals.IntegerLiteralNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.Nil;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;


public abstract class AbstractWhileNode extends BinaryExpressionNode {
  @Child protected DirectCallNode conditionValueSend;
  @Child protected DirectCallNode bodyValueSend;

  protected final boolean predicateBool;
  
  @Override
  /*Analyze what is the best to do for this case*/
  public ExpressionNode getReceiver(){
    return new IntegerLiteralNode(1,this.getSourceSection());
  }
  
  @Override
  /*Analyze what is the best to do for this case*/
  public ExpressionNode getArgument(){
    return new IntegerLiteralNode(1,this.getSourceSection());
  }

  public AbstractWhileNode(final SBlock rcvr, final SBlock arg,
      final boolean predicateBool, final SourceSection source, ExecutionLevel level) {
    super(source);

    CallTarget callTargetCondition = SInvokable.getCallTarget(rcvr.getMethod(), level);
    conditionValueSend = Truffle.getRuntime().createDirectCallNode(
        callTargetCondition);

    CallTarget callTargetBody = SInvokable.getCallTarget(arg.getMethod(), level);
    bodyValueSend = Truffle.getRuntime().createDirectCallNode(
        callTargetBody);

    this.predicateBool = predicateBool;
  }

  @Override
  public final Object executeEvaluated(final VirtualFrame frame,
      final Object rcvr, final Object arg) {
    return doWhileConditionally(frame, (SBlock) rcvr, (SBlock) arg);
  }

  protected final DynamicObject doWhileUnconditionally(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody) {
    long iterationCount = 0;

    boolean loopConditionResult = (boolean) conditionValueSend.call(
        frame, SArguments.createSArguments(SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), new Object[] {loopCondition}));

    try {
      // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
      while (loopConditionResult == predicateBool) {
        bodyValueSend.call(frame, new Object[] {loopBody});
        loopConditionResult = (boolean) conditionValueSend.call(
            frame, new Object[] {loopCondition});

        if (CompilerDirectives.inInterpreter()) { iterationCount++; }
      }
    } finally {
      if (CompilerDirectives.inInterpreter()) {
        reportLoopCount(iterationCount);
      }
    }
    return Nil.nilObject;
  }

  protected abstract DynamicObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody);

  protected final void reportLoopCount(final long count) {
    CompilerAsserts.neverPartOfCompilation("reportLoopCount");
    Node current = getParent();
    while (current != null && !(current instanceof RootNode)) {
      current = current.getParent();
    }
    if (current != null) {
      ((Invokable) current).propagateLoopCountThroughoutLexicalScope(count);
    }
  }
}
