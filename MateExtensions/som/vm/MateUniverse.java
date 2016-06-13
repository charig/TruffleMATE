package som.vm;

import static som.vm.constants.Classes.objectClass;
import static som.vm.constants.MateClasses.shapeClass;
import static som.vm.constants.MateClasses.environmentMO;
import static som.vm.constants.MateClasses.messageMO;
import static som.vm.constants.MateClasses.operationalSemanticsMO;
import static som.vm.constants.MateClasses.contextClass;
import som.interpreter.Invokable;
import som.interpreter.MateifyVisitor;
import som.interpreter.nodes.MateMessageSpecializationsFactory;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.vm.constants.Nil;
import som.vmobjects.InvokableLayoutImpl;
import som.vmobjects.SClass;
import som.vmobjects.SMateEnvironment;
import som.vmobjects.SObject;
import som.vmobjects.SReflectiveObject;
import som.vmobjects.SSymbol;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Shape;

public class MateUniverse extends Universe {
  private Assumption mateActivated;
  private Assumption mateDeactivated;
  
  public MateUniverse() {
    super();
    mateDeactivated = this.getTruffleRuntime().createAssumption();
    mateActivated = null;
  }

  protected void initializeObjectSystem() {
    if (alreadyInitialized) {
      return;
    } else {
      super.initializeObjectSystem();
      
      mateifyNilObject();
      
      // Initialize the Mate metamodel.
      initializeSystemClass(environmentMO, objectClass, "EnvironmentMO");
      initializeSystemClass(operationalSemanticsMO, objectClass, "OperationalSemanticsMO");
      initializeSystemClass(messageMO, objectClass, "MessageMO");
      initializeSystemClass(shapeClass, objectClass, "Shape");
      initializeSystemClass(contextClass, objectClass, "Context");
      
      // Load methods and fields into the Mate MOP.
      loadSystemClass(environmentMO);
      loadSystemClass(operationalSemanticsMO);
      loadSystemClass(messageMO);
      loadSystemClass(shapeClass);
      loadSystemClass(contextClass);
      
      AbstractMessageSendNode.specializationFactory = new MateMessageSpecializationsFactory();
    }
  }
  
  public static DynamicObject newInstance(final DynamicObject instanceClass) {
    return SReflectiveObject.create(instanceClass);
  }
  
  public static DynamicObject newEnvironment(final DynamicObject instanceClass) {
    return SMateEnvironment.create(instanceClass);
  }
  
  @Override
  public DynamicObject loadClass(final SSymbol name) {
    if ((DynamicObject) getGlobal(name) != null){
      return super.loadClass(name);
    } else {
      DynamicObject result = super.loadClass(name);
      try{
        mateify(result);
      } catch (NullPointerException e){
        println(name.getString());
      }
      mateify(SObject.getSOMClass(result));
      return result;
    }
  }
  
  protected void loadSystemClass(final DynamicObject systemClass) {
    super.loadSystemClass(systemClass);
    mateify(systemClass);
    mateify(SObject.getSOMClass(systemClass));
  }
  
  public void mateify(DynamicObject clazz) {
    int countOfInvokables = SClass.getNumberOfInstanceInvokables(clazz);
    MateifyVisitor visitor = new MateifyVisitor();
    for (int i = 0; i < countOfInvokables; i++){
      DynamicObject method = SClass.getInstanceInvokable(clazz, i);
      Invokable node = InvokableLayoutImpl.INSTANCE.getInvokable(method);
      if (SClass.getName(clazz).getString().equals("Handle class") &&
          InvokableLayoutImpl.INSTANCE.getSignature(method).getString().equals("targetBaseeeeee:")){
        return;
      } else if (SClass.getName(clazz).getString().equals("Handle")){
        return;
      } else {
        node.accept(visitor);
      }
    }
  }
  
  public void mateifyMethod(DynamicObject method) {
    MateifyVisitor visitor = new MateifyVisitor();
    Invokable node = InvokableLayoutImpl.INSTANCE.getInvokable(method);
    node.accept(visitor);
  }
  
  public static void main(final String[] arguments) {
    MateUniverse u = current();
    try {
      u.interpret(arguments);
      u.exit(0);
    } catch (IllegalStateException e) {
      errorExit(e.getMessage());
    }
  }
  
  @Override
  public DynamicObjectFactory getInstancesFactory(){
    return SReflectiveObject.SREFLECTIVE_OBJECT_FACTORY;
  }
  
  @Override
  public Shape createObjectShapeForClass(DynamicObject clazz){
    return SReflectiveObject.createObjectShapeForClass(clazz);
  }
  
  public Assumption getMateDeactivatedAssumption(){
    return this.mateDeactivated;
  }
  
  public Assumption getMateActivatedAssumption(){
    return this.mateActivated;
  }
  
  @Override
  public void activatedMate(){
    if (this.getMateDeactivatedAssumption().isValid()){
      this.getMateDeactivatedAssumption().invalidate();
    }
    this.mateActivated = this.getTruffleRuntime().createAssumption();
  }
  
  public void deactivateMate(){
    if (this.getMateActivatedAssumption().isValid()){
      this.getMateActivatedAssumption().invalidate();
    }
    this.mateDeactivated = this.getTruffleRuntime().createAssumption();
  }
  
  public static MateUniverse current() {
    if (Universe.getCurrent() == null) {
      Universe.setCurrent(new MateUniverse());
    }
    return (MateUniverse) Universe.getCurrent();
  }
  
  public void mateifyNilObject(){
    Nil.nilObject.setShapeAndGrow(Nil.nilObject.getShape(), Nil.nilObject.getShape().changeType(SReflectiveObject.SREFLECTIVE_OBJECT_TYPE));
  }
}