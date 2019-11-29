package som.primitives.reflection;

import static som.interpreter.TruffleCompiler.transferToInterpreterAndInvalidate;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

import som.interpreter.nodes.dispatch.DispatchChain;
import som.interpreter.objectstorage.FieldAccessorNode;
import som.interpreter.objectstorage.FieldAccessorNode.ReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.WriteFieldNode;
import som.vm.constants.Nil;


public abstract class IndexDispatch extends Node implements DispatchChain {
  public static final int INLINE_CACHE_SIZE = 6;

  public static IndexDispatch create() {
    return new UninitializedDispatchNode(0);
  }

  protected final int depth;

  public IndexDispatch(final int depth) {
    this.depth = depth;
  }

  public abstract Object executeDispatch(DynamicObject obj, int index);
  public abstract Object executeDispatch(DynamicObject obj, int index, Object value);

  private static final class UninitializedDispatchNode extends IndexDispatch {

    UninitializedDispatchNode(final int depth) {
      super(depth);
    }

    private IndexDispatch specialize(final int index, final boolean read) {
      transferToInterpreterAndInvalidate("Initialize a dispatch node.");

      if (depth < INLINE_CACHE_SIZE) {
        IndexDispatch specialized;
        if (read) {
          specialized = new CachedReadDispatchNode(index,
            new UninitializedDispatchNode(depth + 1), depth);
        } else {
          specialized = new CachedWriteDispatchNode(index,
              new UninitializedDispatchNode(depth + 1), depth);
        }
        return replace(specialized);
      }

      IndexDispatch headNode = determineChainHead();
      return headNode.replace(new GenericDispatchNode());
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index) {
      return specialize(index, true).
          executeDispatch(obj, index);
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index, final Object value) {
      return specialize(index, false).
          executeDispatch(obj, index, value);
    }


    private IndexDispatch determineChainHead() {
      Node i = this;
      while (i.getParent() instanceof IndexDispatch) {
        i = i.getParent();
      }
      return (IndexDispatch) i;
    }

    @Override
    public int lengthOfDispatchChain() {
      return 0;
    }
  }

  private static final class CachedReadDispatchNode extends IndexDispatch {
    private final int index;
    @Child private ReadFieldNode access;
    @Child private IndexDispatch next;

    CachedReadDispatchNode(final int index,
        final IndexDispatch next, final int depth) {
      super(depth);
      this.index = index;
      this.next = next;
      access = FieldAccessorNode.createRead(index);
      this.adoptChildren();
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index) {
      if (this.index == index) {
        return access.executeRead(obj);
      } else {
        return next.executeDispatch(obj, index);
      }
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index, final Object value) {
      CompilerAsserts.neverPartOfCompilation("CachedReadDispatchNode");
      throw new RuntimeException("This should be never reached.");
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1 + next.lengthOfDispatchChain();
    }
  }

  private static final class CachedWriteDispatchNode extends IndexDispatch {
    private final int index;
    @Child private WriteFieldNode access;
    @Child private IndexDispatch next;

    CachedWriteDispatchNode(final int index,
        final IndexDispatch next, final int depth) {
      super(depth);
      this.index = index;
      this.next = next;
      access = FieldAccessorNode.createWrite(index);
      this.adoptChildren();
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index) {
      CompilerAsserts.neverPartOfCompilation("CachedWriteDispatchNode");
      throw new RuntimeException("This should be never reached.");
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index, final Object value) {
      if (this.index == index) {
        return access.write(obj, value);
      } else {
        return next.executeDispatch(obj, index, value);
      }
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1 + next.lengthOfDispatchChain();
    }
  }

  private static final class GenericDispatchNode extends IndexDispatch {

    GenericDispatchNode() {
      super(0);
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index) {
      /*The nil as default value is needed for the case when the object has not been still initialized.
        See ReadFieldNode when location == null*/
      return obj.get(index, Nil.nilObject);
    }

    @Override
    public Object executeDispatch(final DynamicObject obj, final int index, final Object value) {
      obj.set(index, value);
      obj.define(index, value);
      return value;
    }

    @Override
    public int lengthOfDispatchChain() {
      return 1000;
    }
  }
}
