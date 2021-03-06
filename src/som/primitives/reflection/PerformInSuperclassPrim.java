package som.primitives.reflection;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import som.interpreter.SArguments;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Object", primitive = "perform:inSuperclass:", selector = "perform:inSuperclass:")
public abstract class PerformInSuperclassPrim extends TernaryExpressionNode {
  @Child private IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();

  @Specialization
  public final Object doSAbstractObject(final VirtualFrame frame,
      final Object receiver, final SSymbol selector, final DynamicObject clazz) {
    CompilerAsserts.neverPartOfCompilation("PerformInSuperclassPrim");
    DynamicObject invokable = SClass.lookupInvokable(clazz, selector);
    return call.call(SInvokable.getCallTarget(invokable, SArguments.getExecutionLevel(frame)), new Object[] {SArguments.getEnvironment(frame), SArguments.getExecutionLevel(frame), receiver});
  }
}
