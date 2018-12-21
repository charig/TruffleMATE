package som.primitives;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SArray;
import som.vmobjects.SClass;


public class ClassPrims {

  @GenerateNodeFactory
  @Primitive(className = "Class", primitive = "name")
  @ImportStatic(SClass.class)
  public abstract static class NamePrim extends UnaryExpressionNode {
    @TruffleBoundary
    @Specialization(guards = "isSClass(receiver)")
    public final SAbstractObject doSClass(final DynamicObject receiver) {
      // CompilerAsserts.neverPartOfCompilation("Class>>NamePrim");
      return SClass.getName(receiver);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Class", primitive = "superclass", selector = "superclass")
  @ImportStatic(SClass.class)
  public abstract static class SuperClassPrim extends UnaryExpressionNode {
    @Specialization(guards = "isSClass(receiver)")
    public final DynamicObject doSClass(final DynamicObject receiver) {
      // CompilerAsserts.neverPartOfCompilation("Class>>SuperClassPrim");
      return SClass.getSuperClass(receiver);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Class", primitive = "methods", selector = "methods")
  @ImportStatic(SClass.class)
  public abstract static class InstanceInvokablesPrim extends UnaryExpressionNode {
    @TruffleBoundary
    @Specialization(guards = "isSClass(receiver)")
    public final SArray doSClass(final DynamicObject receiver) {
      CompilerAsserts.neverPartOfCompilation("Class>>InstanceInvokablesPrim");
      return SClass.getInstanceInvokables(receiver);
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Class", primitive = "fields", selector = "fields")
  @ImportStatic(SClass.class)
  public abstract static class InstanceFieldsPrim extends UnaryExpressionNode {
    @Specialization(guards = "isSClass(receiver)")
    public final SArray doSClass(final DynamicObject receiver) {
      CompilerAsserts.neverPartOfCompilation("Class>>instanceFields");
      return SClass.getInstanceFields(receiver);
    }
  }
}
