package som.primitives;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import som.interpreter.SomException;
import som.interpreter.nodes.nary.UnaryExpressionNode;


public abstract class ExceptionsPrims {
  @GenerateNodeFactory
  @Primitive(className = "Exception", primitive = "signal")
  public abstract static class SignalPrim extends UnaryExpressionNode {
    @Specialization
    public final Object doSignal(final DynamicObject exceptionObject) {
      throw new SomException(exceptionObject);
    }
  }
}
