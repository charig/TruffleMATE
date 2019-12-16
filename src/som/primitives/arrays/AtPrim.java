package som.primitives.arrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.ValueProfile;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.Nil;
import som.vmobjects.SArray;
import som.vmobjects.SArray.ArrayType;
import tools.dym.Tags.ArrayRead;
import tools.dym.Tags.BasicPrimitiveOperation;

@GenerateNodeFactory
@Primitive(className = "Array", primitive = "at:", selector = "at:", receiverType = SArray.class)
@ImportStatic(ArrayType.class)
public abstract class AtPrim extends BinaryExpressionNode {
  private final ValueProfile storageType = ValueProfile.createClassProfile();

  @Specialization(guards = "isEmptyType(receiver)")
  public final Object doEmptySArray(final SArray receiver, final long idx) {
    assert idx > 0;
    assert idx <= receiver.getEmptyStorage(storageType);
    return Nil.nilObject;
  }

  @ReportPolymorphism.Exclude
  @Specialization(guards = "isPartiallyEmptyType(receiver)")
  public final Object doPartiallyEmptySArray(final SArray receiver, final long idx) {
    return receiver.getPartiallyEmptyStorage(storageType).get(idx - 1);
  }

  @ReportPolymorphism.Exclude
  @Specialization(guards = "isObjectType(receiver)")
  public final Object doObjectSArray(final SArray receiver, final long idx) {
    return receiver.getObjectStorage(storageType)[(int) idx - 1];
  }

  @Specialization(guards = "isLongType(receiver)")
  public final long doLongSArray(final SArray receiver, final long idx) {
    return receiver.getLongStorage(storageType)[(int) idx - 1];
  }

  @Specialization(guards = "isByteType(receiver)")
  public final long doByteSArray(final SArray receiver, final long idx) {
    return receiver.getByteStorage(storageType)[(int) idx - 1];
  }

  @Specialization(guards = "isDoubleType(receiver)")
  public final double doDoubleSArray(final SArray receiver, final long idx) {
    return receiver.getDoubleStorage(storageType)[(int) idx - 1];
  }

  @Specialization(guards = "isBooleanType(receiver)")
  public final boolean doBooleanSArray(final SArray receiver, final long idx) {
    return receiver.getBooleanStorage(storageType)[(int) idx - 1];
  }

  @Specialization(guards = "isCharType(receiver)")
  public final char doCharSArray(final SArray receiver, final long idx) {
    return receiver.getCharStorage(storageType)[(int) idx - 1];
  }

  @Override
  protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
    if (tag == BasicPrimitiveOperation.class) {
      return true;
    } else if (tag == ArrayRead.class) {
      return true;
    } else {
      return super.hasTagIgnoringEagerness(tag);
    }
  }
}
