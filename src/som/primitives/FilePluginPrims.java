package som.primitives;

import java.io.File;
import java.io.IOException;

import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SArray;
import som.vmobjects.SFile;
import som.vmobjects.SArray.ArrayType;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ValueProfile;


public abstract class FilePluginPrims {
  
  @GenerateNodeFactory
  public abstract static class ImageFilePrim extends UnaryExpressionNode {
    @Specialization
    public String doGeneric(DynamicObject receiver) {
      return System.getProperty("user.dir") + "/" + Universe.current().imageName();
    }
  }
  
  @GenerateNodeFactory
  public abstract static class OpenFilePrim extends TernaryExpressionNode {
    @Specialization
    public Object doGeneric(DynamicObject receiver, String filename, Boolean writable) {
      SFile file = new SFile(new File(filename), writable);
      if (!file.getFile().exists()){
        return Nil.nilObject;
      }
      return file;
    }
  }
  
  @GenerateNodeFactory
  public abstract static class GetPositionFilePrim extends BinaryExpressionNode {
    @Specialization
    public long doGeneric(DynamicObject receiver, SFile file) {
      return file.getPosition();
    }
  }
  
  @GenerateNodeFactory
  public abstract static class SetPositionFilePrim extends TernaryExpressionNode {
    @Specialization
    public long doGeneric(DynamicObject receiver, SFile file, long position) {
      //Todo: move the stream accordingly
      file.setPosition(position);
      return position;
    }
  }
  
  @GenerateNodeFactory
  public abstract static class SizeFilePrim extends BinaryExpressionNode {
    @Specialization
    public long doGeneric(DynamicObject receiver, SFile file) {
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
  @ImportStatic(ArrayType.class)
  public abstract static class ReadIntoFilePrim extends ExpressionNode {
    public ReadIntoFilePrim() { super(null); }
    
    private final ValueProfile storageType = ValueProfile.createClassProfile();
    
    @Specialization(guards = "isEmptyType(collection)")
    public long doEmpty(DynamicObject receiver, SFile file, SArray collection, long startingAt, long count) {
      collection.transitionTo(ArrayType.BYTE, new byte[(int) count]);
      return this.doGeneric(receiver, file, collection, startingAt, count);
    }
    
    @Specialization
    public long doGeneric(DynamicObject receiver, SFile file, SArray collection, long startingAt, long count) {
      try {
        return (long) file.getInputStream().read(collection.getByteStorage(storageType), (int)startingAt - 1, (int)count);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return 0;
    }
  }
  
  @GenerateNodeFactory
  public abstract static class AtEndFilePrim extends BinaryExpressionNode {
    @Specialization
    public boolean doGeneric(DynamicObject receiver, SFile file) {
      try {
        return file.getInputStream().available() == 0;
      } catch (IOException e) {
        return true;
      }
    }
  }
  
}

