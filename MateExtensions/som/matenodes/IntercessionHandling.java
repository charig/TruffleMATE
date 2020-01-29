package som.matenodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;

import som.interpreter.SArguments;
import som.interpreter.nodes.ISuperReadNode;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateActivationDispatchNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateCachedDispatchMessageLookupNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateCachedDispatchSuperMessageLookupNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchFieldReadNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchFieldWriteNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchLocalVarReadNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchLocalVarWriteNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchPrimFieldReadNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchPrimFieldWriteNodeGen;
import som.matenodes.MateAbstractReflectiveDispatchFactory.MateDispatchReturnNodeGen;
import som.matenodes.MateAbstractSemanticNodes.MateAbstractSemanticsLevelNode;
import som.matenodes.MateAbstractSemanticNodesFactory.MateSemanticCheckNodeGen;
import som.vm.Universe;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SSymbol;

public abstract class IntercessionHandling extends Node {
  public abstract Object doMateSemantics(VirtualFrame frame,
      Object[] arguments);

  public static IntercessionHandling createForOperation(final ReflectiveOp operation) {
    if (operation == ReflectiveOp.None) {
      return new VoidIntercessionHandling();
    } else {
      return new MateIntercessionHandling(operation);
    }
  }

  public static IntercessionHandling createForMessageLookup(final SSymbol selector) {
    return new MateIntercessionHandling(selector, ReflectiveOp.MessageLookup);
  }

  public static IntercessionHandling createForMethodActivation(final SSymbol selector) {
    return new MateIntercessionHandling(selector, ReflectiveOp.MessageActivation);
  }

  public static IntercessionHandling createForSuperMessageLookup(final SSymbol selector, final ISuperReadNode node) {
    return new MateIntercessionHandling(selector, node);
  }

  public static class VoidIntercessionHandling extends IntercessionHandling {
    @Override
    public Object doMateSemantics(final VirtualFrame frame,
        final Object[] arguments) {
      return null;
    }
  }

  public static class MateIntercessionHandling extends IntercessionHandling {
    @Child MateAbstractSemanticsLevelNode   semanticCheck;
    @Child MateAbstractReflectiveDispatch     reflectiveDispatch;
    private final BranchProfile semanticsRedefined = BranchProfile.create();

    protected MateIntercessionHandling(final ReflectiveOp operation) {
      semanticCheck = MateSemanticCheckNodeGen.create(Universe.emptySource.createUnavailableSection(), operation);
      switch (operation) {
        case LayoutReadField: case ExecutorReadField:
          reflectiveDispatch = MateDispatchFieldReadNodeGen.create();
          break;
        case LayoutPrimReadField:
          reflectiveDispatch = MateDispatchPrimFieldReadNodeGen.create();
          break;
        case LayoutWriteField: case ExecutorWriteField:
          reflectiveDispatch = MateDispatchFieldWriteNodeGen.create();
          break;
        case LayoutPrimWriteField:
          reflectiveDispatch = MateDispatchPrimFieldWriteNodeGen.create();
          break;
        case ExecutorLocalArg: case ExecutorNonLocalArg: case ExecutorLocalSuperArg: case ExecutorNonLocalSuperArg:
          reflectiveDispatch = MateDispatchLocalVarReadNodeGen.create();
          break;
        case ExecutorReadLocal:
          reflectiveDispatch = MateDispatchLocalVarReadNodeGen.create();
          break;
        case ExecutorWriteLocal:
          reflectiveDispatch = MateDispatchLocalVarWriteNodeGen.create();
          break;
        case ExecutorReturn:
          reflectiveDispatch = MateDispatchReturnNodeGen.create();
          break;
        default:
          Universe.errorExit("Unexepected operation");
      }
      this.adoptChildren();
    }

    protected MateIntercessionHandling(final SSymbol selector, final ReflectiveOp operation) {
      semanticCheck = MateSemanticCheckNodeGen.create(Universe.emptySource.createUnavailableSection(), operation);
      if (operation == ReflectiveOp.MessageLookup) {
        reflectiveDispatch = MateCachedDispatchMessageLookupNodeGen.create(selector);
      } else {
        reflectiveDispatch = MateActivationDispatchNodeGen.create(selector);
      }
      this.adoptChildren();
    }

    protected MateIntercessionHandling(final SSymbol selector, final ISuperReadNode node) {
      semanticCheck = MateSemanticCheckNodeGen.create(Universe.emptySource.createUnavailableSection(), ReflectiveOp.MessageLookup);
      reflectiveDispatch = MateCachedDispatchSuperMessageLookupNodeGen.create(selector, node);
      this.adoptChildren();
    }

    @Override
    public Object doMateSemantics(final VirtualFrame frame,
        final Object[] arguments) {
      assert SArguments.getExecutionLevel(frame) == ExecutionLevel.Base;
      DynamicObject method = this.getMateNode().execute(frame, arguments);
      if (method != null) {
        semanticsRedefined.enter();
        return this.getMateDispatch().executeDispatch(frame, method, arguments[0], arguments);
      }
      return null;
    }

    private MateAbstractSemanticsLevelNode getMateNode() {
      return this.semanticCheck;
    }

    private MateAbstractReflectiveDispatch getMateDispatch() {
      return this.reflectiveDispatch;
    }
  }
}
