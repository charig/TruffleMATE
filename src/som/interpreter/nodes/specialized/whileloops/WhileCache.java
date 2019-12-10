package som.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;

import som.interpreter.SArguments;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.Globals;
import som.vm.constants.Nil;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import tools.dym.Tags.LoopNode;


@ReportPolymorphism
public abstract class WhileCache extends BinaryExpressionNode {

  public static final int INLINE_CACHE_SIZE = 6;

  protected final boolean predicateBool;

  public WhileCache(final boolean predicateBool) {
    this.predicateBool = predicateBool;
  }

  @Specialization(limit = "INLINE_CACHE_SIZE",
      guards = {"loopCondition.getMethod() == cachedLoopCondition",
                "loopBody.getMethod() == cachedLoopBody"})
  public final DynamicObject doCached(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody,
      @Cached("loopCondition.getMethod()") final DynamicObject cachedLoopCondition,
      @Cached("loopBody.getMethod()") final      DynamicObject cachedLoopBody,
      @Cached("create(loopCondition, loopBody, predicateBool, getExecutionLevel(frame))") final
         WhileWithDynamicBlocksNode whileNode) {
    return whileNode.doWhileUnconditionally(frame, loopCondition, loopBody);
  }

  protected static ExecutionLevel getExecutionLevel(final VirtualFrame frame) {
    return SArguments.getExecutionLevel(frame);
  }

  private boolean obj2bool(final Object o) {
    if (o instanceof Boolean) {
      return (boolean) o;
    } else if (o == Globals.trueObject) {
      CompilerAsserts.neverPartOfCompilation("obj2Bool1");
      return true;
    } else {
      CompilerAsserts.neverPartOfCompilation("obj2Bool2");
      assert o == Globals.falseObject;
      return false;
    }
  }

  @Specialization(replaces = "doCached")
  public final DynamicObject doUncached(final VirtualFrame frame, final SBlock loopCondition,
      final SBlock loopBody) {
    CompilerAsserts.neverPartOfCompilation("WhileCache.GenericDispatch"); // no caching, direct invokes, no loop count reporting...

    Object conditionResult = SInvokable.invoke(loopCondition.getMethod(), frame, loopCondition);

    // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
    boolean loopConditionResult = obj2bool(conditionResult);

    // TODO: this is a simplification, we don't cover the case receiver isn't a boolean
    while (loopConditionResult == predicateBool) {
      SInvokable.invoke(loopBody.getMethod(), frame, loopBody);
      conditionResult = SInvokable.invoke(loopCondition.getMethod(), frame, loopCondition);
      loopConditionResult = obj2bool(conditionResult);
    }
    return Nil.nilObject;
  }

  @Override
  protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
    if (tag == LoopNode.class) {
      return true;
    } else {
      return super.hasTagIgnoringEagerness(tag);
    }
  }
}
