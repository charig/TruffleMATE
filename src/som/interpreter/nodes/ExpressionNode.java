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

import java.math.BigInteger;

import som.interpreter.TypesGen;
import som.vm.constants.ReflectiveOp;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SArray;
import som.vmobjects.SBlock;
import som.vmobjects.SSymbol;
import tools.dym.Tags.ControlFlowCondition;
import tools.dym.Tags.LoopBody;
import tools.dym.Tags.PrimitiveArgument;
import tools.dym.Tags.VirtualInvokeReceiver;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;

@Instrumentable(factory = ExpressionNodeWrapper.class)
public abstract class ExpressionNode extends SOMNode {

  public ExpressionNode(final SourceSection sourceSection) {
    super(sourceSection);
  }

  public abstract Object executeGeneric(final VirtualFrame frame);
  
  @Override
  protected boolean isTaggedWith(final Class<?> tag) {
    if (tag == StatementTag.class) {
      return true;
    } else if (tag == RootTag.class) {
      return false;//isTagged(ROOT_EXPR);
    } else if (tag == LoopBody.class) {
      return false; //isTagged(LOOP_BODY);
    } else if (tag == ControlFlowCondition.class) {
      return false; //isTagged(CONTROL_FLOW_CONDITION);
    } else if (tag == PrimitiveArgument.class) {
      return false; //isTagged(PRIMITIVE_ARGUMENT);
    } else if (tag == VirtualInvokeReceiver.class) {
      return false; //isTagged(VIRTUAL_INVOKE_RECEIVER);
    } else {
      return super.isTaggedWith(tag);
    }
  }

  @Override
  public ExpressionNode getFirstMethodBodyNode() { return this; }
  
  public boolean executeBoolean(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectBoolean(executeGeneric(frame));
  }

  public long executeLong(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectLong(executeGeneric(frame));
  }

  public BigInteger executeBigInteger(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectBigInteger(executeGeneric(frame));
  }

  public String executeString(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectString(executeGeneric(frame));
  }

  public double executeDouble(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectDouble(executeGeneric(frame));
  }

  public SSymbol executeSSymbol(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectSSymbol(executeGeneric(frame));
  }

  public SBlock executeSBlock(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectSBlock(executeGeneric(frame));
  }

  public DynamicObject executeDynamicObject(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectDynamicObject(executeGeneric(frame));
  }

  /*public SInvokable executeSInvokable(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectSInvokable(executeGeneric(frame));
  }*/

  public SArray executeSArray(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectSArray(executeGeneric(frame));
  }

  public SAbstractObject executeSAbstractObject(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectSAbstractObject(executeGeneric(frame));
  }

  public Object[] executeArgumentArray(final VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectObjectArray(executeGeneric(frame));
  }
  
  public ReflectiveOp reflectiveOperation(){
    return ReflectiveOp.None;
  }
}
