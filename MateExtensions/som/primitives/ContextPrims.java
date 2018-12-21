package som.primitives;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import som.interpreter.FrameOnStackMarker;
import som.interpreter.Invokable;
import som.interpreter.MateVisitors;
import som.interpreter.SArguments;
import som.interpreter.SomLanguage;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.MockJavaObject;


public class ContextPrims {
  @GenerateNodeFactory
  @Primitive(className = "Context", primitive = "method", selector = "method", receiverType = {FrameInstance.class})
  public abstract static class GetMethodPrim extends UnaryExpressionNode {
    @Specialization
    public final DynamicObject doMaterializedFrame(final FrameInstance frame) {
      RootCallTarget target = ((RootCallTarget) frame.getCallTarget());
      return ((Invokable) target.getRootNode()).getBelongsToMethod();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Context", primitive = "sender", selector = "sender", receiverType = {FrameInstance.class})
  public abstract static class SenderPrim extends UnaryExpressionNode {
    @Specialization
    public final FrameInstance doMaterializedFrame(final FrameInstance frame) {
      TruffleRuntime runtime = this.getRootNode().getLanguage(SomLanguage.class).getContextReference().get().getTruffleRuntime();
      FrameInstance sender;
      if (runtime.getCurrentFrame() == frame) {
        sender = runtime.getCallerFrame();
      } else {
        sender = runtime.iterateFrames(new MateVisitors.FindSenderFrame(frame.getFrame(FrameAccess.MATERIALIZE)));
      }
      Frame senderFrame = sender.getFrame(FrameAccess.MATERIALIZE);
      if (senderFrame.getFrameDescriptor().findFrameSlot(Universe.frameOnStackSlotName()) == null) {
        senderFrame.setObject(
            senderFrame.getFrameDescriptor().addFrameSlot(Universe.frameOnStackSlotName(), FrameSlotKind.Object),
            new FrameOnStackMarker());
      }
      return sender;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Context", primitive = "receiver", selector = "receiver", receiverType = {FrameInstance.class})
  public abstract static class GetReceiverFromContextPrim extends UnaryExpressionNode {
    @Specialization
    public final DynamicObject doMaterializedFrame(final FrameInstance frame) {
      Frame virtualFrame = frame.getFrame(FrameAccess.READ_ONLY);
      return (DynamicObject) SArguments.rcvr(virtualFrame);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Context", primitive = "localAt:", selector = "localAt:", receiverType = { MockJavaObject.class })
  public abstract static class GetLocalVarAtPrim extends BinaryExpressionNode {
    @Specialization(guards = {"identifier==cachedIdentifier"})
    public final Object doVirtualFrame(final MockJavaObject mockedFrame,
        final String identifier,
        @Cached(value = "identifier") final String cachedIdentifier,
        @Cached(value = "findSlotForIdInLevel(identifier, 1)") final FrameSlot slot) {
      MaterializedFrame frame = (MaterializedFrame) mockedFrame.getMockedObject();
      // Todo: specialize on type
      return frame.getValue(slot);
      // return readnode.executeGeneric(frame);
    }

    protected static FrameSlot findSlotForIdInLevel(final String identifier, final int level) {
      int[] currentLevel = new int[1];
      FrameInstance f = Universe.getCurrent().getTruffleRuntime().iterateFrames(fi -> {
        if (currentLevel[0] == level) {
          return fi;
        }
        currentLevel[0]++;
        return null;
      });
      f.getFrame(FrameAccess.READ_ONLY);
      return f.getCallNode().getRootNode().getFrameDescriptor().findFrameSlot(identifier);
    }

    /*protected static LocalVariableReadNode variableNodeForIdentifier(final String identifier, final int level) {
      FrameSlot slot = findSlotForIdInLevel(identifier, level);
      return LocalVariableReadNodeGen.create(slot, null);
    }*/
  }

  @GenerateNodeFactory
  @Primitive(className = "Context", primitive = "localAt:put:", selector = "localAt:put:", receiverType = { MockJavaObject.class })
  public abstract static class LocalVarAtPutPrim extends TernaryExpressionNode {
    @Specialization(guards = {"identifier==cachedIdentifier"})
    public final Object doVirtualFrame(final MockJavaObject mockedFrame,
        final String identifier, final long value,
        @Cached(value = "identifier") final String cachedIdentifier,
        @Cached(value = "findSlotForId(identifier)") final FrameSlot slot) {
      MaterializedFrame frame = (MaterializedFrame) mockedFrame.getMockedObject();
      if (slot.getKind() != FrameSlotKind.Long) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        slot.setKind(FrameSlotKind.Long);
      }
      frame.setLong(slot, value);
      return value;
    }

    @Specialization(guards = {"identifier==cachedIdentifier"})
    public final Object doVirtualFrame(final MockJavaObject mockedFrame,
        final String identifier, final Object value,
        @Cached(value = "identifier") final String cachedIdentifier,
        @Cached(value = "findSlotForId(identifier)") final FrameSlot slot) {
      MaterializedFrame frame = (MaterializedFrame) mockedFrame.getMockedObject();
      if (slot.getKind() != FrameSlotKind.Object) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        slot.setKind(FrameSlotKind.Object);
      }
      frame.setObject(slot, value);
      return value;
    }

    protected FrameSlot findSlotForId(final String identifier) {
      return GetLocalVarAtPrim.findSlotForIdInLevel(identifier, 1);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Context", primitive = "argAt:", selector = "argAt:", receiverType = { MockJavaObject.class })
  public abstract static class GetArgAtPrim extends BinaryExpressionNode {
    // Todo: dispatch chain index->slot
    @Specialization
    public final Object doVirtualFrame(final MockJavaObject mockedFrame,
        final long index) {
      Frame frame = (Frame) mockedFrame.getMockedObject();
      return SArguments.arg(frame, (int) index);
    }
  }
}
