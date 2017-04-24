package som.primitives;

import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.constants.MateClasses;
import som.vmobjects.MockJavaObject;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SInvokable;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;


public abstract class MethodPrims {

  @GenerateNodeFactory
  @Primitive(klass = "Method", selector = "signature")
  @Primitive(klass = "Primitive", selector = "signature", eagerSpecializable = false)
  public abstract static class SignaturePrim extends UnaryExpressionNode {
    public SignaturePrim(final boolean eagWrap, final SourceSection source) {
      super(eagWrap, source);
    }

    @Specialization
    public final SAbstractObject doSMethod(final DynamicObject receiver) {
      return SInvokable.getSignature(receiver);
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "Method", selector = "holder")
  @Primitive(klass = "Primitive", selector = "holder", eagerSpecializable = false)
  public abstract static class HolderPrim extends UnaryExpressionNode {
    public HolderPrim(final boolean eagWrap, final SourceSection source) {
      super(eagWrap, source);
    }

    @Specialization
    public final DynamicObject doSMethod(final DynamicObject receiver) {
      return SInvokable.getHolder(receiver);
    }
  }
  
  @GenerateNodeFactory
  @Primitive(klass = "Method", selector = "compilation")
  public abstract static class CompilationPrim extends UnaryExpressionNode {
    public CompilationPrim(final boolean eagWrap, final SourceSection source) {
      super(eagWrap, source);
    }

    @TruffleBoundary
    @Specialization
    public final MockJavaObject doSMethod(final DynamicObject receiver) {
      // TODO: Analyze if it is also interesting to experiment with the MetaLevel
      return new MockJavaObject(SInvokable.getInvokable(receiver), MateClasses.astNodeClass);
    }
  }
}
