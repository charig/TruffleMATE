package som.matenodes;

import som.interpreter.nodes.ISuperReadNode;
import som.matenodes.MateAbstractReflectiveDispatch.MateAbstractStandardDispatch;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateCachedDispatchMessageLookupNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateCachedDispatchSuperMessageLookupNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchFieldReadNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchFieldWriteNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateAbstractSemanticsLevelNode;
import som.matenodes.MateAbstractSemanticNodes.MateSemanticCheckNode;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;

public interface MateBehavior {
  public abstract MateAbstractSemanticsLevelNode getMateNode();
  public abstract MateAbstractStandardDispatch getMateDispatch();
  public abstract void setMateNode(MateAbstractSemanticsLevelNode node);
  public abstract void setMateDispatch(MateAbstractStandardDispatch node);

  public default Object doMateSemantics(final VirtualFrame frame,
      final Object[] arguments, BranchProfile semanticsRedefined) {
    DynamicObject method = this.getMateNode().execute(frame, arguments);
    if (method != null){
      semanticsRedefined.enter();
      return this.getMateDispatch().executeDispatch(frame, method, arguments[0], arguments);
    } 
    return null;      
  }
  
  public default void initializeMateSemantics(SourceSection source, ReflectiveOp operation){
    this.setMateNode(MateSemanticCheckNode.createForFullCheck(source, operation));
  }
  
  public default void initializeMateDispatchForFieldRead(SourceSection source){
    this.setMateDispatch(MateDispatchFieldReadNodeGen.create(source));
  }
  
  public default void initializeMateDispatchForFieldWrite(SourceSection source){
    this.setMateDispatch(MateDispatchFieldWriteNodeGen.create(source));
  }
  
  public default void initializeMateDispatchForArgumentReads(SourceSection source){
    this.setMateDispatch(MateDispatchFieldReadNodeGen.create(source));
  }
  
  public default void initializeMateDispatchForMessages(SourceSection source, SSymbol selector){
    this.setMateDispatch(MateCachedDispatchMessageLookupNodeGen.create(source, selector));
    //MateDispatchMessageLookupNodeGen.create(this.getSourceSection(), this.getSelector());
  }
  
  public default void initializeMateDispatchForSuperMessages(SourceSection source, SSymbol selector, ISuperReadNode node){
    this.setMateDispatch(MateCachedDispatchSuperMessageLookupNodeGen.create(source, selector, node));
  }
  
  public default NodeCost getCost() {
    return NodeCost.NONE;
  }
}
