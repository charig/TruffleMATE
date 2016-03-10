package som.matenodes;

import som.interpreter.SArguments;
import som.interpreter.nodes.ISuperReadNode;
import som.interpreter.nodes.MateMethodActivationNode;
import som.vm.MateUniverse;
import som.vm.constants.ExecutionLevel;
import som.vmobjects.SArray;
import som.vmobjects.SInvokable;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

public abstract class MateAbstractReflectiveDispatch extends Node {

  protected final static int INLINE_CACHE_SIZE = 6;
  
  public MateAbstractReflectiveDispatch(final SourceSection source) {
    super(source);
  }

  protected Object[] computeArgumentsForMetaDispatch(VirtualFrame frame, Object[] arguments) {
    return SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Meta, arguments);
  }

  public DirectCallNode createDispatch(final SInvokable metaMethod) {
    DirectCallNode node = MateUniverse.current().getTruffleRuntime()
        .createDirectCallNode(metaMethod.getCallTarget());
    node.forceInlining();
    return node;
  }

  public abstract static class MateDispatchFieldAccessor extends
      MateAbstractReflectiveDispatch {

    public MateDispatchFieldAccessor(final SourceSection source) {
      super(source);
    }
  }
  
  @Override
  public NodeCost getCost() {
    return NodeCost.NONE;
  }
  
  public abstract static class MateAbstractStandardDispatch extends
      MateAbstractReflectiveDispatch {

     @Child IndirectCallNode dispatch;
      
    public MateAbstractStandardDispatch(SourceSection source) {
      super(source);
      dispatch = IndirectCallNode.create();
    }

    public abstract Object executeDispatch(final VirtualFrame frame,
        SInvokable method, Object[] arguments);

    @Specialization
    public Object doMateNode(final VirtualFrame frame, final SInvokable method,
        final Object[] arguments) {
      Object value = dispatch.call(frame, method.getCallTarget(), this.computeArgumentsForMetaDispatch(frame, arguments));
      return value;
    }
  }

  public abstract static class MateDispatchFieldAccess extends
      MateAbstractStandardDispatch {

    public MateDispatchFieldAccess(SourceSection source) {
      super(source);
    }
  }

  public abstract static class MateDispatchMessageLookup extends
      MateAbstractStandardDispatch {

    private final SSymbol    selector;
    @Child MateMethodActivationNode activationNode;

    public MateDispatchMessageLookup(SourceSection source, SSymbol sel) {
      super(source);
      selector = sel;
      activationNode = new MateMethodActivationNode();
    }

    @Specialization
    @Override
    public Object doMateNode(final VirtualFrame frame, final SInvokable method,
        final Object[] arguments) {
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      SInvokable actualMethod = this.reflectiveLookup(frame, dispatch, arguments, method);
      return activationNode.doActivation(frame, actualMethod, arguments);
    }
    
    public SInvokable reflectiveLookup(final VirtualFrame frame, IndirectCallNode reflectiveMethod,
        final Object[] arguments, SInvokable method) {
      DynamicObject receiver = (DynamicObject) arguments[0];
      Object[] args = { SArguments.getEnvironment(frame), ExecutionLevel.Meta, receiver, this.getSelector(), this.lookupSinceFor(receiver)};
      return (SInvokable) reflectiveMethod.call(frame, method.getCallTarget(), args);
    }
    
    protected DynamicObject lookupSinceFor(DynamicObject receiver){
      return SObject.getSOMClass(receiver);
    }

    protected SSymbol getSelector() {
      return selector;
    }    
  }
  
  public abstract static class MateDispatchSuperMessageLookup extends MateDispatchMessageLookup{
    ISuperReadNode superNode;
    
    public MateDispatchSuperMessageLookup(SourceSection source, SSymbol sel, ISuperReadNode node) {
      super(source, sel);
      superNode = node;
    }

    @Override
    protected DynamicObject lookupSinceFor(DynamicObject receiver){
      return superNode.getLexicalSuperClass();
    }
  }
  
  /*public abstract static class MateCachedDispatchMessageLookup extends
    MateDispatchMessageLookup {

    public MateCachedDispatchMessageLookup(SourceSection source, SSymbol sel) {
      super(source, sel);
    }
    
    @Specialization(guards = {"cachedMethod==method", "classOfReceiver(arguments) == cachedClass"}, 
        insertBefore="doMateNode", limit = "INLINE_CACHE_SIZE")
    public Object doMateNodeCached(final VirtualFrame frame, final SInvokable method,
        final Object[] arguments,
        @Cached("method") final SInvokable cachedMethod,
        @Cached("classOfReceiver(arguments)") final DynamicObject cachedClass,
        @Cached("lookupResult(frame, method, arguments)") final SInvokable lookupResult){
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      return activationNode.doActivation(frame, lookupResult, arguments);
    }
    
    @Specialization(guards = {"cachedMethod==method"}, contains = {"doMateNodeCached"}, insertBefore="doMateNode")
    public Object doMegaMorphic(final VirtualFrame frame, final SInvokable method,
        final Object[] arguments,
        @Cached("method") final SInvokable cachedMethod,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod) {
      return super.doMateNode(frame, method, arguments, cachedMethod, reflectiveMethod);
    }
    
    public SInvokable lookupResult(final VirtualFrame frame, final SInvokable method,
        final Object[] arguments){
      return this.reflectiveLookup(frame, this.createDispatch(method), arguments);
    }
    
    protected DynamicObject classOfReceiver(Object[] arguments){
      return SObject.getSOMClass((DynamicObject) arguments[0]);
    }

  }
  
  public abstract static class MateCachedDispatchSuperMessageLookup extends MateCachedDispatchMessageLookup{
    ISuperReadNode superNode;
    
    public MateCachedDispatchSuperMessageLookup(SourceSection source, SSymbol sel, ISuperReadNode node) {
      super(source, sel);
      superNode = node;
    }

    @Override
    protected DynamicObject lookupSinceFor(DynamicObject receiver){
      return superNode.getLexicalSuperClass();
    }
  }*/
  
  public abstract static class MateActivationDispatch extends
      MateAbstractReflectiveDispatch {
    
    @Child IndirectCallNode dispatchToActivate;
    @Child IndirectCallNode dispatchToMetaobject;
    
    public MateActivationDispatch(SourceSection source) {
      super(source);
      dispatchToActivate = IndirectCallNode.create();
      dispatchToMetaobject = IndirectCallNode.create();    
    }

    public abstract Object executeDispatch(final VirtualFrame frame,
        SInvokable method, SInvokable methodToActivate, Object[] arguments);

    @Specialization(guards = {"cachedMethod==method","methodToActivate == cachedMethodToActivate"}, limit = "INLINE_CACHE_SIZE")
    public Object doMetaLevel(final VirtualFrame frame,
        final SInvokable method, final SInvokable methodToActivate,
        final Object[] arguments,
        @Cached("method") final SInvokable cachedMethod,
        @Cached("methodToActivate") final SInvokable cachedMethodToActivate,
        @Cached("createDirectCall(methodToActivate)") final DirectCallNode callNode,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod) {
      // The MOP receives the standard ST message Send stack (rcvr, method, arguments) and returns its own
      Object[] args = { SArguments.getEnvironment(frame), ExecutionLevel.Meta, (DynamicObject) arguments[0], methodToActivate, 
          SArray.create(SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments))};
      SArray realArguments = (SArray)dispatchToMetaobject.call(frame, method.getCallTarget(), args);
      return dispatchToActivate.call(frame, methodToActivate.getCallTarget(), realArguments.toJavaArray());
    }
    
    @Specialization(guards = {"cachedMethod==method"}, contains = "doMetaLevel")
    public Object doMegamorphicMetaLevel(final VirtualFrame frame,
        final SInvokable method, final SInvokable methodToActivate,
        final Object[] arguments,
        @Cached("method") final SInvokable cachedMethod,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod,
        @Cached("createIndirectCall()") final IndirectCallNode callNode){
      Object[] args = { SArguments.getEnvironment(frame), ExecutionLevel.Meta, (DynamicObject) arguments[0], methodToActivate, 
          SArray.create(SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments))};
      SArray realArguments = (SArray)reflectiveMethod.call(frame, args);
      return callNode.call(frame, methodToActivate.getCallTarget(), realArguments.toJavaArray());
    }
  }
  
  public static DirectCallNode createDirectCall(SInvokable methodToActivate){
    DirectCallNode node = DirectCallNode.create(methodToActivate.getCallTarget()); 
    node.forceInlining();
    return node;
  }
  
  public static IndirectCallNode createIndirectCall(){
    return IndirectCallNode.create();
  }
}
