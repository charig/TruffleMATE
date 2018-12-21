package som.primitives;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import bd.primitives.Primitive;
import bd.primitives.Specializer;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.GlobalNode;
import som.interpreter.nodes.GlobalNode.UninitializedGlobalReadWithoutErrorNode;
import som.interpreter.nodes.SOMNode;
import som.primitives.SystemPrims.BinarySystemNode;
import som.vm.NotYetImplementedException;
import som.vm.Universe;
import som.vm.constants.Classes;
import som.vm.constants.Nil;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;


@ImportStatic(SystemPrims.class)
@Primitive(className = "System", primitive = "global:", selector = "global:",
           specializer = GlobalPrim.IsSystemObject.class)
public abstract class GlobalPrim extends BinarySystemNode {
  @Child private GetGlobalNode getGlobal = new UninitializedGetGlobal(0).initialize(Universe.emptySource.createUnavailableSection());

  @Specialization(guards = "receiverIsSystemObject(receiver)")
  public final Object doSObject(final VirtualFrame frame, final DynamicObject receiver, final SSymbol argument) {
    return getGlobal.getGlobal(frame, argument);
  }

  public static class IsSystemObject extends Specializer<Universe, ExpressionNode, SSymbol> {
    public IsSystemObject(final Primitive prim, final NodeFactory<ExpressionNode> fact) {
      super(prim, fact);
    }

    @Override
    public boolean matches(final Object[] args, final ExpressionNode[] argNodess) {
      try {
        return SObject.getSOMClass((DynamicObject) args[0]) == Classes.systemClass;
      } catch (ClassCastException e) {
        return false;
      }
    }
  }


  private abstract static class GetGlobalNode extends SOMNode {
    protected static final int INLINE_CACHE_SIZE = 6;

    public abstract Object getGlobal(VirtualFrame frame, SSymbol argument);

    @Override
    public ExpressionNode getFirstMethodBodyNode() {
      throw new NotYetImplementedException();
    }
  }

  private static final class UninitializedGetGlobal extends GetGlobalNode {
    private final int depth;

    UninitializedGetGlobal(final int depth) {
      this.depth = depth;
    }

    @Override
    public Object getGlobal(final VirtualFrame frame, final SSymbol argument) {
      return specialize(argument).
          getGlobal(frame, argument);
    }

    private GetGlobalNode specialize(final SSymbol argument) {
      if (depth < INLINE_CACHE_SIZE) {
        return replace(new CachedGetGlobal(argument, depth).initialize(getSourceSection()));
      } else {
        GetGlobalNode head = this;
        while (head.getParent() instanceof GetGlobalNode) {
          head = (GetGlobalNode) head.getParent();
        }
        return head.replace(new GetGlobalFallback().initialize(getSourceSection()));
      }
    }
  }

  private static final class CachedGetGlobal extends GetGlobalNode {
    private final int depth;
    private final SSymbol name;
    @Child private GlobalNode getGlobal;
    @Child private GetGlobalNode next;

    CachedGetGlobal(final SSymbol name, final int depth) {
      this.depth = depth;
      this.name  = name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CachedGetGlobal initialize(final SourceSection sourceSection) {
      super.initialize(sourceSection);
      getGlobal = new UninitializedGlobalReadWithoutErrorNode(name).initialize(getSourceSection());
      next = new UninitializedGetGlobal(this.depth + 1).initialize(getSourceSection());
      return this;
    }



    @Override
    public Object getGlobal(final VirtualFrame frame, final SSymbol argument) {
      if (name == argument) {
        return getGlobal.executeGeneric(frame);
      } else {
        return next.getGlobal(frame, argument);
      }
    }
  }

  private static final class GetGlobalFallback extends GetGlobalNode {

    private final Universe universe;

    GetGlobalFallback() {
      this.universe = Universe.getCurrent();
    }

    @Override
    public Object getGlobal(final VirtualFrame frame, final SSymbol argument) {
      Object result = universe.getGlobal(argument);
      return result != null ? result : Nil.nilObject;
    }
  }
}
