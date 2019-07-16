package som.primitives.arrays;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.ValueProfile;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SArray;
import som.vmobjects.SArray.ArrayType;
import som.vmobjects.SArray.PartiallyEmptyArray;
import tools.dym.Tags.ArrayRead;
import tools.dym.Tags.BasicPrimitiveOperation;

@GenerateNodeFactory
@Primitive(className = "Array", primitive = "asString", selector = "asString", receiverType = SArray.class)
@ImportStatic(ArrayType.class)
public abstract class AsStringPrim extends UnaryExpressionNode {
  private final ValueProfile storageType = ValueProfile.createClassProfile();

  @Specialization(guards = "isEmptyType(receiver)")
  public final String doEmptySArray(final SArray receiver) {
    return "";
  }

  @Specialization(guards = "isPartiallyEmptyType(receiver)")
  public final String doPartiallyEmptySArray(final SArray receiver) {
    String str = "";

    PartiallyEmptyArray storage = receiver.getPartiallyEmptyStorage(storageType);
    for (int i = 0; i < storage.getLength(); i++) {
      str += storage.get(i).toString();
    }
    return  str;
  }

  @Specialization(guards = "isCharType(receiver)")
  public final String doCharSArray(final SArray receiver) {
    return String.valueOf(receiver.getCharStorage(storageType));
  }

  @Specialization(guards = "isObjectType(receiver)")
  public final String doObject(final SArray receiver) {
    String str = "";

    for (Object elem : receiver.getObjectStorage(storageType)) {
      str += elem.toString();
    }
    return  str;
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
