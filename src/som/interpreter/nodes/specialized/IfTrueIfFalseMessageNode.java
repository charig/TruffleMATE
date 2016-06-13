package som.interpreter.nodes.specialized;

import som.interpreter.SArguments;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;

/**
 * This node implements the correct message semantics and uses sends to the
 * blocks' methods instead of inlining the code directly.
 * @author smarr
 */
public abstract class IfTrueIfFalseMessageNode extends TernaryExpressionNode {
  private final ConditionProfile condProf = ConditionProfile.createCountingProfile();

  private final DynamicObject trueMethod;
  private final DynamicObject falseMethod;

  @Child protected DirectCallNode trueValueSend;
  @Child protected DirectCallNode falseValueSend;

  @Child private IndirectCallNode call;

  public IfTrueIfFalseMessageNode(final Object rcvr, final Object arg1,
      final Object arg2, ExecutionLevel level) {
    if (arg1 instanceof SBlock) {
      SBlock trueBlock = (SBlock) arg1;
      trueMethod = trueBlock.getMethod();
      trueValueSend = Truffle.getRuntime().createDirectCallNode(
          SInvokable.getCallTarget(trueMethod, level));
    } else {
      trueMethod = null;
    }

    if (arg2 instanceof SBlock) {
      SBlock falseBlock = (SBlock) arg2;
      falseMethod = falseBlock.getMethod();
      falseValueSend = Truffle.getRuntime().createDirectCallNode(
          SInvokable.getCallTarget(falseMethod, level));
    } else {
      falseMethod = null;
    }

    call = Truffle.getRuntime().createIndirectCallNode();
  }

  public IfTrueIfFalseMessageNode(final IfTrueIfFalseMessageNode node, ExecutionLevel level) {
    super();
    trueMethod = node.trueMethod;
    if (node.trueMethod != null) {
      trueValueSend = Truffle.getRuntime().createDirectCallNode(
          SInvokable.getCallTarget(trueMethod, level));
    }

    falseMethod = node.falseMethod;
    if (node.falseMethod != null) {
      falseValueSend = Truffle.getRuntime().createDirectCallNode(
          SInvokable.getCallTarget(falseMethod, level));
    }
    call = Truffle.getRuntime().createIndirectCallNode();
  }

  protected final boolean hasSameArguments(final Object firstArg, final Object secondArg) {
    return (trueMethod  == null || ((SBlock) firstArg).getMethod()  == trueMethod)
        && (falseMethod == null || ((SBlock) secondArg).getMethod() == falseMethod);
  }

  @Specialization(guards = "hasSameArguments(trueBlock, falseBlock)")
  public final Object doIfTrueIfFalseWithInliningTwoBlocks(final VirtualFrame frame,
      final boolean receiver, final SBlock trueBlock, final SBlock falseBlock) {
    if (condProf.profile(receiver)) {
      return trueValueSend.call(frame, SArguments.createSArguments(SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), new Object[] {trueBlock}));
    } else {
      return falseValueSend.call(frame, SArguments.createSArguments(SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame),new Object[] {falseBlock}));
    }
  }

  @Specialization(contains = {"doIfTrueIfFalseWithInliningTwoBlocks"})
  public final Object doIfTrueIfFalse(final VirtualFrame frame,
      final boolean receiver, final SBlock trueBlock, final SBlock falseBlock) {
    CompilerAsserts.neverPartOfCompilation("IfTrueIfFalseMessageNode.10");
    if (condProf.profile(receiver)) {
      return SInvokable.invoke(trueBlock.getMethod(), frame, call, trueBlock);
    } else {
      return SInvokable.invoke(falseBlock.getMethod(), frame, call, falseBlock);
    }
  }

  @Specialization(guards = "hasSameArguments(trueValue, falseBlock)")
  public final Object doIfTrueIfFalseWithInliningTrueValue(final VirtualFrame frame,
      final boolean receiver, final Object trueValue, final SBlock falseBlock) {
    if (condProf.profile(receiver)) {
      return trueValue;
    } else {
      return falseValueSend.call(frame, SArguments.createSArguments(SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame),new Object[] {falseBlock}));
    }
  }

  @Specialization(guards = "hasSameArguments(trueBlock, falseValue)")
  public final Object doIfTrueIfFalseWithInliningFalseValue(final VirtualFrame frame,
      final boolean receiver, final SBlock trueBlock, final Object falseValue) {
    if (condProf.profile(receiver)) {
      return trueValueSend.call(frame, SArguments.createSArguments(SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame),new Object[] {trueBlock}));
    } else {
      return falseValue;
    }
  }

  @Specialization(contains = {"doIfTrueIfFalseWithInliningTrueValue"})
  public final Object doIfTrueIfFalseTrueValue(final VirtualFrame frame,
      final boolean receiver, final Object trueValue, final SBlock falseBlock) {
    if (condProf.profile(receiver)) {
      return trueValue;
    } else {
      CompilerAsserts.neverPartOfCompilation("IfTrueIfFalseMessageNode.20");
      return SInvokable.invoke(falseBlock.getMethod(), frame, call, falseBlock);
    }
  }

  @Specialization(contains = {"doIfTrueIfFalseWithInliningFalseValue"})
  public final Object doIfTrueIfFalseFalseValue(final VirtualFrame frame,
      final boolean receiver, final SBlock trueBlock, final Object falseValue) {
    if (condProf.profile(receiver)) {
      CompilerAsserts.neverPartOfCompilation("IfTrueIfFalseMessageNode.30");
      return SInvokable.invoke(trueBlock.getMethod(), frame, call, trueBlock);
    } else {
      return falseValue;
    }
  }

  @Specialization
  public final Object doIfTrueIfFalseTwoValues(final VirtualFrame frame,
      final boolean receiver, final Object trueValue, final Object falseValue) {
    if (condProf.profile(receiver)) {
      return trueValue;
    } else {
      return falseValue;
    }
  }
}
