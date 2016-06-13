package som.interpreter.nodes.nary;

import som.interpreter.nodes.ExpressionNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateAbstractSemanticsLevelNode;
import som.matenodes.MateBehavior;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;


public class MateEagerUnaryPrimitiveNode extends EagerUnaryPrimitiveNode implements MateBehavior {
  @Child MateAbstractSemanticsLevelNode   semanticCheck;
  @Child MateAbstractStandardDispatch     reflectiveDispatch;
  private final BranchProfile semanticsRedefined = BranchProfile.create();
  
  public MateEagerUnaryPrimitiveNode(SSymbol selector, ExpressionNode receiver,
      UnaryExpressionNode primitive) {
    super(selector, receiver, primitive);
    this.initializeMateSemantics(this.getSourceSection(), this.reflectiveOperation());
    this.initializeMateDispatchForMessages(this.getSourceSection(), this.getSelector());
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    Object rcvr = this.getReceiver().executeGeneric(frame);
    Object value = this.doMateSemantics(frame, new Object[] {rcvr}, semanticsRedefined);
    if (value == null){
     value = executeEvaluated(frame, rcvr);
    }
    return value;
  }

  @Override
  public MateAbstractSemanticsLevelNode getMateNode() {
    return semanticCheck;
  }

  @Override
  public MateAbstractStandardDispatch getMateDispatch() {
    return reflectiveDispatch;
  }
  
  @Override
  public void setMateNode(MateAbstractSemanticsLevelNode node) {
    semanticCheck = node;
  }

  @Override
  public void setMateDispatch(MateAbstractStandardDispatch node) {
    reflectiveDispatch = node;
  }
}
