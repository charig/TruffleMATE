package som.primitives;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.UnaryBasicOperation;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Character", primitive = "asString")
@Primitive(className = "Symbol", primitive = "asString")
@Primitive(className = "Integer", primitive = "asString")
@Primitive(className = "Double", primitive = "asString")
public abstract class AsStringPrim extends UnaryBasicOperation {
  @Specialization
  public final String doSSymbol(final SSymbol receiver) {
    return receiver.getString();
  }

  @Specialization
  public final String doCharacter(final char receiver) {
    return Character.toString(receiver);
  }

  @TruffleBoundary
  @Specialization
  public final String doLong(final long receiver) {
    return Long.toString(receiver);
  }

  @TruffleBoundary
  @Specialization
  public final String doDouble(final double receiver) {
    return Double.toString(receiver);
  }

  @TruffleBoundary
  @Specialization
  public final String doBigInteger(final BigInteger receiver) {
    return receiver.toString();
  }
}
