package som.primitives;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import som.interpreter.SomLanguage;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryBasicOperation;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.Globals;
import som.vm.constants.Nil;
import som.vmobjects.SBlock;
import som.vmobjects.SClass;
import som.vmobjects.SSymbol;


public final class SystemPrims {
  public static boolean receiverIsSystemObject(final DynamicObject receiver) {
    return receiver == Globals.systemObject;
  }

  @GenerateNodeFactory
  public abstract static class BinarySystemNode extends BinaryExpressionNode {
    protected final Universe universe;
    protected BinarySystemNode() {
      this.universe = Universe.getCurrent();
    }
  }

  @ImportStatic(SystemPrims.class)
  @Primitive(className = "System", primitive = "load:")
  public abstract static class LoadPrim extends BinarySystemNode {
    @Specialization(guards = "receiverIsSystemObject(receiver)")
    public final Object doSObject(final DynamicObject receiver, final SSymbol argument,
        @Cached("currentUniverse()") final Universe currentUniverse) {
      DynamicObject result = currentUniverse.loadClass(argument);
      return result != null ? result : Nil.nilObject;
    }

    public static Universe currentUniverse() {
      return Universe.getCurrent();
    }
  }

  @ImportStatic(SystemPrims.class)
  @Primitive(className = "System", primitive = "exit:")
  public abstract static class ExitPrim extends BinarySystemNode {
    @Specialization(guards = "receiverIsSystemObject(receiver)")
    public final Object doSObject(final DynamicObject receiver, final long error) {
      universe.exit((int) error);
      return receiver;
    }
  }

  @ImportStatic(SystemPrims.class)
  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "global:put:")
  public abstract static class GlobalPutPrim extends TernaryExpressionNode {
    private final Universe universe;
    public GlobalPutPrim() {
      this.universe = Universe.getCurrent();
    }

    @Specialization(guards = "receiverIsSystemObject(receiver)")
    public final Object doSObject(final DynamicObject receiver, final SSymbol global,
        final DynamicObject value) {
      universe.setGlobal(global, value);
      return value;
    }
  }

  @ImportStatic(SystemPrims.class)
  @Primitive(className = "System", primitive = "printString:")
  public abstract static class PrintStringPrim extends BinarySystemNode {
    @Specialization(guards = "receiverIsSystemObject(receiver)")
    public final Object doSObject(final DynamicObject receiver, final String argument) {
      Universe.print(argument);
      return receiver;
    }

    @Specialization(guards = "receiverIsSystemObject(receiver)")
    public final Object doSObject(final DynamicObject receiver, final SSymbol argument) {
      return doSObject(receiver, argument.getString());
    }
  }

  @ImportStatic(SystemPrims.class)
  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "printNewline")
  public abstract static class PrintNewlinePrim extends UnaryExpressionNode {
    @Specialization(guards = "receiverIsSystemObject(receiver)")
    public final Object doSObject(final DynamicObject receiver) {
      Universe.println("");
      return receiver;
    }
  }

  @ImportStatic(SystemPrims.class)
  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "fullGC")
  public abstract static class FullGCPrim extends UnaryExpressionNode {
    @Specialization(guards = "receiverIsSystemObject(receiver)")
    public final Object doSObject(final DynamicObject receiver) {
      System.gc();
      return true;
    }
  }

  @ImportStatic(SystemPrims.class)
  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "time")
  public abstract static class TimePrim extends UnaryBasicOperation {
    @Specialization(guards = "receiverIsSystemObject(receiver)")
    public final long doSObject(final DynamicObject receiver) {
      return System.currentTimeMillis() - startTime;
    }
  }

  @ImportStatic(SystemPrims.class)
  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "ticks")
  public abstract static class TicksPrim extends UnaryBasicOperation {
    @Specialization(guards = "receiverIsSystemObject(receiver)")
    public final long doSObject(final DynamicObject receiver) {
      return System.nanoTime() / 1000L - startMicroTime;
    }
  }

  @ImportStatic(SystemPrims.class)
  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "export:as:")
  public abstract static class ExportAsPrim extends TernaryExpressionNode {

    @Specialization(guards = "receiverIsSystemObject(obj)")
    public final boolean doString(final DynamicObject obj, final SBlock method, final String name) {
      Universe vm = this.getRootNode().getLanguage(SomLanguage.class).getContextReference().get();
      vm.registerExport(name, method);
      return true;
    }

    @Specialization(guards = "receiverIsSystemObject(obj)")
    public final boolean doSymbol(final DynamicObject obj, final SBlock method, final SSymbol name) {
      return doString(obj, method, name.getString());
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System Class", primitive = "current")
  public abstract static class CurrentInstancePrim extends UnaryExpressionNode {
    @Specialization
    public final DynamicObject doSObject(final DynamicObject receiver) {
      assert (SClass.getName(receiver).getString().equals("system"));
      return Universe.getCurrent().getSystemObject();
    }
  }

  {
    startMicroTime = System.nanoTime() / 1000L;
    startTime = startMicroTime / 1000L;
  }
  private static long startTime;
  private static long startMicroTime;

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "inTruffle")
  public abstract static class InTrufflePrim extends UnaryExpressionNode {
    @Specialization
    public final Object doPrim(final DynamicObject receiver) {
      return true;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "System", primitive = "baseExecutionLevel")
  public abstract static class BaseExecutionLevelPrim extends UnaryExpressionNode {
    @Specialization
    public final ExecutionLevel doPrim(final DynamicObject receiver) {
      return ExecutionLevel.Base;
    }
  }

}
