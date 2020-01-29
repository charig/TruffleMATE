package som.interpreter.nodes;

import som.interpreter.nodes.MessageSendNode.UninitializedMessageSendNode;
import som.vm.Universe;

public class MateUninitializedMessageSendNode extends
    UninitializedMessageSendNode {

  public MateUninitializedMessageSendNode(final UninitializedMessageSendNode somNode) {
    super(somNode.getSelector(), somNode.argumentNodes);
    this.adoptChildren();
    initialize(somNode.getSourceSection());
  }

  @Override
  public ExpressionNode asMateNode() {
    return null;
  }

  @Override
  protected AbstractMessageSpecializationsFactory getFactory() {
    return Universe.getCurrent().mateSpecializationFactory;
  }
}
