package som.interpreter.nodes;

import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.interpreter.nodes.dispatch.UninitializedDispatchNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.EagerBinaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerQuaternaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerTernaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerUnaryPrimitiveNode;
import som.interpreter.nodes.nary.QuaternaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SSymbol;


public abstract class AbstractMessageSpecializationsFactory {
  public abstract EagerUnaryPrimitiveNode unaryPrimitiveFor(SSymbol selector, ExpressionNode receiver, UnaryExpressionNode primitive);
  public abstract EagerBinaryPrimitiveNode binaryPrimitiveFor(SSymbol selector, ExpressionNode receiver, ExpressionNode argument, BinaryExpressionNode primitive);
  public abstract EagerTernaryPrimitiveNode ternaryPrimitiveFor(SSymbol selector, ExpressionNode receiver, ExpressionNode argument, ExpressionNode argument2, TernaryExpressionNode primitive);
  public abstract EagerQuaternaryPrimitiveNode quaternaryPrimitiveFor(SSymbol selector, ExpressionNode receiver, ExpressionNode argument, ExpressionNode argument2, ExpressionNode argument3, QuaternaryExpressionNode primitive);
  public abstract GenericMessageSendNode genericMessageFor(SSymbol selector, ExpressionNode[] argumentNodes, SourceSection source);

  public static class SOMMessageSpecializationsFactory extends AbstractMessageSpecializationsFactory {
    @Override
    public EagerUnaryPrimitiveNode unaryPrimitiveFor(final SSymbol selector,
        final ExpressionNode receiver, final UnaryExpressionNode primitive) {
      return new EagerUnaryPrimitiveNode(selector, receiver, primitive);
    }

    @Override
    public EagerBinaryPrimitiveNode binaryPrimitiveFor(final SSymbol selector,
        final ExpressionNode receiver, final ExpressionNode argument,
        final BinaryExpressionNode primitive) {
      return new EagerBinaryPrimitiveNode(selector, receiver, argument, primitive);
    }

    @Override
    public EagerTernaryPrimitiveNode ternaryPrimitiveFor(final SSymbol selector,
        final ExpressionNode receiver, final ExpressionNode argument,
        final ExpressionNode argument2, final TernaryExpressionNode primitive) {
      return new EagerTernaryPrimitiveNode(selector, receiver, argument, argument2, primitive);
    }

    @Override
    public EagerQuaternaryPrimitiveNode quaternaryPrimitiveFor(final SSymbol selector,
        final ExpressionNode receiver, final ExpressionNode argument,
        final ExpressionNode argument2, final ExpressionNode argument3, final QuaternaryExpressionNode primitive) {
      return new EagerQuaternaryPrimitiveNode(selector, receiver, argument, argument2, argument3, primitive);
    }

    @Override
    public GenericMessageSendNode genericMessageFor(final SSymbol selector,
        final ExpressionNode[] argumentNodes, final SourceSection source) {
      return new GenericMessageSendNode(selector, argumentNodes,
          new UninitializedDispatchNode(source, selector)).initialize(source);
    }
  }
}
