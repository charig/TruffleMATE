package som.interpreter.nodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.LocalVariableNode.LocalVariableReadNode;
import som.interpreter.nodes.LocalVariableNode.LocalVariableWriteNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractSemanticNodes.MateAbstractSemanticsLevelNode;
import som.matenodes.MateBehavior;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;

public abstract class MateLocalVariableNode {
  public static class MateLocalVariableReadNode extends LocalVariableReadNode implements
      MateBehavior {
    
    public MateLocalVariableReadNode(LocalVariableReadNode node) {
      super(node);
      this.local = node;
      this.initializeMateSemantics(node.getSourceSection(), this.reflectiveOperation());
    }

    @Child MateAbstractSemanticsLevelNode   semanticCheck;
    @Child MateAbstractStandardDispatch     reflectiveDispatch;
    @Child LocalVariableNode                local;
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
      Object value = this.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)}, semanticsRedefined);
      if (value == null){
       value = local.executeGeneric(frame);
      }
      return value;
    }
  }
  
  public static class MateLocalVariableWriteNode extends LocalVariableWriteNode implements
      MateBehavior {
    
    @Child MateAbstractSemanticsLevelNode   semanticCheck;
    @Child MateAbstractStandardDispatch     reflectiveDispatch;
    @Child LocalVariableWriteNode           local;
    private final BranchProfile semanticsRedefined = BranchProfile.create();
    
    public MateLocalVariableWriteNode(LocalVariableWriteNode node) {
      super(node);
      this.local = node;
      this.initializeMateSemantics(node.getSourceSection(), this.reflectiveOperation());
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
    public Object executeGeneric(VirtualFrame frame) {
      Object value = this.doMateSemantics(frame, new Object[] {SArguments.rcvr(frame)}, semanticsRedefined);
      if (value == null){
       value = local.executeGeneric(frame);
      }
      return value;
    }

    @Override
    public ExpressionNode getExp() {
      return local.getExp();
    }
  }
}
