package som.primitives.reflection;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
@Primitive(className = "Object", primitive = "perform:", selector = "perform:")
public abstract class PerformPrim extends BinaryExpressionNode {
  @Child protected AbstractSymbolDispatch dispatch;

  @Override
  @SuppressWarnings("unchecked")
  public PerformPrim initialize(final SourceSection sourceSection) {
    assert sourceSection != null;
    super.initialize(sourceSection);
    dispatch = AbstractSymbolDispatchNodeGen.create(sourceSection);
    return this;
  }

  @Specialization
  public final Object doObject(final VirtualFrame frame, final Object receiver, final SSymbol selector) {
    return dispatch.
        executeDispatch(frame, receiver, selector, null);
  }
}
