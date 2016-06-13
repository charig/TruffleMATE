package som.interpreter;

import som.compiler.MethodGenerationContext;
import som.compiler.Variable.Local;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.MateReturnNode;
import som.vm.MateUniverse;
import som.vm.Universe;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.ExecutionContext;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

public abstract class Invokable extends RootNode implements MateNode{

  @Child protected ExpressionNode expressionOrSequence;
  
  protected final ExpressionNode uninitializedBody;
  @CompilationFinal protected DynamicObject belongsToMethod;

  public Invokable(final SourceSection sourceSection,
      final FrameDescriptor frameDescriptor,
      final ExpressionNode expressionOrSequence,
      final ExpressionNode uninitialized,
      DynamicObject method) {
    super(SomLanguage.class, sourceSection, frameDescriptor);
    this.uninitializedBody = this.mateifyUninitializedNode(uninitialized);
    this.expressionOrSequence = expressionOrSequence;
    this.belongsToMethod = method;
  }

  @Override
  public final Object execute(final VirtualFrame frame) {
    return expressionOrSequence.executeGeneric(frame);
  }

  public abstract Invokable cloneWithNewLexicalContext(final LexicalScope outerContext);
  public ExpressionNode inline(final MethodGenerationContext mgenc,
      final Local[] locals) {
    return InlinerForLexicallyEmbeddedMethods.doInline(uninitializedBody, mgenc,
        locals, getSourceSection().getCharIndex());
  }

  @Override
  public final boolean isCloningAllowed() {
    return true;
  }
  
  @Override
  public ExecutionContext getExecutionContext() {
    return Universe.current();
  }
  
  public DynamicObject getBelongsToMethod() {
    return this.belongsToMethod; 
  }
  
  public final RootCallTarget createCallTarget() {
    return Truffle.getRuntime().createCallTarget(this);
  }

  public abstract void propagateLoopCountThroughoutLexicalScope(final long count);
  
  public void wrapIntoMateNode() {
    if (this.asMateNode() != null) this.replace(this.asMateNode());
    this.expressionOrSequence.replace(new MateReturnNode(this.expressionOrSequence));
    uninitializedBody.accept(new MateifyVisitor());
  }
  
  private ExpressionNode mateifyUninitializedNode(ExpressionNode uninitialized){
    if (!(Universe.current() instanceof MateUniverse) || uninitialized.asMateNode() == null) {
        return uninitialized;
    } else {
      return (ExpressionNode)uninitialized.asMateNode();
    }
  }
  

  public void setMethod(DynamicObject method) {
    this.belongsToMethod = method;
  }
}
