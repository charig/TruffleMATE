package som.vmobjects;

import com.oracle.truffle.api.object.DynamicObject;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vm.constants.ReflectiveOp;

public class SMateEnvironment extends SObject {
  public static final int Semantics_IDX = 0;
  public static final int Layout_IDX = 1;
  public static final int Message_IDX = 2;
  
  //Todo: Finish the SMateEnvironment type with primitives for setting it fields
  public static DynamicObject methodImplementing(final DynamicObject obj, ReflectiveOp operation){
    int field;
    switch (operation){
      case None: 
        return null;
      case MessageLookup: case MessageActivation: 
        field = Message_IDX;
        break;
      case ExecutorReadField: case ExecutorWriteField: case ExecutorReturn: 
      case ExecutorLocalArg: case ExecutorReadLocal: case ExecutorWriteLocal:
        field = Semantics_IDX;
        break;
      case LayoutReadField: case LayoutWriteField: 
        field = Layout_IDX;
        break;
      default:
        return null;
    }
    DynamicObject metaobject = (DynamicObject) obj.get(field);
    if (metaobject == Nil.nilObject) return null;
    return methodForOperation(metaobject, operation);
  }
  
  /*Optimize this method. It can have the definition of the symbols in a static ahead of time  way*/
  private static DynamicObject methodForOperation(DynamicObject metaobject, ReflectiveOp operation){
    SSymbol selector; 
    switch (operation){
      case MessageLookup:
        selector = Universe.current().symbolFor("find:since:");
        break;
      case MessageActivation:  
        selector = Universe.current().symbolFor("activate:withArguments:");
        break;
      case ExecutorReadField: 
        selector = Universe.current().symbolFor("read:");
        break;
      case ExecutorWriteField: 
        selector = Universe.current().symbolFor("write:value:");
        break;
      case ExecutorReturn: 
        selector = Universe.current().symbolFor("return:");
        break;
      case ExecutorLocalArg: 
        selector = Universe.current().symbolFor("localArgument:inFrame:");
        break;
      case ExecutorReadLocal: 
        selector = Universe.current().symbolFor("readLocal:inFrame:");
        break;
      case ExecutorWriteLocal:
        selector = Universe.current().symbolFor("writeLocal:inFrame:");
        break;
      case LayoutReadField: 
        selector = Universe.current().symbolFor("read:");
        break;
      case LayoutWriteField: 
        selector = Universe.current().symbolFor("write:value:");
        break;
        
      default:
        selector = null;
    }
    return SClass.lookupInvokable(SObject.getSOMClass(metaobject), selector);
  }
}