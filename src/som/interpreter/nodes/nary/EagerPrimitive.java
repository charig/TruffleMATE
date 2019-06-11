package som.interpreter.nodes.nary;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;

import som.interpreter.nodes.AbstractMessageSpecializationsFactory;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.MessageSendNode;
import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.interpreter.nodes.OperationNode;
import som.interpreter.nodes.PreevaluatedExpression;
import som.vm.Universe;
import som.vm.constants.ExecutionLevel;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SSymbol;


public abstract class EagerPrimitive extends ExpressionNode
    implements OperationNode, ExpressionWithReceiver, PreevaluatedExpression {
  protected final SSymbol selector;

  protected EagerPrimitive(final SSymbol selector) {
    this.selector = selector;
  }

  @Override
  public Object executeGeneric(final VirtualFrame frame) {
    return executeGenericWithReceiver(frame, this.getReceiver().executeGeneric(frame));
  }

  protected SSymbol getSelector() {
    return selector;
  }

   @Override
  public String getOperation() {
    return selector.getString();
  }

  @Override
  public ReflectiveOp reflectiveOperation() {
    return ReflectiveOp.MessageLookup;
  }

  protected AbstractMessageSpecializationsFactory getFactory() {
    return Universe.getCurrent().somSpecializationFactory;
  }

  protected final GenericMessageSendNode replaceWithGenericSend(final ExecutionLevel level) {
    ExpressionNode[] arguments = this.getArgumentNodes();
    GenericMessageSendNode node = MessageSendNode.createGeneric(selector,
        arguments, getSourceSection(), level, this.getFactory());
    replace(node);
    return node;
  }

  protected abstract ExpressionNode[] getArgumentNodes();

  protected void setTags(final byte tagMark) {
    ((ExpressionWithTagsNode) this.getPrimitive()).tagWith(tagMark);
  }

  @Override
  public void markAsRootExpression() {
    ((ExpressionWithTagsNode) this.getPrimitive()).markAsRootExpression();
  }

  @Override
  public boolean isMarkedAsRootExpression() {
    return ((ExpressionWithTagsNode) this.getPrimitive()).isMarkedAsRootExpression();
  }

  @Override
  public void markAsLoopBody() {
    ((ExpressionWithTagsNode) this.getPrimitive()).markAsLoopBody();
  }

  @Override
  public void markAsControlFlowCondition() {
    ((ExpressionWithTagsNode) this.getPrimitive()).markAsControlFlowCondition();
  }

  @Override
  public void markAsPrimitiveArgument() {
    ((ExpressionWithTagsNode) this.getPrimitive()).markAsPrimitiveArgument();
  }

  @Override
  public void markAsVirtualInvokeReceiver() {
    ((ExpressionWithTagsNode) this.getPrimitive()).markAsVirtualInvokeReceiver();
  }

  protected abstract ExpressionNode getPrimitive();

  @Override
  public boolean hasTag(final Class<? extends Tag> tag) {
    assert !(this.getPrimitive() instanceof WrapperNode);
    return ((EagerlySpecializableNode) this.getPrimitive()).hasTagIgnoringEagerness(tag);
  }

}
