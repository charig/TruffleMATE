package som.matenodes;

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

import som.interpreter.SArguments;
import som.matenodes.MateAbstractSemanticNodesFactory.MateEnvironmentSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateGlobalSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateObjectSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticCheckNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticsBaselevelNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticsBaselevelNodeUnoptNodeGen;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticsMetalevelNodeGen;
import som.vm.Universe;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SReflectiveObject;

public abstract class MateAbstractSemanticNodes extends Node {
  protected final ReflectiveOp reflectiveOperation;

  protected MateAbstractSemanticNodes(final ReflectiveOp operation) {
    this.reflectiveOperation = operation;
  }

  protected DynamicObject methodImplementingOperationOn(final DynamicObject environment) {
    return SMateEnvironment.methodImplementing(environment, this.reflectiveOperation);
  }

  @Override
  public NodeCost getCost() {
    return NodeCost.NONE;
  }

  public abstract static class MateGlobalSemanticCheckNode extends MateAbstractSemanticNodes {

    protected MateGlobalSemanticCheckNode(final ReflectiveOp operation) {
      super(operation);
    }

    public abstract DynamicObject executeGeneric(VirtualFrame frame);

    @Specialization(assumptions = "getGlobalSemanticsActivatedAssumption()")
    public DynamicObject doCheck(final VirtualFrame frame,
        @Cached("getGlobalEnvironment()") final DynamicObject cachedEnvironment,
        @Cached("methodImplementingOperationOn(cachedEnvironment)") final DynamicObject reflectiveMethod) {
      return reflectiveMethod;
    }

    public static Assumption getGlobalSemanticsActivatedAssumption() {
      return Universe.getCurrent().getGlobalSemanticsActivatedAssumption();
    }

    public static DynamicObject getGlobalEnvironment() {
      return Universe.getCurrent().getGlobalSemantics();
    }
  }

  @ImportStatic(Nil.class)
  public abstract static class MateEnvironmentSemanticCheckNode extends MateAbstractSemanticNodes {
    public abstract DynamicObject executeGeneric(VirtualFrame frame);

    protected MateEnvironmentSemanticCheckNode(final ReflectiveOp operation) {
      super(operation);
    }

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

    protected static DynamicObject getEnvironment(final VirtualFrame frame) {
      return SArguments.getEnvironment(frame);
    }
  }

  public abstract static class MateObjectSemanticCheckNode extends MateAbstractSemanticNodes {
    final BranchProfile metaobjectObserved = BranchProfile.create();

    protected MateObjectSemanticCheckNode(final ReflectiveOp operation) {
      super(operation);
    }

    public abstract DynamicObject executeGeneric(VirtualFrame frame,
        Object receiver);

    @Specialization(guards = {"receiver.getShape() == cachedShape"}, limit = "3")
    public DynamicObject doWarmup(
        final VirtualFrame frame,
        final DynamicObject receiver,
        @Cached("receiver.getShape()") final Shape cachedShape,
        @Cached("environmentReflectiveMethod(getEnvironment(cachedShape), reflectiveOperation)") final DynamicObject method) {
      return method;
    }

    @Specialization(guards = {"receiver.getShape() == cachedShape"}, replaces = {"doWarmup"}, limit = "3")
    public DynamicObject doMonomorhic(
        final VirtualFrame frame,
        final DynamicObject receiver,
        @Cached("receiver.getShape()") final Shape cachedShape,
        @Cached("environmentReflectiveMethod(getEnvironment(cachedShape), reflectiveOperation)") final DynamicObject method) {
      return method;
    }

    @Specialization(guards = {"receiver.getShape().getObjectType() == cachedType"}, replaces = {"doWarmup"}, limit = "5")
    public DynamicObject doPolymorhic(
        final VirtualFrame frame,
        final DynamicObject receiver,
        @Cached("receiver.getShape().getObjectType()") final ObjectType cachedType,
        @Cached("environmentReflectiveMethod(getEnvironment(receiver.getShape()), reflectiveOperation)") final DynamicObject method) {
      return method;
    }

    @Specialization(replaces = {"doPolymorhic"})
    public DynamicObject doMegamorphic(
        final VirtualFrame frame,
        final DynamicObject receiver) {
      return environmentReflectiveMethod(SReflectiveObject.getEnvironment(receiver), this.reflectiveOperation);
    }

    @Specialization
    public DynamicObject doPrimitive(final VirtualFrame frame, final Object receiver) {
          return null;
    }

    protected DynamicObject environmentReflectiveMethod(
        final DynamicObject environment, final ReflectiveOp operation) {
      if (environment == Nil.nilObject) {
        return null;
      } else {
        metaobjectObserved.enter();
        return SMateEnvironment.methodImplementing(environment, operation);
      }
    }

    public static DynamicObject getEnvironment(final Shape shape) {
        return SReflectiveObject.getEnvironment(shape);
    }
  }

  public abstract static class MateAbstractSemanticsLevelNode extends Node {
    public abstract DynamicObject execute(VirtualFrame frame,
        Object[] arguments);

    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }
  }

  public abstract static class MateSemanticCheckNode extends MateAbstractSemanticsLevelNode {
    @Child MateGlobalSemanticCheckNode      global;
    @Child MateEnvironmentSemanticCheckNode environment;
    @Child MateObjectSemanticCheckNode      object;

    public MateSemanticCheckNode(final SourceSection source,
        final ReflectiveOp operation) {
      this(MateEnvironmentSemanticCheckNodeGen.create(operation),
          MateObjectSemanticCheckNodeGen.create(operation), MateGlobalSemanticCheckNodeGen.create(operation));
    }

    protected MateSemanticCheckNode(final MateEnvironmentSemanticCheckNode env,
        final MateObjectSemanticCheckNode obj, final MateGlobalSemanticCheckNode globalCheck) {
      super();
      environment = env;
      object = obj;
      global = globalCheck;
      this.adoptChildren();
    }

    public static MateSemanticCheckNode createForFullCheck(
        final SourceSection source, final ReflectiveOp operation) {
      return MateSemanticCheckNodeGen.create(source, operation);
    }

    @Specialization(guards = "!executeBase(frame)", assumptions = "getMateActivatedAssumption()")
    protected DynamicObject executeSOM(final VirtualFrame frame, final Object[] arguments) {
      return replace(MateSemanticsMetalevelNodeGen.create()).
                  execute(frame, arguments);
    }

    @Specialization(guards = "executeBase(frame)", assumptions = "getOptimizedIHAssumption()")
    protected DynamicObject executeSemanticChecks(final VirtualFrame frame, final Object[] arguments) {
      return replace(MateSemanticsBaselevelNodeGen.create(environment, object, global)).
                  execute(frame, arguments);
    }

    @Specialization(guards = "executeBase(frame)", assumptions = "getMateActivatedAssumption()")
    protected DynamicObject executeSemanticUnoptimizedChecks(final VirtualFrame frame, final Object[] arguments) {
      return replace(MateSemanticsBaselevelNodeUnoptNodeGen.create(environment.reflectiveOperation)).
                  execute(frame, arguments);
    }

    @Specialization(assumptions = "getMateDeactivatedAssumption()")
    protected DynamicObject mateDeactivated(final VirtualFrame frame, final Object[] arguments) {
      return null;
    }

    public static boolean executeBase(final VirtualFrame frame) {
      return SArguments.getExecutionLevel(frame) == ExecutionLevel.Base;
    }

    public static Assumption[] getMateDeactivatedAssumption() {
      return new Assumption[]{Universe.getCurrent().getMateDeactivatedAssumption()};
    }

    public static Assumption[] getMateActivatedAssumption() {
      return new Assumption[]{Universe.getCurrent().getMateActivatedAssumption()};
    }

    public static Assumption[] getOptimizedIHAssumption() {
      return new Assumption[]{Universe.getCurrent().getMateActivatedAssumption(), Universe.getCurrent().getOptimizedIHAssumption()};
    }

    @Override
    public NodeCost getCost() {
      return NodeCost.NONE;
    }

    public ReflectiveOp reflectiveOperation() {
      return this.environment.reflectiveOperation;
    }
  }

  public abstract static class MateSemanticsMetalevelNode extends MateAbstractSemanticsLevelNode {
    public MateSemanticsMetalevelNode() {
      super();
    }

    @Specialization
    public DynamicObject executeOptimized(final VirtualFrame frame,
        final Object[] arguments) {
      return null;
    }
  }

  public abstract static class MateSemanticsBaselevelNode extends MateAbstractSemanticsLevelNode {
    @Child MateGlobalSemanticCheckNode      global;
    @Child MateEnvironmentSemanticCheckNode environment;
    @Child MateObjectSemanticCheckNode      object;
    final BranchProfile executeObjectSemantics = BranchProfile.create();

    public MateSemanticsBaselevelNode(final MateEnvironmentSemanticCheckNode env,
        final MateObjectSemanticCheckNode obj, final MateGlobalSemanticCheckNode globalCheck) {
      super();
      environment = env;
      object = obj;
      global = globalCheck;
      this.adoptChildren();
    }

    @Specialization(assumptions = "getGlobalSemanticsDeactivatedAssumption()")
    public DynamicObject executeLocalSemanticsCheck(final VirtualFrame frame,
        final Object[] arguments) {
      DynamicObject value = environment.executeGeneric(frame);
      if (value == null) {
        executeObjectSemantics.enter();
        return object.executeGeneric(frame, arguments[0]);
      }
      return value;
    }

    @Specialization(assumptions = "getGlobalSemanticsActivatedAssumption()")
    public DynamicObject executeGlobalSemanticsCheck(final VirtualFrame frame,
        final Object[] arguments) {
      DynamicObject value = global.executeGeneric(frame);
      if (value == null) {
        return this.executeLocalSemanticsCheck(frame, arguments);
      }
      return value;
    }

    public static Assumption getGlobalSemanticsDeactivatedAssumption() {
      return Universe.getCurrent().getGlobalSemanticsDeactivatedAssumption();
    }

    public static Assumption getGlobalSemanticsActivatedAssumption() {
      return Universe.getCurrent().getGlobalSemanticsActivatedAssumption();
    }
  }

  public abstract static class MateSemanticsBaselevelNodeUnopt extends MateAbstractSemanticsLevelNode {
    ReflectiveOp reflectiveOperation;

    public MateSemanticsBaselevelNodeUnopt(final ReflectiveOp reflectiveOperation) {
      super();
      this.reflectiveOperation = reflectiveOperation;
    }

    @Specialization
    protected DynamicObject executeGeneric(final VirtualFrame frame,
        final Object[] arguments) {
      if (SArguments.getExecutionLevel(frame) == ExecutionLevel.Base & arguments[0] instanceof DynamicObject) {
        DynamicObject env = SArguments.getEnvironment(frame);
        DynamicObject method = null;
        if (env != Nil.nilObject) {
           method = SMateEnvironment.methodImplementing(env, reflectiveOperation);
        }
        if (method == null & SReflectiveObject.isSReflectiveObject(((DynamicObject) arguments[0]))) {
          env = SReflectiveObject.getEnvironment(((DynamicObject) arguments[0]));
          if (env != Nil.nilObject) {
            method = SMateEnvironment.methodImplementing(env, reflectiveOperation);
          }
        }
        return method;
      }
      return null;
    }
  }
}
