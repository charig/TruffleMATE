package som.primitives;

import som.interpreter.FrameOnStackMarker;
import som.interpreter.Invokable;
import som.interpreter.MateVisitors;
import som.interpreter.SArguments;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SClass;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.object.DynamicObject;


public class ContextPrims {
  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  public abstract static class GetMethodPrim extends UnaryExpressionNode {
    @Specialization
    public final DynamicObject doMaterializedFrame(final FrameInstance frame) {
      RootCallTarget target = ((RootCallTarget)frame.getCallTarget());
      return ((Invokable)target.getRootNode()).getBelongsToMethod();
    }
  }
  
  @GenerateNodeFactory
  public abstract static class SenderPrim extends UnaryExpressionNode {
    @Specialization
    public final FrameInstance doMaterializedFrame(final FrameInstance frame) {
      TruffleRuntime runtime = ((Universe)((ExpressionNode)this).getRootNode().getExecutionContext()).getTruffleRuntime();
      FrameInstance sender;
      if (runtime.getCurrentFrame() == frame){
        sender = runtime.getCallerFrame();
      } else {
        sender = runtime.iterateFrames(new MateVisitors.FindSenderFrame(frame.getFrame(FrameAccess.MATERIALIZE, true)));
      }
      Frame senderFrame = sender.getFrame(FrameAccess.MATERIALIZE, true);
      if (senderFrame.getFrameDescriptor().findFrameSlot(Universe.frameOnStackSlotName()) == null){
        senderFrame.setObject(
            senderFrame.getFrameDescriptor().addFrameSlot(Universe.frameOnStackSlotName(), FrameSlotKind.Object), 
            new FrameOnStackMarker());
      }
      return sender; 
    }  
  }
  
  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  public abstract static class GetReceiverFromContextPrim extends UnaryExpressionNode {
    @Specialization
    public final DynamicObject doMaterializedFrame(final Object frame) {
      Frame virtualFrame = ((FrameInstance)frame).getFrame(FrameAccess.READ_ONLY, false);
      return (DynamicObject) SArguments.rcvr(virtualFrame);
    }
  }
}
