package som.interpreter.nodes.specialized.whileloops;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vmobjects.SBlock;
import tools.dym.Tags.LoopNode;


@GenerateNodeFactory
public abstract class WhilePrimitiveNode extends BinaryExpressionNode {
  final boolean predicateBool;
  @Child protected WhileCache whileNode;

  protected WhilePrimitiveNode(final boolean eagPrim, final SourceSection source, final boolean predicateBool) {
    this.predicateBool = predicateBool;
    this.whileNode = WhileCacheNodeGen.create(predicateBool, null, null).initialize(source);
  }

  @Specialization
  protected DynamicObject doWhileConditionally(final VirtualFrame frame,
      final SBlock loopCondition, final SBlock loopBody) {
    return (DynamicObject) whileNode.executeEvaluated(frame, loopCondition, loopBody);
  }

  // @Primitive(className = "Block", primitive = "whileTrue:", receiverType = {SBlock.class})
  public abstract static class WhileTruePrimitiveNode extends WhilePrimitiveNode {
    public WhileTruePrimitiveNode(final boolean eagPrim, final SourceSection source) { super(eagPrim, source, true); }
  }

  // @Primitive(className = "Block", primitive = "whileFalse:", receiverType = {SBlock.class})
  public abstract static class WhileFalsePrimitiveNode extends WhilePrimitiveNode {
    public WhileFalsePrimitiveNode(final boolean eagPrim, final SourceSection source) { super(eagPrim, source, false); }
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
