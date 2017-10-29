/**
 * Copyright (c) 2013 Stefan Marr, stefan.marr@vub.ac.be
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package som.interpreter.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

import som.interpreter.nodes.MateFieldNodesFactory.MateFieldReadNodeGen;
import som.interpreter.nodes.MateFieldNodesFactory.MateFieldWriteNodeGen;
import som.interpreter.nodes.nary.ExpressionWithTagsNode;
import som.interpreter.objectstorage.FieldAccessorNode;
import som.interpreter.objectstorage.FieldAccessorNode.ReadFieldNode;
import som.interpreter.objectstorage.FieldAccessorNode.WriteFieldNode;
import tools.dym.Tags.FieldRead;
import tools.dym.Tags.FieldWrite;

public abstract class FieldNode extends ExpressionWithTagsNode {
  protected FieldNode(final SourceSection source) {
    super(source);
  }

  public abstract ExpressionNode getSelf();

  @NodeChild(value = "self", type = ExpressionNode.class)
  public abstract static class FieldReadNode extends FieldNode {

    @Child protected ReadFieldNode read;

    protected FieldReadNode(final int fieldIndex, final SourceSection source) {
//    implements PreevaluatedExpression {
      super(source);
      read = FieldAccessorNode.createRead(fieldIndex);
    }

    @Specialization
    public Object executeEvaluated(final VirtualFrame frame, final DynamicObject obj) {
      return read.executeRead(obj);
    }

    /*@Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return executeEvaluated((DynamicObject) arguments[0]);
    }*/

    @Override
    public ExpressionNode asMateNode() {
      return MateFieldReadNodeGen.create(this, this.getSelf());
    }

    @Override
    protected boolean isTaggedWith(final Class<?> tag) {
      if (tag == FieldRead.class) {
        return true;
      } else {
        return super.isTaggedWith(tag);
      }
    }
  }

  @NodeChildren({
    @NodeChild(value = "self", type = ExpressionNode.class),
    @NodeChild(value = "value", type = ExpressionNode.class)})
  public abstract static class FieldWriteNode extends FieldNode {
    // implements PreevaluatedExpression {
    @Child protected WriteFieldNode write;

    public abstract ExpressionNode getValue();

    public FieldWriteNode(final int fieldIndex, final SourceSection source) {
      super(source);
      write = FieldAccessorNode.createWrite(fieldIndex);
    }

    @Specialization
    public Object executeEvaluated(final VirtualFrame frame, final DynamicObject self, final Object value) {
      return write.write(self, value);
    }

    /*@Override
    public final Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return executeEvaluated((DynamicObject) arguments[0], arguments[1]);
    }*/

    @Override
    public ExpressionNode asMateNode() {
      return MateFieldWriteNodeGen.create(this, this.getSelf(), this.getValue());
    }

    @Override
    protected boolean isTaggedWith(final Class<?> tag) {
      if (tag == FieldWrite.class) {
        return true;
      } else {
        return super.isTaggedWith(tag);
      }
    }
  }
}
