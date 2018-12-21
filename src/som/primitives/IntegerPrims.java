package som.primitives;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;

import bd.primitives.Primitive;
import bd.primitives.Specializer;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.UnaryBasicOperation;
import som.primitives.arithmetic.ArithmeticPrim;
import som.vm.Universe;
import som.vm.constants.Classes;
import som.vmobjects.SArray;
import som.vmobjects.SSymbol;
import tools.dym.Tags.ComplexPrimitiveOperation;
import tools.dym.Tags.OpArithmetic;
import tools.dym.Tags.StringAccess;


public abstract class IntegerPrims {

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "atRandom", selector = "atRandom", receiverType = Long.class)
  public abstract static class RandomPrim extends UnaryBasicOperation {
    @Specialization
    public final long doLong(final long receiver) {
      return (long) (receiver * Math.random());
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == OpArithmetic.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "as32BitSignedValue", selector = "as32BitSignedValue", receiverType = Long.class)
  public abstract static class As32BitSignedValue extends UnaryBasicOperation {
    @Specialization
    public final long doLong(final long receiver) {
      return (int) receiver;
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == OpArithmetic.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "as32BitUnsignedValue", selector = "as32BitUnsignedValue", receiverType = Long.class)
  public abstract static class As32BitUnsignedValue extends UnaryBasicOperation {
    @Specialization
    public final long doLong(final long receiver) {
      return Integer.toUnsignedLong((int) receiver);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == OpArithmetic.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer Class", primitive = "fromString:", selector = "fromString:",
      specializer = FromStringPrim.IsIntegerClass.class)
  public abstract static class FromStringPrim extends BinaryExpressionNode {
    protected static final boolean receiverIsIntegerClass(final DynamicObject receiver) {
      return receiver == Classes.integerClass;
    }

    public static class IsIntegerClass extends Specializer<Universe, ExpressionNode, SSymbol> {
      public IsIntegerClass(final Primitive prim, final NodeFactory<ExpressionNode> fact) {
        super(prim, fact);
      }

      @Override
      public boolean matches(final Object[] args, final ExpressionNode[] argNodess) {
        try {
          return receiverIsIntegerClass((DynamicObject) args[0]);
        } catch (ClassCastException e) {
          return false;
        }
      }
    }

    @Specialization(guards = "receiverIsIntegerClass(receiver)")
    public final Object doSClass(final DynamicObject receiver, final String argument) {
      return Long.parseLong(argument);
    }

    @Specialization(guards = "receiverIsIntegerClass(receiver)")
    public final Object doSClass(final DynamicObject receiver, final SSymbol argument) {
      return Long.parseLong(argument.getString());
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == ComplexPrimitiveOperation.class) {
        return true;
      } else if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "<<", selector = "<<", receiverType = Long.class)
  public abstract static class LeftShiftPrim extends ArithmeticPrim {
    private final BranchProfile overflow = BranchProfile.create();

    @Specialization(rewriteOn = ArithmeticException.class)
    public final long doLong(final long receiver, final long right) {
      assert right >= 0;  // currently not defined for negative values of right

      if (Long.SIZE - Long.numberOfLeadingZeros(receiver) + right > Long.SIZE - 1) {
        overflow.enter();
        throw new ArithmeticException("shift overflows long");
      }
      return receiver << right;
    }

    @Specialization
    public final BigInteger doLongWithOverflow(final long receiver, final long right) {
      assert right >= 0;  // currently not defined for negative values of right
      assert right <= Integer.MAX_VALUE;

      return BigInteger.valueOf(receiver).shiftLeft((int) right);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = ">>>", selector = ">>>", receiverType = Long.class)
  public abstract static class UnsignedRightShiftPrim extends ArithmeticPrim {
    @Specialization
    public final long doLong(final long receiver, final long right) {
      return receiver >>> right;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "max:", selector = "max:",
             receiverType = Long.class, disabled = true)
  public abstract static class MaxIntPrim extends ArithmeticPrim {
    @Specialization
    public final long doLong(final long receiver, final long right) {
      return Math.max(receiver, right);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "to:", selector = "to:",
             receiverType = Long.class, disabled = true)
  public abstract static class ToPrim extends BinaryExpressionNode {
    @Specialization
    public final SArray doLong(final long receiver, final long right) {
      int cnt = (int) right - (int) receiver + 1;
      long[] arr = new long[cnt];
      for (int i = 0; i < cnt; i++) {
        arr[i] = i + receiver;
      }
      return SArray.create(arr);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == OpArithmetic.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Integer", primitive = "abs", selector = "abs", receiverType = Long.class)
  public abstract static class AbsPrim extends UnaryBasicOperation {
    @Specialization
    public final long doLong(final long receiver) {
      return Math.abs(receiver);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == OpArithmetic.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }
}
