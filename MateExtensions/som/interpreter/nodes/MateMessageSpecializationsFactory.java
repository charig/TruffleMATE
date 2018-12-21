package som.interpreter.nodes;

import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.AbstractMessageSpecializationsFactory.SOMMessageSpecializationsFactory;
import som.interpreter.nodes.MessageSendNode.GenericMessageSendNode;
import som.interpreter.nodes.dispatch.MateUninitializedDispatchNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.EagerBinaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerQuaternaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerTernaryPrimitiveNode;
import som.interpreter.nodes.nary.EagerUnaryPrimitiveNode;
import som.interpreter.nodes.nary.MateEagerBinaryPrimitiveNode;
import som.interpreter.nodes.nary.MateEagerQuaternaryPrimitiveNode;
import som.interpreter.nodes.nary.MateEagerTernaryPrimitiveNode;
import som.interpreter.nodes.nary.MateEagerUnaryPrimitiveNode;
import som.interpreter.nodes.nary.QuaternaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SSymbol;


public class MateMessageSpecializationsFactory extends
    SOMMessageSpecializationsFactory {
  @Override
  public EagerUnaryPrimitiveNode unaryPrimitiveFor(final SSymbol selector,
      final ExpressionNode receiver, final UnaryExpressionNode primitive) {
    return new MateEagerUnaryPrimitiveNode(selector, receiver, primitive);
  }

  @Override
  public EagerBinaryPrimitiveNode binaryPrimitiveFor(final SSymbol selector,
      final ExpressionNode receiver, final ExpressionNode argument,
      final BinaryExpressionNode primitive) {
    return new MateEagerBinaryPrimitiveNode(selector, receiver, argument, primitive);
  }

  @Override
  public EagerTernaryPrimitiveNode ternaryPrimitiveFor(final SSymbol selector,
      final ExpressionNode receiver, final ExpressionNode argument,
      final ExpressionNode argument2, final TernaryExpressionNode primitive) {
    return new MateEagerTernaryPrimitiveNode(selector, receiver, argument, argument2, primitive);
  }

  @Override
  public EagerQuaternaryPrimitiveNode quaternaryPrimitiveFor(final SSymbol selector,
      final ExpressionNode receiver, final ExpressionNode argument,
      final ExpressionNode argument2, final ExpressionNode argument3,
      final QuaternaryExpressionNode primitive) {
    return new MateEagerQuaternaryPrimitiveNode(selector, receiver, argument, argument2, argument3, primitive);
  }

  @Override
  public GenericMessageSendNode genericMessageFor(final SSymbol selector,
      final ExpressionNode[] argumentNodes, final SourceSection source) {
    return new MateGenericMessageSendNode(selector, argumentNodes,
        new MateUninitializedDispatchNode(source, selector)).initialize(source);
  }
}
