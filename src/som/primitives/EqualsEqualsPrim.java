package som.primitives;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ValueProfile;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.Globals;
import som.vmobjects.MockJavaObject;
import som.vmobjects.SArray;
import som.vmobjects.SBlock;
import som.vmobjects.SFile;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Object", primitive = "==", selector = "==")
public abstract class EqualsEqualsPrim extends BinaryExpressionNode {
  @Specialization
  public final boolean doBoolean(final boolean left, final boolean right) {
    return left == right;
  }

  @Specialization
  public final boolean doBoolean(final boolean left, final DynamicObject right) {
    return (left && Globals.trueObject  == right) ||
          (!left && Globals.falseObject == right);
  }

  @Specialization
  public final boolean doLong(final long left, final long right) {
    return left == right;
  }

  @Specialization
  public final boolean doBigInteger(final BigInteger left, final BigInteger right) {
    return left.equals(right);
  }

  @Specialization
  public final boolean doString(final String left, final String right) {
    return left == right;
  }

  @Specialization
  public final boolean doString(final String receiver, final char argument) {
    return false;
  }

  @Specialization
  public final boolean doDouble(final double left, final double right) {
    return left == right;
  }

  @Specialization
  public final boolean doSBlock(final SBlock left, final Object right) {
    return left == right;
  }

  @Specialization
  public final boolean doArray(final SArray left, final Object right) {
    return left == right;
  }

  @Specialization
  public final boolean doSMethod(final SInvokable left, final Object right) {
    return left == right;
  }

  @Specialization
  public final boolean doSSymbol(final SSymbol left, final Object right) {
    return left == right;
  }

  @Specialization
  public final boolean doSObject(final DynamicObject left, final Object right) {
    return left == right;
  }

  @Specialization
  public final boolean doLong(final long left, final double right) {
    return false;
  }

  @Specialization
  public final boolean doBigInteger(final BigInteger left, final long right) {
    return false;
  }

  @Specialization
  public final boolean doLong(final long left, final BigInteger right) {
    return false;
  }

  @Specialization
  public final boolean doDouble(final double left, final long right) {
    return false;
  }

  @Specialization
  public final boolean doLong(final long left, final String right) {
    return false;
  }

  @Specialization
  public final boolean doLong(final long left, final DynamicObject right) {
    return false;
  }

  @Specialization
  public final boolean doString(final String receiver, final long argument) {
    return false;
  }

  @Specialization
  public final boolean doString(final String receiver, final DynamicObject argument) {
    return false;
  }

  @Specialization
  public final boolean doCharacter(final char receiver, final char argument) {
    return Character.compare(receiver, argument) == 0;
  }

  @Specialization
  public final boolean doCharacter(final char receiver, final DynamicObject argument) {
    return false;
  }

  @Specialization
  public final boolean doCharacter(final char receiver, final String argument) {
    return false;
  }

  @Specialization
  public final boolean doMock(final MockJavaObject receiver,
      final MockJavaObject argument,
      @Cached("createProfile()") final ValueProfile rcvrProfile,
      @Cached("createProfile()") final ValueProfile argProfile) {

    Object rcvrMock = rcvrProfile.profile(receiver.getMockedObject());
    Object argMock = argProfile.profile(argument.getMockedObject());

    return rcvrMock.equals(argMock) && receiver.getSOMClass().equals(argument.getSOMClass());
  }

  @Specialization
  public final boolean doSFile(final SFile file, final Object argument) {
    return file == argument;
  }

  protected static ValueProfile createProfile() {
    return ValueProfile.createClassProfile();
  }
}
