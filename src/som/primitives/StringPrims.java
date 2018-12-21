package som.primitives;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.ValueProfile;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryBasicOperation;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SArray;
import som.vmobjects.SSymbol;
import tools.dym.Tags.ComplexPrimitiveOperation;
import tools.dym.Tags.StringAccess;


public class StringPrims {

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "concatenate:", selector = "concatenate:", receiverType = {SSymbol.class, String.class})
  public abstract static class ConcatPrim extends BinaryExpressionNode {
    @Specialization
    public final String doString(final String receiver, final String argument) {
      return receiver + argument;
    }

    @Specialization
    public final String doString(final String receiver, final SSymbol argument) {
      return receiver + argument.getString();
    }

    @Specialization
    public final String doSSymbol(final SSymbol receiver, final String argument) {
      return receiver.getString() + argument;
    }

    @Specialization
    public final String doSSymbol(final SSymbol receiver, final SSymbol argument) {
      return receiver.getString() + argument.getString();
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "join:", selector = "join:", receiverType = {String.class})
  public abstract static class JoinPrim extends BinaryExpressionNode {
    final ValueProfile profile = ValueProfile.createClassProfile();
    @Specialization
    public final String doString(final String receiver, final SArray argument) {
      return String.valueOf(argument.getCharStorage(profile));
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }


  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "asSymbol", selector = "asSymbol")
  public abstract static class AsSymbolPrim extends UnaryBasicOperation {
    private final Universe universe;
    public AsSymbolPrim() {
      this.universe = Universe.getCurrent();
    }

    @Specialization
    public final SAbstractObject doString(final String receiver) {
      return universe.symbolFor(receiver);
    }

    @Specialization
    public final SAbstractObject doSSymbol(final SSymbol receiver) {
      return receiver;
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "primSubstringFrom:to:", selector = "primSubstringFrom:to:", receiverType = {SSymbol.class, String.class})
  public abstract static class SubstringPrim extends TernaryExpressionNode {
    @Specialization
    public final String doString(final String receiver, final long start,
        final long end) {
      try {
        return receiver.substring((int) start - 1, (int) end);
      } catch (IndexOutOfBoundsException e) {
        return "Error - index out of bounds";
      }
    }

    @Specialization
    public final String doSSymbol(final SSymbol receiver, final long start,
        final long end) {
      return doString(receiver.getString(), start, end);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else if (tag == ComplexPrimitiveOperation.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "at:", receiverType = String.class)
  /*
   * It is not specializable for avoiding the clash with Array at: primitive.
   * We should improve the specialization so that it enables to store different
   * specializers for the same selector.
   */
  public abstract static class AtStringPrim extends BinaryExpressionNode {
    @Specialization
    public final char doString(final String receiver, final long index) {
      if (index > receiver.length()) {
        Universe.errorExit("Accessing string:" + receiver + "out of range: " + String.valueOf(index));
      }
      return receiver.charAt((int) index - 1);
    }

    @Specialization
    public final char doSymbol(final SSymbol receiver, final long index) {
      return doString(receiver.getString(), index);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "String", primitive = "asNumber", selector = "asNumber", receiverType = String.class)
  public abstract static class AsNumberStringPrim extends UnaryExpressionNode {
    @Specialization
    public final Number doString(final String receiver) {
      try {
        return Long.parseLong(receiver);
      } catch (NumberFormatException e) {
        return Double.parseDouble(receiver);
      }
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }
}
