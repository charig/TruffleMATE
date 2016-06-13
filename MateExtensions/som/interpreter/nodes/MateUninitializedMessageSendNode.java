package som.interpreter.nodes;

import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.interpreter.nodes.MessageSendNode.UninitializedMessageSendNode;
import som.interpreter.nodes.dispatch.UninitializedDispatchNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateAbstractSemanticsLevelNode;
import som.matenodes.MateBehavior;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;

public class MateUninitializedMessageSendNode extends
    UninitializedMessageSendNode implements MateBehavior {
  @Child MateAbstractSemanticsLevelNode   semanticCheck;
  @Child MateAbstractStandardDispatch     reflectiveDispatch;
  private final BranchProfile semanticsRedefined = BranchProfile.create();
  
  public MateUninitializedMessageSendNode(UninitializedMessageSendNode somNode) {
    super(somNode.getSelector(), somNode.argumentNodes, somNode.getSourceSection());
    this.initializeMateSemantics(this.getSourceSection(), this.reflectiveOperation());
    if (this.isSuperSend()){
      ISuperReadNode superNode = (ISuperReadNode)this.argumentNodes[0];
      this.initializeMateDispatchForSuperMessages(this.getSourceSection(), this.getSelector(), superNode);
    } else {
      this.initializeMateDispatchForMessages(this.getSourceSection(), this.getSelector());
    }
  }
  
  @Override
  public final Object executeGeneric(final VirtualFrame frame) {
    Object[] arguments = evaluateArguments(frame);
    Object value = this.doMateSemantics(frame, arguments, semanticsRedefined);
    if (value == null){
     value = doPreEvaluated(frame, arguments);
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
  
  @Override
  public ExpressionNode asMateNode() {
    return null;
  }
  
  @Override
  protected GenericMessageSendNode makeGenericSend() {
    GenericMessageSendNode send = new MateGenericMessageSendNode(selector,
        argumentNodes,
        new UninitializedDispatchNode(selector),
        getSourceSection());
    return replace(send);
  }
}