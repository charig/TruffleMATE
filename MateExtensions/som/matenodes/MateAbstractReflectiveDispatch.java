package som.matenodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ValueProfile;

import som.interpreter.SArguments;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.ISuperReadNode;
import som.vm.Universe;
import som.vm.constants.Classes;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.Nil;
import som.vmobjects.MockJavaObject;
import som.vmobjects.SArray;
import som.vmobjects.SBlock;
import som.vmobjects.SInvokable;
import som.vmobjects.SInvokable.SMethod;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;

@ReportPolymorphism
public abstract class MateAbstractReflectiveDispatch extends Node {

  protected static final int INLINE_CACHE_SIZE = 6;

  public MateAbstractReflectiveDispatch() {
    super();
  }

  protected Object[] computeArgumentsForMetaDispatch(final VirtualFrame frame, final Object[] arguments) {
    return SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Meta, arguments);
  }

  public DirectCallNode createDispatch(final DynamicObject metaMethod) {
    DirectCallNode node = Universe.getCurrent().getTruffleRuntime().
        createDirectCallNode(SInvokable.getCallTarget(metaMethod, ExecutionLevel.Meta));
    node.forceInlining();
    return node;
  }

  @Override
  public NodeCost getCost() {
    return NodeCost.NONE;
  }

  public abstract Object executeDispatch(VirtualFrame frame,
      DynamicObject method, Object subject, Object[] arguments);

  public abstract static class MateDispatchFieldRead extends
      MateAbstractReflectiveDispatch {

    @Specialization(guards = "cachedMethod==method", limit = "INLINE_CACHE_SIZE")
    public Object doMateNode(final VirtualFrame frame, final DynamicObject method,
        final Object subject, final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod) {
      return reflectiveMethod.call(this.computeArgumentsForMetaDispatch(frame, arguments));
    }

    @Specialization(replaces = {"doMateNode"})
    public Object doMegaMorphic(final VirtualFrame frame, final DynamicObject method,
        final Object subject, final Object[] arguments,
        @Cached("createIndirectCall()") final IndirectCallNode callNode) {
      return callNode.call(SInvokable.getCallTarget(method, ExecutionLevel.Meta), this.computeArgumentsForMetaDispatch(frame, arguments));
    }

    @Override
    protected Object[] computeArgumentsForMetaDispatch(final VirtualFrame frame, final Object[] arguments) {
      return new Object[]{SArguments.getEnvironment(frame), ExecutionLevel.Meta, arguments[0], ((long) arguments[1]) + 1};
    }
  }

  public abstract static class MateDispatchReturn extends
    MateDispatchFieldRead {

    @Override
    protected Object[] computeArgumentsForMetaDispatch(final VirtualFrame frame, final Object[] arguments) {
      return new Object[]{SArguments.getEnvironment(frame), ExecutionLevel.Meta, arguments[0], arguments[1]};
    }
  }

  public abstract static class MateDispatchPrimFieldRead extends
      MateDispatchFieldRead{

    @Override
    protected Object[] computeArgumentsForMetaDispatch(final VirtualFrame frame, final Object[] arguments) {
      return new Object[]{SArguments.getEnvironment(frame), ExecutionLevel.Meta, arguments[0], ((long) arguments[1]) - 1};
    }
  }

  public abstract static class MateDispatchLocalVarRead extends
      MateDispatchFieldRead {
    final DynamicObject context;

    public MateDispatchLocalVarRead() {
      context = Universe.getCurrent().getObjectMemory().getGlobal(Universe.getCurrent().symbolFor("Context"));
    }

    @Override
    protected Object[] computeArgumentsForMetaDispatch(final VirtualFrame frame, final Object[] arguments) {
      return new Object[]{SArguments.getEnvironment(frame), ExecutionLevel.Meta, arguments[0],
          arguments[1], new MockJavaObject(frame.materialize(), context)};
    }
  }

  public abstract static class MateDispatchLocalVarWrite extends
      MateDispatchLocalVarRead {
    @Override
    protected Object[] computeArgumentsForMetaDispatch(final VirtualFrame frame, final Object[] arguments) {
      return new Object[]{SArguments.getEnvironment(frame), ExecutionLevel.Meta, arguments[0],
          arguments[1],
          new MockJavaObject(frame.materialize(), context),
          ((ExpressionNode) arguments[2]).executeGeneric(frame)};
    }
  }

  public abstract static class MateDispatchFieldWrite extends
      MateDispatchFieldRead {

    @Override
    protected Object[] computeArgumentsForMetaDispatch(final VirtualFrame frame, final Object[] arguments) {
      return new Object[]{SArguments.getEnvironment(frame), ExecutionLevel.Meta, arguments[0], ((long) arguments[1]) + 1, arguments[2]};
    }
  }

  public abstract static class MateDispatchPrimFieldWrite extends
      MateDispatchFieldWrite {

    @Override
    protected Object[] computeArgumentsForMetaDispatch(final VirtualFrame frame, final Object[] arguments) {
      return new Object[]{SArguments.getEnvironment(frame), ExecutionLevel.Meta, arguments[0], (long) (arguments[1]) - 1, arguments[2]};
    }
  }

  public abstract static class MateDispatchMessageLookup extends
      MateAbstractReflectiveDispatch {

    private final SSymbol    selector;
    @Child IntercessionHandling ih;

    public MateDispatchMessageLookup(final SSymbol sel) {
      selector = sel;
      ih = IntercessionHandling.createForMethodActivation(selector);
      this.adoptChildren();
    }

    @Specialization(guards = {"cachedMethod==method"})
    public Object doMateNode(final VirtualFrame frame, final DynamicObject method,
        final DynamicObject subject, final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod,
        @Cached("createIndirectCall()") final IndirectCallNode cachedCall) {
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      DynamicObject actualMethod = this.reflectiveLookup(frame, reflectiveMethod, subject, lookupSinceFor(subject));
      // return activationNode.doActivation(frame, actualMethod, arguments);
      Object[] realArgs = (Object[]) ih.doMateSemantics(frame, arguments);
      if (realArgs == null) {
        realArgs = SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments);
      }
      return cachedCall.call(SMethod.getCallTarget(actualMethod, ExecutionLevel.Base), realArgs);
    }

    public DynamicObject reflectiveLookup(final VirtualFrame frame, final DirectCallNode reflectiveMethod,
        final Object receiver, final DynamicObject lookupSince) {
      Object[] args = {SArguments.getEnvironment(frame), ExecutionLevel.Meta, receiver, this.getSelector(), lookupSince};
      return (DynamicObject) reflectiveMethod.call(args);
    }

    protected DynamicObject lookupSinceFor(final DynamicObject receiver) {
      return SObject.getSOMClass(receiver);
    }

    protected SSymbol getSelector() {
      return selector;
    }

    public static IndirectCallNode createIndirectCall() {
      return IndirectCallNode.create();
    }
  }

  public abstract static class MateDispatchSuperMessageLookup extends MateDispatchMessageLookup{
    @Child private ISuperReadNode superNode;

    public MateDispatchSuperMessageLookup(final SSymbol sel, final ISuperReadNode node) {
      super(sel);
      superNode = node;
      this.adoptChildren();
    }

    @Override
    protected DynamicObject lookupSinceFor(final DynamicObject receiver) {
      return superNode.getLexicalSuperClass();
    }
  }

  @ImportStatic(Classes.class)
  public abstract static class MateCachedDispatchMessageLookup extends
    MateDispatchMessageLookup {

    public MateCachedDispatchMessageLookup(final SSymbol sel) {
      super(sel);
    }

    @Specialization(guards = {"cachedMethod==method"}, insertBefore = "doMateNode")
    public Object doMateLongNodeCached(final VirtualFrame frame, final DynamicObject method,
        final long subject, final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("lookupResultFixedType(frame, method, subject, arguments, integerClass)") final DynamicObject lookupResult,
        @Cached("createDirectCall(lookupResult)") final DirectCallNode cachedCall) {
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      Object[] realArgs = (Object[]) ih.doMateSemantics(frame, arguments);
      if (realArgs == null) {
        realArgs = SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments);
      }
      return cachedCall.call(realArgs);
    }

    @Specialization(guards = {"cachedMethod==method"}, insertBefore = "doMateNode")
    public Object doMateStringNodeCached(final VirtualFrame frame, final DynamicObject method,
        final String subject, final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("lookupResultFixedType(frame, method, subject, arguments, stringClass)") final DynamicObject lookupResult,
        @Cached("createDirectCall(lookupResult)") final DirectCallNode cachedCall) {
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      Object[] realArgs = (Object[]) ih.doMateSemantics(frame, arguments);
      if (realArgs == null) {
        realArgs = SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments);
      }
      return cachedCall.call(realArgs);
    }

    @Specialization(guards = {"cachedMethod==method"}, insertBefore = "doMateNode")
    public Object doMateDoubleNodeCached(final VirtualFrame frame, final DynamicObject method,
        final double subject, final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("lookupResultFixedType(frame, method, subject, arguments, doubleClass)") final DynamicObject lookupResult,
        @Cached("createDirectCall(lookupResult)") final DirectCallNode cachedCall) {
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      Object[] realArgs = (Object[]) ih.doMateSemantics(frame, arguments);
      if (realArgs == null) {
        realArgs = SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments);
      }
      return cachedCall.call(realArgs);
    }

    @Specialization(guards = {"cachedMethod==method"}, insertBefore = "doMateNode")
    public Object doMateBooleanNodeCached(final VirtualFrame frame, final DynamicObject method,
        final boolean subject, final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("lookupResultFixedType(frame, method, subject, arguments, booleanClass)") final DynamicObject lookupResult,
        @Cached("createDirectCall(lookupResult)") final DirectCallNode cachedCall) {
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      Object[] realArgs = (Object[]) ih.doMateSemantics(frame, arguments);
      if (realArgs == null) {
        realArgs = SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments);
      }
      return cachedCall.call(realArgs);
    }

    @Specialization(guards = {"cachedMethod==method"}, insertBefore = "doMateNode")
    public Object doMateSSymbolNodeCached(final VirtualFrame frame, final DynamicObject method,
        final SSymbol subject, final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("lookupResultFixedType(frame, method, subject, arguments, subject.getSOMClass())") final DynamicObject lookupResult,
        @Cached("createDirectCall(lookupResult)") final DirectCallNode cachedCall) {
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      Object[] realArgs = (Object[]) ih.doMateSemantics(frame, arguments);
      if (realArgs == null) {
        realArgs = SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments);
      }
      return cachedCall.call(realArgs);
    }

    @Specialization(guards = {"cachedMethod==method"}, insertBefore = "doMateNode")
    public Object doMateSArrayNodeCached(final VirtualFrame frame, final DynamicObject method,
        final SArray subject, final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("lookupResultFixedType(frame, method, subject, arguments, subject.getSOMClass())") final DynamicObject lookupResult,
        @Cached("createDirectCall(lookupResult)") final DirectCallNode cachedCall) {
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      Object[] realArgs = (Object[]) ih.doMateSemantics(frame, arguments);
      if (realArgs == null) {
        realArgs = SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments);
      }
      return cachedCall.call(realArgs);
    }

    @Specialization(guards = {"cachedMethod==method"}, insertBefore = "doMateNode")
    public Object doMateSArrayNodeCached(final VirtualFrame frame, final DynamicObject method,
        final SBlock subject, final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("lookupResultFixedType(frame, method, subject, arguments, subject.getSOMClass())") final DynamicObject lookupResult,
        @Cached("createDirectCall(lookupResult)") final DirectCallNode cachedCall) {
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      Object[] realArgs = (Object[]) ih.doMateSemantics(frame, arguments);
      if (realArgs == null) {
        realArgs = SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments);
      }
      return cachedCall.call(realArgs);
    }

    @Specialization(guards = {"cachedMethod == method", "shapeOfReceiver(arguments) == cachedShape"},
        insertBefore = "doMateNode", limit = "INLINE_CACHE_SIZE")
    public Object doMateNodeCached(final VirtualFrame frame, final DynamicObject method,
        final DynamicObject subject, final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("shapeOfReceiver(arguments)") final Shape cachedShape,
        @Cached("lookupResult(frame, method, subject, arguments)") final DynamicObject lookupResult,
        @Cached("createDirectCall(lookupResult)") final DirectCallNode cachedCall) {
      // The MOP receives the class where the lookup must start (find: aSelector since: aClass)
      Object[] realArgs = (Object[]) ih.doMateSemantics(frame, arguments);
      if (realArgs == null) {
        realArgs = SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments);
      }
      return cachedCall.call(realArgs);
    }

    @Specialization(guards = {"cachedMethod==method"}, replaces = {"doMateNodeCached"}, insertBefore = "doMateNode")
    public Object doMegaMorphic(final VirtualFrame frame, final DynamicObject method,
        final DynamicObject subject, final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod,
        @Cached("createIndirectCall()") final IndirectCallNode indirect) {
      return super.doMateNode(frame, method, subject, arguments, cachedMethod, reflectiveMethod, indirect);
    }

    protected Shape shapeOfReceiver(final Object[] arguments) {
      return ((DynamicObject) arguments[0]).getShape();
    }

    public DynamicObject lookupResult(final VirtualFrame frame, final DynamicObject method,
        final DynamicObject receiver, final Object[] arguments) {
        return this.reflectiveLookup(frame, this.createDispatch(method), receiver, lookupSinceFor(receiver));
    }

    public DynamicObject lookupResultFixedType(final VirtualFrame frame, final DynamicObject method,
        final Object receiver, final Object[] arguments, final DynamicObject sinceClass) {
        return this.reflectiveLookup(frame, this.createDispatch(method), receiver, sinceClass);
    }

    public static DirectCallNode createDirectCall(final DynamicObject methodToActivate) {
      DirectCallNode node = DirectCallNode.create(SInvokable.getCallTarget(methodToActivate, ExecutionLevel.Base));
      node.forceInlining();
      return node;
    }
  }

  public abstract static class MateCachedDispatchSuperMessageLookup extends MateCachedDispatchMessageLookup {
    @Child private ISuperReadNode superNode;

    public MateCachedDispatchSuperMessageLookup(final SSymbol sel, final ISuperReadNode node) {
      super(sel);
      superNode = node;
      this.adoptChildren();
    }

    @Override
    protected DynamicObject lookupSinceFor(final DynamicObject receiver) {
      return superNode.getLexicalSuperClass();
    }
  }

  public abstract static class MateActivationDispatch extends
      MateAbstractReflectiveDispatch {
    private final SSymbol selector;

    public MateActivationDispatch(final SSymbol sel) {
      selector = sel;
    }

    @Specialization(guards = {"cachedMethod==method"}, limit = "INLINE_CACHE_SIZE")
    public Object[] doMetaLevel(final VirtualFrame frame,
        final DynamicObject method, final Object subject,
        final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        // @Cached("methodToActivate") final DynamicObject cachedMethodToActivate,
        // @Cached("createDirectCall(methodToActivate)") final DirectCallNode callNode,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod,
        @Cached("classProfile()") final ValueProfile profile) {
      // The MOP receives the standard ST message Send stack (rcvr, method, arguments) and returns its own
      Object[] args = {Nil.nilObject, ExecutionLevel.Meta, arguments[0], selector,
          SArray.create(SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments))};
      return ((SArray) reflectiveMethod.call(args)).toJavaArray(profile);
      // return callNode.call(realArguments.toJavaArray(profile));
    }

    /*@Specialization(guards = {"cachedMethod==method"}, replaces = "doMetaLevel")
    public Object doMegamorphicMetaLevel(final VirtualFrame frame,
        final DynamicObject method, final DynamicObject methodToActivate,
        final Object[] arguments,
        @Cached("method") final DynamicObject cachedMethod,
        @Cached("createDispatch(method)") final DirectCallNode reflectiveMethod,
        @Cached("createIndirectCall()") final IndirectCallNode callNode,
        @Cached("classProfile()") final ValueProfile profile) {
      Object[] args = {Nil.nilObject, ExecutionLevel.Meta, arguments[0], methodToActivate,
          SArray.create(SArguments.createSArguments(SArguments.getEnvironment(frame), ExecutionLevel.Base, arguments))};
      SArray realArguments = (SArray) reflectiveMethod.call(args);
      return callNode.call(SInvokable.getCallTarget(methodToActivate, ExecutionLevel.Base), realArguments.toJavaArray(profile));
    }*/
  }

  public static IndirectCallNode createIndirectCall() {
    return IndirectCallNode.create();
  }

  protected ValueProfile classProfile() {
    return ValueProfile.createClassProfile();
  }
}
