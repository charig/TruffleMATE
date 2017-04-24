package som.primitives;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import som.interpreter.MateNode;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.interpreter.nodes.dispatch.AbstractDispatchNode.AbstractCachedDispatchNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.interpreter.objectstorage.FieldAccessorNode.ReadFieldNode;
import som.vm.ObjectMemory;
import som.vm.Universe;
import som.vm.constants.MateClasses;
import som.vm.constants.MateGlobals;
import som.vm.constants.Nil;
import som.vmobjects.MockJavaObject;
import som.vmobjects.SArray;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Introspection;
import com.oracle.truffle.api.dsl.Introspection.Provider;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.Introspection.SpecializationInfo;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeVisitor;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;


public class CompilationPrims {
  public static DynamicObject translateSpecializationInfo(SpecializationInfo specialization){
    DynamicObject stSpecialization = Universe.getCurrent().createInstance("SpecializationInfo");
    stSpecialization.define(MateGlobals.SPECIALZATIONINFO_METHOD_NAME_INDEX,
        specialization.getMethodName());
    stSpecialization.define(MateGlobals.SPECIALZATIONINFO_IS_ACTIVE_INDEX,
        specialization.isActive());
    stSpecialization.define(MateGlobals.SPECIALZATIONINFO_CACHED_DATA_INDEX,
        Nil.nilObject); // Do we need the cached data? In which format?
    stSpecialization.define(MateGlobals.SPECIALZATIONINFO_SPECIALIZATIONS_INDEX,
        translateSpecializations(specialization.getSpecializations()));
    return stSpecialization;
  }
  
  protected static DynamicObject translateSpecializations(List<Object> specializations){
    SArray arrayOfSpecializations;
    long last;
    if (specializations == null){
      arrayOfSpecializations = SArray.create(new Object[]{});
      last = 1;
    } else {
      MockJavaObject[] stSpecializations = new MockJavaObject[specializations.size()];
      int i = 0;
      ObjectMemory engine = Universe.getCurrent().getObjectMemory();
      for (Object specialization: specializations){
        stSpecializations[i] = new MockJavaObject(specialization, 
            Universe.getCurrent().loadClass(engine.symbolFor(specialization.getClass().getSimpleName())));
        i++;
      }
      arrayOfSpecializations = SArray.create(stSpecializations);
      last = specializations.size() + 1;
    }
    DynamicObject vector = Universe.getCurrent().createInstance("Vector");
    vector.define(MateGlobals.VECTOR_FIRST_INDEX,
        (long) 1);
    vector.define(MateGlobals.VECTOR_LAST_INDEX,
        last);
    vector.define(MateGlobals.VECTOR_STORAGE_INDEX,
        arrayOfSpecializations);
    return vector;
  }
  
  @GenerateNodeFactory
  @Primitive(klass = "MessageSendNode", selector = "selector",
             eagerSpecializable = false, mate = true)
  public abstract static class MateMessageSendNodesPrim extends UnaryExpressionNode {
    public MateMessageSendNodesPrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }

    @Specialization
    public final SSymbol doMock(final MockJavaObject receiver) {
      AbstractMessageSendNode mockedNode = (AbstractMessageSendNode) receiver.getMockedObject();
      return mockedNode.getSelector();
    }
  }
  
  @GenerateNodeFactory
  @Primitive(klass = "FieldReadNode", selector = "fieldIndex",
             eagerSpecializable = false, mate = true)
  public abstract static class MateFieldReadFieldIndexPrim extends UnaryExpressionNode {
    public MateFieldReadFieldIndexPrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }

    @Specialization
    public final long doMock(final MockJavaObject receiver) {
      ReadFieldNode mockedNode = (ReadFieldNode) receiver.getMockedObject();
      return mockedNode.getFieldIndex();
    }
  }
  
  @GenerateNodeFactory
  @Primitive(klass = "DispatchChain", selector = "basicRemove:",
             eagerSpecializable = false, mate = true)
  public abstract static class RemoveSpecializationPrim extends BinaryExpressionNode {
    public RemoveSpecializationPrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }

    @Specialization
    public final boolean doMock(final DynamicObject dispatchChain, final MockJavaObject node) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      Node baseNode = (Node) ((MockJavaObject) dispatchChain.get(1)).getMockedObject();
      Object specialization = node.getMockedObject();
      String removeMethodName = "remove" + specialization.getClass().getSimpleName().replace("Data", "") + "_";
      Method method;
      try {
        method = baseNode.getClass().getDeclaredMethod(removeMethodName, Object.class);
        method.invoke(baseNode, specialization);
      } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException e) {
        return false;
      }
      return true;
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "ASTNode", selector = "isMateNode",
             eagerSpecializable = false, mate = true)
  public abstract static class IsMateNodePrim extends UnaryExpressionNode {
    public IsMateNodePrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }

    @Specialization
    public final boolean doMock(final MockJavaObject receiver) {
      return receiver.getMockedObject() instanceof MateNode;
    }
  }

  @GenerateNodeFactory
  @Primitive(klass = "ASTNode", selector = "filterNodesByClassname:as:",
      eagerSpecializable = false, mate = true)
  public abstract static class MateFilterNodesByClassPrim extends TernaryExpressionNode {
    public MateFilterNodesByClassPrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }
    
    @SuppressWarnings("unchecked")
    @TruffleBoundary
    @Specialization
    public final SArray doMock(final MockJavaObject receiver, final String classname,
        DynamicObject klass) {
      Node mockedNode = (Node) receiver.getMockedObject();
      Class<? extends Node> somNodeClass = null;
      try {
        somNodeClass = (Class<? extends Node>) Class.forName(classname);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      
      List<Node> nodes = filterChildrenByClass(mockedNode, somNodeClass);
      MockJavaObject[] stNodes = new MockJavaObject[nodes.size()]; 
      int i = 0;
      for (Node node: nodes){
        stNodes[i] = new MockJavaObject(node, klass);
        i++;
      }
      return SArray.create(stNodes);
    }
    
    public static List<Node> filterChildrenByClass(Node node, Class<? extends Node> somClass){
      NodeTypeFilter filter = new NodeTypeFilter(somClass);
      node.accept(filter);
      return filter.getResultingNodes();
    }
  }

  private static final class NodeTypeFilter implements NodeVisitor {
    
    Class<? extends Node> filter;
    private final ArrayList<Node> selectedNodes = new ArrayList<Node>();

    NodeTypeFilter(Class<? extends Node> filter) {
        this.filter = filter;
    }

    public boolean visit(Node node) {
      if (filter.isInstance(node)) {
        selectedNodes.add(node);
      }
      return true;
    }
    
    public ArrayList<Node> getResultingNodes() {
      return selectedNodes;
    }
  }
  
  @GenerateNodeFactory
  @Primitive(klass = "ASTNode", selector = "dispatchChain",
      eagerSpecializable = false, mate = true)
  public abstract static class GetDispatchChainPrim extends UnaryExpressionNode {
    public GetDispatchChainPrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }
    
    @Specialization(guards = "isIntrospectable(receiver)")
    public final DynamicObject doInstrospectable(final MockJavaObject receiver) {
      Node mockedNode = (Node) receiver.getMockedObject();
      assert Introspection.isIntrospectable(mockedNode);
      List<SpecializationInfo> specializations = Introspection.getSpecializations(mockedNode);
      DynamicObject[] stSpecializations = new DynamicObject[specializations.size()];
      int i = 0;
      for (SpecializationInfo specialization : specializations){
        stSpecializations[i] = translateSpecializationInfo(specialization); 
        i++;
      }
      return this.createChain(stSpecializations, receiver);
    }
    
    @Specialization(guards = "isMessageSendNode(receiver)")
    public final DynamicObject doMessage(final MockJavaObject receiver) {
      AbstractMessageSendNode mockedNode = (AbstractMessageSendNode) receiver.getMockedObject();
      DynamicObject[] specializations = mockedNode.getSpecializations();
      return this.createChain(specializations, receiver);
    }
    
    protected DynamicObject createChain(DynamicObject[] specializations, MockJavaObject node){
      DynamicObject chain = Universe.getCurrent().createInstance("DispatchChain");
      chain.define(MateGlobals.DISPATCHCHAIN_SPECIALIZATIONS_INFO_INDEX,
          SArray.create(specializations));
      chain.define(MateGlobals.DISPATCHCHAIN_PARENT_NODE_INDEX, node);
      return chain;
    }
    
    protected static boolean isIntrospectable(MockJavaObject object){
      return object.getMockedObject() instanceof Provider;
    }
    
    protected static boolean isMessageSendNode(MockJavaObject object){
      return object.getMockedObject() instanceof AbstractMessageSendNode;
    }
  }
  
  @GenerateNodeFactory
  @Primitive(klass = "CachedDispatchNode", selector = "targetNode",
      eagerSpecializable = false, mate = true)
  public abstract static class TargetNodePrim extends UnaryExpressionNode {
    public TargetNodePrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }
    
    @Specialization
    public final MockJavaObject doInstrospectable(final MockJavaObject receiver) {
      AbstractCachedDispatchNode mockedNode = (AbstractCachedDispatchNode) receiver.getMockedObject();
      return new MockJavaObject(mockedNode.getCallNode().getCurrentRootNode(),
          MateClasses.astNodeClass);
    }
  }
  
  @GenerateNodeFactory
  @Primitive(klass = "CachedDispatchNode", selector = "split",
      eagerSpecializable = false, mate = true)
  public abstract static class SplitNodePrim extends UnaryExpressionNode {
    public SplitNodePrim(final boolean eagWrap, final SourceSection source) {
      super(false, source);
    }
    
    @TruffleBoundary
    @Specialization
    public final boolean doCachedNode(final MockJavaObject receiver) {
      AbstractCachedDispatchNode mockedNode = (AbstractCachedDispatchNode) receiver.getMockedObject();
      assert mockedNode.getCallNode().isCallTargetCloningAllowed();
      assert !(mockedNode.getCallNode().isCallTargetCloned());
      CompilerDirectives.transferToInterpreterAndInvalidate();
      mockedNode.getCallNode().cloneCallTarget();
      return mockedNode.getCallNode().isCallTargetCloned();
    }
  }
}
