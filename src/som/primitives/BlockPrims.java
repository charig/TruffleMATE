package som.primitives;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import som.VmSettings;
import som.interpreter.SArguments;
import som.interpreter.SomException;
import som.interpreter.nodes.dispatch.BlockDispatchNode;
import som.interpreter.nodes.dispatch.BlockDispatchNodeGen;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.QuaternaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SBlock;
import som.vmobjects.SClass;
import som.vmobjects.SInvokable;
import som.vmobjects.SObject;
import tools.dym.Tags.OpClosureApplication;


public abstract class BlockPrims {

  @GenerateNodeFactory
  @Primitive(klass = "Block", selector = "restart", eagerSpecializable = false)
  public abstract static class RestartPrim extends UnaryExpressionNode {
    public RestartPrim(final boolean eagWrap, final SourceSection source) {
      super(eagWrap, source);
    }

    @Specialization
    public SAbstractObject doSBlock(final SBlock receiver) {
      CompilerDirectives.transferToInterpreter();
      // TruffleSOM intrinsifies #whileTrue: and #whileFalse:
      throw new RuntimeException("This primitive is not supported anymore! "
          + "Something went wrong with the intrinsification of "
          + "#whileTrue:/#whileFalse:?");
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "Block1", selector = "value",
             receiverType = {SBlock.class, Boolean.class})
  public abstract static class ValueNonePrim extends UnaryExpressionNode {
    @Child private BlockDispatchNode dispatchNode = BlockDispatchNodeGen.create();

    public ValueNonePrim(final boolean eagWrap) {
      super(eagWrap, Universe.emptySource.createUnavailableSection());
    }

    public ValueNonePrim(final boolean eagerlyWrapped, final SourceSection source) {
      super(eagerlyWrapped, source);
    }

    @Specialization
    public final Object doSBlock(final VirtualFrame frame, final SBlock receiver) {
      return dispatchNode.executeDispatch(frame, new Object[] {receiver});
    }

    @Specialization
    public final boolean doBoolean(final boolean receiver) {
      return receiver;
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "Block2", selector = "value:", receiverType = {SBlock.class})
  public abstract static class ValueOnePrim extends BinaryExpressionNode {
    @Child private BlockDispatchNode dispatchNode = BlockDispatchNodeGen.create();

    public ValueOnePrim(final boolean eagWrap) {
      this(eagWrap, Universe.emptySource.createUnavailableSection());
    }

    public ValueOnePrim(final boolean eagWrap, final SourceSection source) {
      super(eagWrap, source);
    }

    @Specialization
    public final Object doSBlock(final VirtualFrame frame, final SBlock receiver,
        final Object arg) {
      return dispatchNode.executeDispatch(frame, new Object[] {receiver, arg});
    }

    @Override
    protected boolean isTaggedWithIgnoringEagerness(final Class<?> tag) {
      if (tag == OpClosureApplication.class) {
        return true;
      } else {
        return super.isTaggedWith(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "Block3", selector = "value:with:",
      receiverType = {SBlock.class})
  public abstract static class ValueTwoPrim extends TernaryExpressionNode {
    @Child private BlockDispatchNode dispatchNode = BlockDispatchNodeGen.create();

    public ValueTwoPrim(final boolean eagerlyWrapped) {
      this(eagerlyWrapped, null);
    }

    public ValueTwoPrim(final boolean eagerlyWrapped, final SourceSection source) {
      super(eagerlyWrapped, source);
    }

    @Specialization
    public final Object doSBlock(final VirtualFrame frame,
        final SBlock receiver, final Object arg1, final Object arg2) {
      return dispatchNode.executeDispatch(frame, new Object[] {receiver, arg1, arg2});
    }

    @Override
    protected boolean isTaggedWithIgnoringEagerness(final Class<?> tag) {
      if (tag == OpClosureApplication.class) {
        return true;
      } else {
        return super.isTaggedWith(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "Block4", selector = "value:with:with:",
      receiverType = {SBlock.class})
  public abstract static class ValueThreePrim extends QuaternaryExpressionNode {
    @Child private BlockDispatchNode dispatchNode = BlockDispatchNodeGen.create();

    public ValueThreePrim(final boolean eagerlyWrapped) {
      this(eagerlyWrapped, null);
    }

    public ValueThreePrim(final boolean eagerlyWrapped, final SourceSection source) {
      super(eagerlyWrapped, source);
    }

    @Specialization
    public final Object doSBlock(final VirtualFrame frame,
        final SBlock receiver, final Object arg1, final Object arg2, final Object arg3) {
      return dispatchNode.executeDispatch(frame, new Object[] {receiver, arg1, arg2, arg3});
    }

    @Override
    protected boolean isTaggedWithIgnoringEagerness(final Class<?> tag) {
      if (tag == OpClosureApplication.class) {
        return true;
      } else {
        return super.isTaggedWith(tag);
      }
    }
  }

  /*@GenerateNodeFactory
  @Primitive(klass = "Block5", selector = "value:with:with:with:",
             receiverType = {SBlock.class})
  public abstract static class ValueMorePrim extends QuaternaryExpressionNode {
    public ValueMorePrim(final boolean eagerlyWrapped) {this(eagerlyWrapped, null);}
    public ValueMorePrim(final boolean eagerlyWrapped, SourceSection source) { super(eagerlyWrapped, source); }
    @Specialization
    public final Object doSBlock(final VirtualFrame frame,
        final SBlock receiver, final Object firstArg, final Object secondArg,
        final Object thirdArg) {
      CompilerDirectives.transferToInterpreter();
      throw new RuntimeException("This should never be called, because SOM Blocks have max. 2 arguments.");
    }
  }*/

  @GenerateNodeFactory
  @Primitive(klass = "Block", selector = "doTry:onCatchDo:")
  public abstract static class ExceptionDoOnPrim extends TernaryExpressionNode {

    protected static final int INLINE_CACHE_SIZE = VmSettings.DYNAMIC_METRICS ? 100 : 6;
    protected static final IndirectCallNode indirect = Truffle.getRuntime().createIndirectCallNode();

    public static final DirectCallNode createCallNode(final SBlock block, final VirtualFrame frame) {
      return Truffle.getRuntime().createDirectCallNode(
          SInvokable.getCallTarget(block.getMethod(), SArguments.getExecutionLevel(frame)));
    }

    public ExceptionDoOnPrim(final boolean eagWrap, final SourceSection source) {
      super(eagWrap, source);
    }

    public static final boolean sameBlock(final SBlock block, final DynamicObject method) {
      return block.getMethod() == method;
    }

    @Specialization(limit = "INLINE_CACHE_SIZE",
        guards = {"sameBlock(body, cachedBody)",
            "sameBlock(exceptionHandler, cachedExceptionMethod)"})
    public final Object doException(final VirtualFrame frame, final SBlock body,
        final DynamicObject exceptionClass, final SBlock exceptionHandler,
        @Cached("body.getMethod()") final DynamicObject cachedBody,
        @Cached("createCallNode(body, frame)") final DirectCallNode bodyCall,
        @Cached("exceptionHandler.getMethod()") final DynamicObject cachedExceptionMethod,
        @Cached("createCallNode(exceptionHandler, frame)") final DirectCallNode exceptionCall) {
      try {
        return bodyCall.call(SArguments.createSArguments(SArguments.getEnvironment(frame),
            SArguments.getExecutionLevel(frame), new Object[] {body}));
      } catch (SomException e) {
        if (SClass.isKindOf(SObject.getSOMClass(e.getSomObject()), exceptionClass)) {
          return exceptionCall.call(SArguments.createSArguments(SArguments.getEnvironment(frame),
              SArguments.getExecutionLevel(frame), new Object[] {exceptionHandler, e.getSomObject()}));
        } else {
          throw e;
        }
      }
    }

    @Specialization(replaces = "doException")
    public final Object doExceptionUncached(final VirtualFrame frame, final SBlock body,
        final DynamicObject exceptionClass, final SBlock exceptionHandler) {
      try {
        return SInvokable.invoke(body.getMethod(), frame, indirect, new Object[] {body});
      } catch (SomException e) {
        if (SClass.isKindOf(SObject.getSOMClass(e.getSomObject()), exceptionClass)) {
          return SInvokable.invoke(exceptionHandler.getMethod(), frame, indirect,
              new Object[] {exceptionHandler, e.getSomObject()});
        } else {
          throw e;
        }
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "", selector = "ensurePrimitive:")
  @Primitive(selector = "ensure:", receiverType = SBlock.class)
  public abstract static class EnsurePrim extends BinaryExpressionNode {

    @Child private BlockDispatchNode dispatchBody = BlockDispatchNodeGen.create();
    @Child private BlockDispatchNode dispatchHandler = BlockDispatchNodeGen.create();

    protected EnsurePrim(final boolean eagWrap, final SourceSection source) {
      super(eagWrap, source);
    }

    @Specialization
    public final Object doException(final VirtualFrame frame, final SBlock body, final SBlock ensureHandler) {
      try {
        return dispatchBody.executeDispatch(frame, new Object[] {body});
      } finally {
        dispatchHandler.executeDispatch(frame, new Object[] {ensureHandler});
      }
    }
  }
}
