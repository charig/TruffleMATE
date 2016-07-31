package som.matenodes;

import som.interpreter.SArguments;
import som.matenodes.MateAbstractSemanticNodesFactory.MateEnvironmentSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateObjectSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticCheckNodeGen;
import som.vm.MateUniverse;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SInvokable;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;

public abstract class MateAbstractSemanticNodes {

  public static abstract class MateEnvironmentSemanticCheckNode extends Node {
    private final ReflectiveOp reflectiveOperation;

    protected MateEnvironmentSemanticCheckNode(ReflectiveOp operation) {
      super();
      reflectiveOperation = operation;
    }

    public abstract SInvokable executeGeneric(VirtualFrame frame);

    
    @Specialization(guards = "getEnvironment(frame) == null")
    public SInvokable doNoSemanticsInFrame(final VirtualFrame frame) {
      return null;
    }
    
    @Specialization(guards = {"getEnvironment(frame) == environment"})
    public SInvokable doSemanticsInFrame(final VirtualFrame frame,
        @Cached("getEnvironment(frame)") final DynamicObject environment,
        @Cached("methodImplementingOperationOn(environment)") final SInvokable reflectiveMethod) {
        return reflectiveMethod;
    }
    
    public static DynamicObject getEnvironment(VirtualFrame frame){
      return SArguments.getEnvironment(frame);
    }
    
    public SInvokable methodImplementingOperationOn(final DynamicObject environment){
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

    public abstract SInvokable executeGeneric(VirtualFrame frame,
        Object receiver);

    @Specialization(guards = {"receiver.getShape() == cachedShape"}, limit = "1")
    public SInvokable doSReflectiveObject(
        final VirtualFrame frame,
        final DynamicObject receiver,
        @Cached("receiver.getShape()") final Shape cachedShape,
        @Cached("environmentReflectiveMethod(getEnvironment(cachedShape), reflectiveOperation)") final SInvokable method) {
      return method;
    }
    
    @Specialization(guards = {"receiver.getShape().getObjectType() == cachedType"}, contains={"doSReflectiveObject"}, limit = "6")
    public SInvokable doSReflectiveObjectMega(
        final VirtualFrame frame,
        final DynamicObject receiver,
        @Cached("receiver.getShape().getObjectType()") final ObjectType cachedType,
        @Cached("environmentReflectiveMethod(getEnvironment(receiver.getShape()), reflectiveOperation)") final SInvokable method) {
      return method;
    }
    
    //@Specialization(contains={"doSReflectiveObject", "doSReflectiveObjectMega", "doStandardSOMForPrimitives"})
    @Specialization(contains={"doSReflectiveObjectMega"})
    public SInvokable doMegamorphicReceiver(
        final VirtualFrame frame,
        final DynamicObject receiver) {
      return environmentReflectiveMethod(SReflectiveObject.getEnvironment(receiver), this.reflectiveOperation);
    }
    
    protected static SInvokable environmentReflectiveMethod(
        DynamicObject environment, ReflectiveOp operation) {
      if (environment == Nil.nilObject){
        return null;
      }
      return SMateEnvironment.methodImplementing(environment, operation);
    }
    
    public static DynamicObject getEnvironment(Shape shape){
      try {
        return SReflectiveObject.getEnvironment(shape);
      } catch (Exception e){
        return Nil.nilObject;
      }
    }
    
    public static boolean isSReflectiveObject(DynamicObject object){
      /*return 
           SReflectiveObject.isSReflectiveObject(object) | 
           SObject.isSObject(object) |
           SClass.isSClass(object);*/
      //return SReflectiveObject.isSReflectiveObject(object);
      return true;
    }
    
    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }
  }

  public static abstract class MateSemanticCheckNode extends Node {
    @Child MateEnvironmentSemanticCheckNode environment;
    @Child MateObjectSemanticCheckNode      object;

    public abstract SInvokable execute(final VirtualFrame frame,
        Object[] arguments);

    public MateSemanticCheckNode(MateEnvironmentSemanticCheckNode env,
        MateObjectSemanticCheckNode obj) {
      super();
      environment = env;
      object = obj;
    }

    public static MateSemanticCheckNode createForFullCheck(
        SourceSection source, ReflectiveOp operation) {
      return MateSemanticCheckNodeGen.create(
          MateEnvironmentSemanticCheckNodeGen.create(operation),
          MateObjectSemanticCheckNodeGen.create(operation));
    }
    
    @Specialization(guards = "!executeBase(frame)" , 
        assumptions = "getMateActivatedAssumption()")
    protected SInvokable executeSOM(final VirtualFrame frame, Object[] arguments) {
      return null;
    }

    @Specialization(assumptions = "getMateActivatedAssumption()")
    protected SInvokable executeSemanticChecks(final VirtualFrame frame,
        Object[] arguments) {
      if (arguments[0] instanceof DynamicObject){
        SInvokable value = environment.executeGeneric(frame);
        if (value == null){  
          return object.executeGeneric(frame, arguments[0]);
        } else {
          return value;
        }
      } else {
        return null;
      }
    }
    
    @Specialization(assumptions = "getMateDeactivatedAssumption()")
    protected SInvokable mateDeactivated(final VirtualFrame frame, Object[] arguments) {
      return null;
    }
    

    public MateSemanticCheckNode(final SourceSection source,
        ReflectiveOp operation) {
      super();
      environment = MateEnvironmentSemanticCheckNodeGen.create(operation);
      object = MateObjectSemanticCheckNodeGen.create(operation);
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
}
