package som.primitives;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.constants.MateClasses;
import som.vmobjects.MockJavaObject;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SInvokable;


public abstract class MethodPrims {

  @GenerateNodeFactory
  @Primitive(className = "Method", primitive = "signature", selector = "signature")
  @Primitive(className = "Primitive", primitive = "signature")
  public abstract static class SignaturePrim extends UnaryExpressionNode {
    @Specialization
    public final SAbstractObject doSMethod(final DynamicObject receiver) {
      return SInvokable.getSignature(receiver);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Method", primitive = "holder", selector = "holder")
  @Primitive(className = "Primitive", primitive = "holder")
  public abstract static class HolderPrim extends UnaryExpressionNode {
    @Specialization
    public final DynamicObject doSMethod(final DynamicObject receiver) {
      return SInvokable.getHolder(receiver);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Method", primitive = "compilation", selector = "compilation")
  public abstract static class CompilationPrim extends UnaryExpressionNode {
    @TruffleBoundary
    @Specialization
    public final MockJavaObject doSMethod(final DynamicObject receiver) {
      // TODO: Analyze if it is also interesting to experiment with the MetaLevel
      return new MockJavaObject(SInvokable.getInvokable(receiver), MateClasses.astNodeClass);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Method", primitive = "sourceCode")
  public abstract static class SourceCodePrim extends UnaryExpressionNode {
    @Specialization
    public final String doSMethod(final DynamicObject receiver) {
      return SInvokable.getInvokable(receiver).getSourceSection().getCharacters().toString();
    }
  }
}
