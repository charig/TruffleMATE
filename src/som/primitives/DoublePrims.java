package som.primitives;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import bd.primitives.Specializer;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.UnaryBasicOperation;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vm.constants.Classes;
import som.vmobjects.SSymbol;
import tools.debugger.Tags.LiteralTag;
import tools.dym.Tags.OpArithmetic;


public abstract class DoublePrims  {

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "round", selector = "round", receiverType = Double.class)
  public abstract static class RoundPrim extends UnaryBasicOperation {
    @Specialization
    public final long doDouble(final double receiver) {
      return Math.round(receiver);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == OpArithmetic.class) { // TODO: is this good enough?
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "asInteger", selector = "asInteger", receiverType = Double.class)
  public abstract static class AsIntegerPrim extends UnaryBasicOperation {
    @Specialization
    public final long doDouble(final double receiver) {
      return (long) receiver;
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == OpArithmetic.class) { // TODO: is this good enough?
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  public static class IsDoubleClass extends Specializer<Universe, ExpressionNode, SSymbol> {
    public IsDoubleClass(final Primitive prim, final NodeFactory<ExpressionNode> fact) {
      super(prim, fact);
    }

    @Override
    public boolean matches(final Object[] args, final ExpressionNode[] argNodess) {
      return args[0] == Classes.doubleClass;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Double Class", primitive = "PositiveInfinity", selector = "PositiveInfinity",
             noWrapper = true, specializer = IsDoubleClass.class)
  public abstract static class PositiveInfinityPrim extends UnaryExpressionNode {
    protected final boolean receiverIsDoubleClass(final DynamicObject receiver) {
      return receiver == Classes.doubleClass;
    }

    @Specialization(guards = "receiverIsDoubleClass(receiver)")
    public final double doSClass(final DynamicObject receiver) {
      return Double.POSITIVE_INFINITY;
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == LiteralTag.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Double", primitive = "floor", selector = "floor", receiverType = Double.class)
  public abstract static class FloorPrim extends UnaryBasicOperation {
    @Specialization
    public final long doDouble(final double receiver) {
      return (long) Math.floor(receiver);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == OpArithmetic.class) { // TODO: is this good enough?
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }
}
