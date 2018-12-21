package tools.dym.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import som.primitives.LengthPrim;
import som.primitives.LengthPrimFactory;
import som.vmobjects.SArray;
import tools.dym.profiles.ArrayCreationProfile;


public class ArrayAllocationProfilingNode extends CountingNode<ArrayCreationProfile> {

  @Child protected LengthPrim size;

  public ArrayAllocationProfilingNode(final ArrayCreationProfile counter) {
    super(counter);
    size = LengthPrimFactory.create(null);
  }

  @Override
  protected void onReturnValue(final VirtualFrame frame, final Object result) {
    counter.profileArraySize((int) size.executeEvaluated((SArray) result));
  }
}
