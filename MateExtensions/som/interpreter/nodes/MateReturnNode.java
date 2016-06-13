package som.interpreter.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import som.interpreter.SArguments;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateAbstractSemanticsLevelNode;
import som.matenodes.MateBehavior;
import som.vm.constants.ReflectiveOp;

public class MateReturnNode extends ExpressionNode implements MateBehavior {
  @Child MateAbstractSemanticsLevelNode   semanticCheck;
  @Child MateAbstractStandardDispatch     reflectiveDispatch;
  @Child ExpressionNode                   expression;
  
  public MateReturnNode(ExpressionNode node) {
    super(node.getSourceSection());
    this.expression = node;
    this.initializeMateSemantics(node.getSourceSection(), this.reflectiveOperation());
    this.initializeMateDispatchForFieldRead(this.getSourceSection());
  }
  
  private final BranchProfile semanticsRedefined = BranchProfile.create();

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

  @Override
  public Object executeGeneric(VirtualFrame frame) {
    Object value = expression.executeGeneric(frame);
    Object valueRedefined = this.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame), value}, semanticsRedefined);
    if (valueRedefined == null){
      return value;
    }
    return valueRedefined;
  }
  
  public ReflectiveOp reflectiveOperation(){
    return ReflectiveOp.ExecutorReturn;
  }
}
