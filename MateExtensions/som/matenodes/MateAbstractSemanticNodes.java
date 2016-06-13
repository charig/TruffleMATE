package som.matenodes;

import som.interpreter.SArguments;
import som.matenodes.MateAbstractSemanticNodesFactory.MateEnvironmentSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateObjectSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticsBaselevelNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticsMetalevelNodeGen;
import som.vm.MateUniverse;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;

public abstract class MateAbstractSemanticNodes {

  @ImportStatic(Nil.class)
  public static abstract class MateEnvironmentSemanticCheckNode extends Node {
    private final ReflectiveOp reflectiveOperation;

    protected MateEnvironmentSemanticCheckNode(ReflectiveOp operation) {
      super();
      reflectiveOperation = operation;
    }

    public abstract DynamicObject executeGeneric(VirtualFrame frame);

    
    @Specialization(guards = "getEnvironment(frame) == nilObject")
    public DynamicObject doNoSemanticsInFrame(final VirtualFrame frame) {
      return null;
    }
    
    @Specialization(guards = {"getEnvironment(frame) == cachedEnvironment"})
    public DynamicObject doSemanticsInFrame(final VirtualFrame frame,
        @Cached("getEnvironment(frame)") final DynamicObject cachedEnvironment,
        @Cached("methodImplementingOperationOn(cachedEnvironment)") final DynamicObject reflectiveMethod) {
        return reflectiveMethod;
    }
    
    public static DynamicObject getEnvironment(VirtualFrame frame){
      return SArguments.getEnvironment(frame);
    }
    
    public DynamicObject methodImplementingOperationOn(final DynamicObject environment){
      return SMateEnvironment.methodImplementing(environment, this.reflectiveOperation);
    }
    
    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }
  }

  public static abstract class MateObjectSemanticCheckNode extends Node {

    protected final ReflectiveOp reflectiveOperation;

    protected MateObjectSemanticCheckNode(ReflectiveOp operation) {
      super();
      reflectiveOperation = operation;
    }

    public abstract DynamicObject executeGeneric(VirtualFrame frame,
        Object receiver);

    @Specialization(guards = {"receiver.getShape() == cachedShape"}, limit = "1")
    public DynamicObject doMonomorhic(
        final VirtualFrame frame,
        final DynamicObject receiver,
        @Cached("receiver.getShape()") final Shape cachedShape,
        @Cached("environmentReflectiveMethod(getEnvironment(cachedShape), reflectiveOperation)") final DynamicObject method) {
      return method;
    }
    
    @Specialization(guards = {"receiver.getShape().getObjectType() == cachedType"}, contains={"doMonomorhic"}, limit = "6")
    public DynamicObject doPolymorhic(
        final VirtualFrame frame,
        final DynamicObject receiver,
        @Cached("receiver.getShape().getObjectType()") final ObjectType cachedType,
        @Cached("environmentReflectiveMethod(getEnvironment(receiver.getShape()), reflectiveOperation)") final DynamicObject method) {
      return method;
    }
    
    //@Specialization(contains={"doSReflectiveObject", "doSReflectiveObjectMega", "doStandardSOMForPrimitives"})
    @Specialization(contains={"doPolymorhic"})
    public DynamicObject doMegamorphic(
        final VirtualFrame frame,
        final DynamicObject receiver) {
      return environmentReflectiveMethod(SReflectiveObject.getEnvironment(receiver), this.reflectiveOperation);
    }
    
    @Specialization
    public DynamicObject doPrimitive(final VirtualFrame frame, final Object receiver){
          return null;
    }
        
    protected static DynamicObject environmentReflectiveMethod(
        DynamicObject environment, ReflectiveOp operation) {
      if (environment == Nil.nilObject){
        return null;
      }
      return SMateEnvironment.methodImplementing(environment, operation);
    }
    
    public static DynamicObject getEnvironment(Shape shape){
        return SReflectiveObject.getEnvironment(shape);
    }
    
    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }
  }

  public static abstract class MateAbstractSemanticsLevelNode extends Node {
    public abstract DynamicObject execute(final VirtualFrame frame,
        Object[] arguments);
    
    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }
  }
  
  public static abstract class MateSemanticCheckNode extends MateAbstractSemanticsLevelNode {
    @Child MateEnvironmentSemanticCheckNode environment;
    @Child MateObjectSemanticCheckNode      object;

    public MateSemanticCheckNode(final SourceSection source,
        ReflectiveOp operation) {
      super();
      environment = MateEnvironmentSemanticCheckNodeGen.create(operation);
      object = MateObjectSemanticCheckNodeGen.create(operation);
    }
    
    public MateSemanticCheckNode(final MateEnvironmentSemanticCheckNode env,
        final MateObjectSemanticCheckNode obj) {
      super();
      environment = env;
      object = obj;
    }

    public static MateSemanticCheckNode createForFullCheck(
        final SourceSection source, final ReflectiveOp operation) {
      return MateSemanticCheckNodeGen.create(
          MateEnvironmentSemanticCheckNodeGen.create(operation),
          MateObjectSemanticCheckNodeGen.create(operation));
    }
    
    @Specialization(guards = "!executeBase(frame)", assumptions = "getMateActivatedAssumption()")
    protected DynamicObject executeSOM(final VirtualFrame frame, Object[] arguments) {
      return replace(MateSemanticsMetalevelNodeGen.create()).
                  execute(frame, arguments);
    }

    @Specialization(guards = "executeBase(frame)", assumptions = "getMateActivatedAssumption()")
    protected DynamicObject executeSemanticChecks(final VirtualFrame frame, Object[] arguments) {
      return replace(MateSemanticsBaselevelNodeGen.create(environment, object)).
                  execute(frame, arguments);
    }
    
    @Specialization(assumptions = "getMateDeactivatedAssumption()")
    protected DynamicObject mateDeactivated(final VirtualFrame frame, Object[] arguments) {
      return null;
    }

    public static boolean executeBase(VirtualFrame frame) {
      return SArguments.getExecutionLevel(frame) == ExecutionLevel.Base;
    }
    
    public static Assumption getMateDeactivatedAssumption() {
      return MateUniverse.current().getMateDeactivatedAssumption();
    }
    
    public static Assumption getMateActivatedAssumption() {
      return MateUniverse.current().getMateActivatedAssumption();
    }
    
    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }
  }  
  
  public static abstract class MateSemanticsMetalevelNode extends MateAbstractSemanticsLevelNode {
    public MateSemanticsMetalevelNode() {
      super();
    }
    
    @Specialization
    public DynamicObject executeOptimized(final VirtualFrame frame,
        Object[] arguments){
      return null;
    }
  }
    
  public static abstract class MateSemanticsBaselevelNode extends MateAbstractSemanticsLevelNode {
    @Child MateEnvironmentSemanticCheckNode environment;
    @Child MateObjectSemanticCheckNode      object;
    final BranchProfile executeObjectSemantics = BranchProfile.create();
    
    public MateSemanticsBaselevelNode(MateEnvironmentSemanticCheckNode env, MateObjectSemanticCheckNode obj) {
      super();
      environment = env;
      object = obj;
    }
    
    @Specialization
    public DynamicObject executeOptimized(final VirtualFrame frame,
        Object[] arguments){
      DynamicObject value = environment.executeGeneric(frame);
      if (value == null){  
        executeObjectSemantics.enter();
        return object.executeGeneric(frame, arguments[0]);
      } 
      return value;
    }
  }
}
