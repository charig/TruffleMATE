package som.primitives.arrays;

import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vm.constants.Classes;
import som.vmobjects.SArray;
import som.vmobjects.SClass;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;


@GenerateNodeFactory
public abstract class NewPrim extends BinaryExpressionNode {

  protected static final boolean receiverIsArrayClass(final DynamicObject receiver) {
    return receiver == Classes.arrayClass;
  }
  
  
  protected static final boolean receiverIsByteArrayClass(final DynamicObject receiver) {
    return (SClass.getName(receiver).getString() == "ByteArray");
  }
  
  @Specialization(guards = "receiverIsArrayClass(receiver)")
  public final SArray doSClass(final DynamicObject receiver, final long length) {
    return new SArray(length);
  }
  
  @Specialization(guards = "receiverIsByteArrayClass(receiver)")
  public final SArray doByteSClass(final DynamicObject receiver, final long length) {
    return SArray.create(new byte[(int) length]);
  }
}
