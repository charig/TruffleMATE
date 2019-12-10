package som.primitives;

import java.io.File;
import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ValueProfile;

import bd.primitives.Primitive;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.EagerPrimitive;
import som.interpreter.nodes.nary.EagerlySpecializableNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SArray;
import som.vmobjects.SArray.ArrayType;
import som.vmobjects.SFile;
import som.vmobjects.SSymbol;


public abstract class FilePluginPrims {

  @GenerateNodeFactory
  @Primitive(className = "FilePluginPrims", primitive = "imageFile", selector = "imageFile")
  public abstract static class ImageFilePrim extends UnaryExpressionNode {
    @Specialization
    public String doGeneric(final DynamicObject receiver) {
      return System.getProperty("user.dir") + "/" + Universe.getCurrent().imageName();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "StandardFileStream", primitive = "primOpen:writable:", selector = "primOpen:writable:")
  public abstract static class OpenFilePrim extends TernaryExpressionNode {
    @Specialization
    public Object doGeneric(final DynamicObject receiver, final String filename, final Boolean writable) {
      SFile file = new SFile(new File(filename), writable);
      if (!file.getFile().exists()) {
        return Nil.nilObject;
      }
      return file;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "StandardFileStream", primitive = "primGetPosition:", selector = "primGetPosition:")
  public abstract static class GetPositionFilePrim extends BinaryExpressionNode {
    @Specialization
    public long doGeneric(final DynamicObject receiver, final SFile file) {
      return file.getPosition();
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "StandardFileStream", primitive = "primSetPosition:to:", selector = "primSetPosition:to:")
  public abstract static class SetPositionFilePrim extends TernaryExpressionNode {
    @Specialization
    @TruffleBoundary
    public long doGeneric(final DynamicObject receiver, final SFile file, final long position) {
      file.setPosition(position);
      return position;
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "StandardFileStream", selector = "primSize:")
  public abstract static class SizeFilePrim extends BinaryExpressionNode {
    @Specialization
    public long doGeneric(final DynamicObject receiver, final SFile file) {
      return file.getFile().length();
    }
  }

  @GenerateNodeFactory
  @NodeChildren({
    @NodeChild(value = "receiver", type = ExpressionNode.class),
    @NodeChild(value = "sfile", type = ExpressionNode.class),
    @NodeChild(value = "vector", type = ExpressionNode.class),
    @NodeChild(value = "starting", type = ExpressionNode.class),
    @NodeChild(value = "count", type = ExpressionNode.class),
  })
  @Primitive(className = "StandardFileStream", primitive = "primRead:into:startingAt:count:", noWrapper = true)
  @ImportStatic(ArrayType.class)
  public abstract static class ReadIntoFilePrim extends EagerlySpecializableNode {

    private final ValueProfile storageType = ValueProfile.createClassProfile();

    public abstract Object executeEvaluated(VirtualFrame frame, Object receiver, Object file, Object collection, Object startingAt, Object count);


    @Specialization(guards = {"isByteType(collection)"})
    public long doEmptyBytes(final DynamicObject receiver, final SFile file, final SArray collection, final long startingAt, final long count) {
      if (ArrayType.isEmptyType(collection)) {
        collection.transitionTo(ArrayType.BYTE, new byte[(int) count]);
      }
      byte[] buffer = collection.getByteStorage(storageType);
      return read(file, buffer, (int) startingAt - 1, (int) count);
    }

    @TruffleBoundary
    @Specialization(guards = {"!isByteType(collection)"})
    public long doEmpty(final DynamicObject receiver, final SFile file, final SArray collection, final long startingAt, final long count) {
      byte[] buffer = new byte[(int) count];
      long countRead = read(file, buffer, (int) startingAt - 1, (int) count);
      /*TODO: Workaround this so in case the read is in a subpart of the array we do not lose the rest*/
      collection.transitionTo(ArrayType.CHAR, (new String(buffer)).toCharArray());
      return countRead;
    }

    @TruffleBoundary
    private static long read(final SFile file, final byte[] buffer, final int start, final int count) {
      try {
        return file.getInputStream().read(buffer, start, count);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return 0;
    }

    @Override
    public EagerPrimitive wrapInEagerWrapper(final SSymbol selector,
        final ExpressionNode[] arguments, final Universe vm) {
      Universe.errorExit("It should never enter here since noWrapper is set to true.\n" +
          "The right implementation is to remove the specializer o fix bd so that the default" +
          "specializer do not neccesarily requires an EagerPrimitive");
      return null;
    }

    @Override
    public Object doPreEvaluated(final VirtualFrame frame,
        final Object[] arguments) {
      return executeEvaluated(frame, arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);
    }

  }

  @GenerateNodeFactory
  @Primitive(className = "StandardFileStream", primitive = "primAtEnd:", selector = "primAtEnd:")
  public abstract static class AtEndFilePrim extends BinaryExpressionNode {
    @Specialization
    @TruffleBoundary
    public boolean doGeneric(final DynamicObject receiver, final SFile file) {
      try {
        return file.getInputStream().available() == 0;
      } catch (IOException e) {
        Universe.errorExit("Error when trying to set file to eof");
        return false;
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "StandardFileStream", primitive = "primClose:", selector = "primClose:")
  public abstract static class CloseFilePrim extends BinaryExpressionNode {
    @Specialization
    @TruffleBoundary
    public boolean doGeneric(final DynamicObject receiver, final SFile file) {
      try {
        file.close();
        return true;
      } catch (IOException e) {
        Universe.errorExit("Error when closing file");
        return false;
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "StandardFileStream", primitive = "primSizeNoError:", selector = "primSizeNoError:")
  public abstract static class SizeNoErrorFilePrim extends BinaryExpressionNode {
    @Specialization
    @TruffleBoundary
    public Object doGeneric(final DynamicObject receiver, final SFile file) {
      long size = file.getFile().length();
      if (size > 0) {
        return size;
      } else {
        return Nil.nilObject;
      }
    }
  }


}
