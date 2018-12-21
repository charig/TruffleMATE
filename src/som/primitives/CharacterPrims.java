package som.primitives;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;

import bd.primitives.Primitive;
import bd.primitives.Specializer;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.nary.BinaryExpressionNode;
import som.interpreter.nodes.nary.UnaryExpressionNode;
import som.vm.Universe;
import som.vm.constants.Classes;
import som.vmobjects.SSymbol;
import tools.dym.Tags.StringAccess;

public class CharacterPrims {

  @GenerateNodeFactory
  @Primitive(className = "Character class", primitive = "new:",
      specializer = NewCharPrim.IsCharacterClass.class)
  // No specialization to avoid clash with new: from Arrays
  public abstract static class NewCharPrim extends BinaryExpressionNode {

    @Specialization
    public final Character doCreate(final DynamicObject clazz, final long value) {
      return (char) value;
    }

    public static class IsCharacterClass extends Specializer<Universe, ExpressionNode, SSymbol> {
      public IsCharacterClass(final Primitive prim, final NodeFactory<ExpressionNode> fact) {
        super(prim, fact);
      }

      @Override
      public boolean matches(final Object[] args, final ExpressionNode[] argNodes) {
        return receiverIsCharacterClass((DynamicObject) args[0]);
      }

      protected static final boolean receiverIsCharacterClass(final DynamicObject receiver) {
        return receiver == Classes.characterClass;
      }
    }


    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Character", primitive = "asInteger")
  public abstract static class AsIntegerCharPrim extends UnaryExpressionNode {
    @Specialization
    public final long doCharacter(final char subject) {
      return subject;
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Character", primitive = "isDigit", selector = "isDigit")
  public abstract static class IsDigitCharPrim extends UnaryExpressionNode {
    @Specialization
    public final boolean doCharacter(final char subject) {
      return Character.isDigit(subject);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Character", primitive = "asDigit", selector = "asDigit")
  public abstract static class AsDigitCharPrim extends UnaryExpressionNode {
    @Specialization
    public final long doCharacter(final char subject) {
      return Character.getNumericValue(subject);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Character", primitive = "isLetter", selector = "isLetter")
  public abstract static class IsLetterCharPrim extends UnaryExpressionNode {
    @Specialization
    public final boolean doCharacter(final char subject) {
      return Character.isLetter(subject);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Character", primitive = "isAlphaNumeric", selector = "isAlphaNumeric")
  public abstract static class IsAlphaNumericCharPrim extends UnaryExpressionNode {
    @Specialization
    public final boolean doCharacter(final char subject) {
      return Character.isLetterOrDigit(subject);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Character", primitive = "asUppercase", selector = "asUppercase")
  public abstract static class AsUppercaseCharPrim extends UnaryExpressionNode {
    @Specialization
    public final char doCharacter(final char subject) {
      return Character.toUpperCase(subject);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Character", primitive = "isUppercase", selector = "isUppercase")
  public abstract static class IsUppercaseCharPrim extends UnaryExpressionNode {
    @Specialization
    public final boolean doCharacter(final char subject) {
      return Character.isUpperCase(subject);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Character", primitive = "asLowercase", selector = "asLowercase")
  public abstract static class AsLowercaseCharPrim extends UnaryExpressionNode {
    @Specialization
    public final char doCharacter(final char subject) {
      return Character.toLowerCase(subject);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Character", primitive = "isLowercase", selector = "isLowercase")
  public abstract static class IsLowercaseCharPrim extends UnaryExpressionNode {
    @Specialization
    public final boolean doCharacter(final char subject) {
      return Character.isLowerCase(subject);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }

  @GenerateNodeFactory
  @Primitive(className = "Character", primitive = "compareWith:")
  public abstract static class CompareCharsPrim extends BinaryExpressionNode {
    @Specialization
    public final long doCharacter(final char receiver, final char param) {
      return Character.compare(receiver, param);
    }

    @Override
    protected boolean hasTagIgnoringEagerness(final Class<? extends Tag> tag) {
      if (tag == StringAccess.class) {
        return true;
      } else {
        return super.hasTagIgnoringEagerness(tag);
      }
    }
  }
}
