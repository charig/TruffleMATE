package som.primitives;

import java.lang.reflect.Array;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.ValueProfile;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.UnaryBasicOperation;
import som.vmobjects.SArray;
import som.vmobjects.SArray.ArrayType;
import som.vmobjects.SSymbol;
import tools.dym.Tags.OpLength;

@GenerateNodeFactory
@ImportStatic(ArrayType.class)
@Primitive(className = "String", primitive = "length", selector = "length",
           receiverType = {String.class, Array.class})
@Primitive(className = "Array", primitive = "length")
public abstract class LengthPrim extends UnaryBasicOperation {
  private final ValueProfile storageType = ValueProfile.createClassProfile();

  @Specialization(guards = "isEmptyType(receiver)")
  public final long doEmptySArray(final SArray receiver) {
    return receiver.getEmptyStorage(storageType);
  }

  @Specialization(guards = "isPartiallyEmptyType(receiver)")
  public final long doPartialEmptySArray(final SArray receiver) {
    return receiver.getPartiallyEmptyStorage(storageType).getLength();
  }

  @Specialization(guards = "isObjectType(receiver)")
  public final long doObjectSArray(final SArray receiver) {
    return receiver.getObjectStorage(storageType).length;
  }

  @Specialization(guards = "isLongType(receiver)")
  public final long doLongSArray(final SArray receiver) {
    return receiver.getLongStorage(storageType).length;
  }

  @Specialization(guards = "isDoubleType(receiver)")
  public final long doDoubleSArray(final SArray receiver) {
    return receiver.getDoubleStorage(storageType).length;
  }

  @Specialization(guards = "isBooleanType(receiver)")
  public final long doBooleanSArray(final SArray receiver) {
    return receiver.getBooleanStorage(storageType).length;
  }

  @Specialization(guards = "isByteType(receiver)")
  public final long doByteSArray(final SArray receiver) {
    return receiver.getByteStorage(storageType).length;
  }

  @Specialization(guards = "isCharType(receiver)")
  public final long doCharSArray(final SArray receiver) {
    return receiver.getCharStorage(storageType).length;
  }

  public abstract long executeEvaluated(SArray receiver);

  @Specialization
  public final long doString(final String receiver) {
    return receiver.length();
  }

  @Specialization
  public final long doSSymbol(final SSymbol receiver) {
    return receiver.getString().length();
  }

  @Override
  protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
    if (tag == OpLength.class) {
      return true;
    } else {
      return super.hasTagIgnoringEagerness(tag);
    }
  }
}
