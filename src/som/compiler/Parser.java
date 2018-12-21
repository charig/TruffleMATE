/**
 * Copyright (c) 2013 Stefan Marr,   stefan.marr@vub.ac.be
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
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

package som.compiler;

import static som.compiler.Symbol.And;
import static som.compiler.Symbol.Assign;
import static som.compiler.Symbol.At;
import static som.compiler.Symbol.Colon;
import static som.compiler.Symbol.Comma;
import static som.compiler.Symbol.Div;
import static som.compiler.Symbol.Double;
import static som.compiler.Symbol.EndBlock;
import static som.compiler.Symbol.EndTerm;
import static som.compiler.Symbol.Equal;
import static som.compiler.Symbol.Exit;
import static som.compiler.Symbol.Identifier;
import static som.compiler.Symbol.Integer;
import static som.compiler.Symbol.Keyword;
import static som.compiler.Symbol.KeywordSequence;
import static som.compiler.Symbol.Less;
import static som.compiler.Symbol.Minus;
import static som.compiler.Symbol.Mod;
import static som.compiler.Symbol.More;
import static som.compiler.Symbol.NONE;
import static som.compiler.Symbol.NewBlock;
import static som.compiler.Symbol.NewTerm;
import static som.compiler.Symbol.Not;
import static som.compiler.Symbol.OperatorSequence;
import static som.compiler.Symbol.Or;
import static som.compiler.Symbol.Per;
import static som.compiler.Symbol.Period;
import static som.compiler.Symbol.Plus;
import static som.compiler.Symbol.Pound;
import static som.compiler.Symbol.Primitive;
import static som.compiler.Symbol.STChar;
import static som.compiler.Symbol.STString;
import static som.compiler.Symbol.SemiColon;
import static som.compiler.Symbol.Separator;
import static som.compiler.Symbol.Star;
import static som.interpreter.SNodeFactory.createCascadeMessageSend;
import static som.interpreter.SNodeFactory.createGlobalRead;
import static som.interpreter.SNodeFactory.createMessageSend;
import static som.interpreter.SNodeFactory.createSequence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import som.VmSettings;
import som.compiler.Lexer.SourceCoordinate;
import som.compiler.Variable.Local;
import som.interpreter.SomLanguage;
import som.interpreter.nodes.ExpressionNode;
import som.interpreter.nodes.FieldNode.FieldReadNode;
import som.interpreter.nodes.FieldNode.FieldWriteNode;
import som.interpreter.nodes.MessageSendNode.AbstractMessageSendNode;
import som.interpreter.nodes.MessageSendNode.CascadeMessageSendNode;
import som.interpreter.nodes.literals.ArrayLiteralNode;
import som.interpreter.nodes.literals.BigIntegerLiteralNode;
import som.interpreter.nodes.literals.BlockNode;
import som.interpreter.nodes.literals.BlockNode.BlockNodeWithContext;
import som.interpreter.nodes.literals.CharLiteralNode;
import som.interpreter.nodes.literals.DoubleLiteralNode;
import som.interpreter.nodes.literals.IntegerLiteralNode;
import som.interpreter.nodes.literals.LiteralNode;
import som.interpreter.nodes.literals.StringLiteralNode;
import som.interpreter.nodes.literals.SymbolLiteralNode;
import som.interpreter.nodes.nary.ExpressionWithTagsNode;
import som.interpreter.nodes.specialized.BooleanInlinedLiteralNode.AndInlinedLiteralNode;
import som.interpreter.nodes.specialized.BooleanInlinedLiteralNode.OrInlinedLiteralNode;
import som.interpreter.nodes.specialized.IfInlinedLiteralNode;
import som.interpreter.nodes.specialized.IfTrueIfFalseInlinedLiteralsNode;
import som.interpreter.nodes.specialized.IntToDoInlinedLiteralsNodeGen;
import som.interpreter.nodes.specialized.whileloops.WhileInlinedLiteralsNode;
import som.vm.ObjectMemory;
import som.vm.Symbols;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SArray;
import som.vmobjects.SClass;
import som.vmobjects.SObject;
import som.vmobjects.SSymbol;
import tools.debugger.Tags;
import tools.debugger.Tags.DelimiterClosingTag;
import tools.debugger.Tags.DelimiterOpeningTag;
import tools.debugger.Tags.IdentifierTag;
import tools.debugger.Tags.KeywordTag;
import tools.debugger.Tags.StatementSeparatorTag;
import tools.language.StructuralProbe;

public class Parser {
  protected final ObjectMemory      objectMemory;
  private final Lexer               lexer;
  private final Source              source;

  private Symbol                    sym;
  private String                    text;
  private Symbol                    nextSym;

  private SourceSection             lastMethodsSourceSection;
  private final StructuralProbe     structuralProbe;

  private static final List<Symbol> singleOpSyms        = new ArrayList<Symbol>();
  private static final List<Symbol> binaryOpSyms        = new ArrayList<Symbol>();
  private static final List<Symbol> keywordSelectorSyms = new ArrayList<Symbol>();

  private final SomLanguage language;

  static {
    for (Symbol s : new Symbol[] {Not, And, Or, Star, Div, Mod, Plus, Equal,
        More, Less, Comma, Minus, At, Per, NONE}) {
      singleOpSyms.add(s);
    }
    for (Symbol s : new Symbol[] {Or, Comma, Minus, Equal, Not, And, Or, Star,
        Div, Mod, Plus, Equal, More, Less, Comma, At, Per, NONE}) {
      binaryOpSyms.add(s);
    }
    for (Symbol s : new Symbol[] {Keyword, KeywordSequence}) {
      keywordSelectorSyms.add(s);
    }
  }

  @Override
  public String toString() {
    return "Parser(" + source.getName() + ", " + this.getCoordinate().toString() + ")";
  }

  public static class ParseError extends Exception {
    private static final long serialVersionUID = 425390202979033628L;
    private final String message;
    private final SourceCoordinate sourceCoordinate;
    private final String text;
    private final String rawBuffer;
    private final String fileName;
    private final Symbol expected;
    private final Symbol found;

    ParseError(final String message, final Symbol expected, final Parser parser) {
      this.message = message;
      this.text             = parser.text;
      if (parser.lexer == null) {
        this.sourceCoordinate = new SourceCoordinate(0, 0, 0, 0);
        this.rawBuffer = "";
      } else {
        this.sourceCoordinate = parser.getCoordinate();
        this.rawBuffer = new String(parser.lexer.getCurrentLine());
      }
      this.fileName         = parser.source.getName();
      this.expected         = expected;
      this.found            = parser.sym;
    }

    protected String expectedSymbolAsString() {
      return expected.toString();
    }

    @Override
    public String toString() {
      String msg = "%(file)s:%(line)d:%(column)d: error: " + message;
      String foundStr;
      if (Parser.printableSymbol(found)) {
        foundStr = found + " (" + text + ")";
      } else {
        foundStr = found.toString();
      }
      msg += ": " + rawBuffer;
      String expectedStr = expectedSymbolAsString();

      msg = msg.replace("%(file)s",     fileName);
      msg = msg.replace("%(line)d",     java.lang.Integer.toString(sourceCoordinate.startLine));
      msg = msg.replace("%(column)d",   java.lang.Integer.toString(sourceCoordinate.startColumn));
      msg = msg.replace("%(expected)s", expectedStr);
      msg = msg.replace("%(found)s",    foundStr);
      return msg;
    }
  }

  public static class ParseErrorWithSymbolList extends ParseError {
    private static final long serialVersionUID = 561313162441723955L;
    private final List<Symbol> expectedSymbols;

    ParseErrorWithSymbolList(final String message, final List<Symbol> expected,
        final Parser parser) {
      super(message, null, parser);
      this.expectedSymbols = expected;
    }

    @Override
    protected String expectedSymbolAsString() {
        StringBuilder sb = new StringBuilder();
        String deliminator = "";

        for (Symbol s : expectedSymbols) {
            sb.append(deliminator);
            sb.append(s);
            deliminator = ", ";
        }
        return sb.toString();
    }
  }

  public Parser(final String content, final long fileSize, final Source source,
      final ObjectMemory memory, final StructuralProbe structuralProbe, final SomLanguage language) throws ParseError {
    this.objectMemory = memory;
    this.source   = source;

    sym = NONE;
    nextSym = NONE;

    if (fileSize == 0) {
      throw new ParseError("Provided file is empty.", NONE, this);
    }

    lexer = new Lexer(content);
    getSymbolFromLexer();
    this.structuralProbe = structuralProbe;
    this.language = language;
  }

  private SourceCoordinate getCoordinate() {
    return lexer.getStartCoordinate();
  }

  public void classdef(final ClassGenerationContext cgenc) throws ParseError {
    cgenc.setName(Symbols.symbolFor(text));
    expect(Identifier, IdentifierTag.class);
    expect(Equal, KeywordTag.class);

    superclass(cgenc);

    expect(NewTerm, null);
    instanceFields(cgenc);

    while (isIdentifier(sym) || sym == Keyword || sym == OperatorSequence
        || symIn(binaryOpSyms)) {
      MethodGenerationContext mgenc = new MethodGenerationContext(cgenc, language);

      ExpressionWithTagsNode methodBody = method(mgenc);
      DynamicObject method = mgenc.assemble(methodBody, lastMethodsSourceSection);
      cgenc.addInstanceMethod(method);
      if (structuralProbe != null) {
        structuralProbe.recordNewMethod(method);
      }
    }

    if (accept(Separator, StatementSeparatorTag.class)) {
      cgenc.setClassSide(true);
      classFields(cgenc);
      while (isIdentifier(sym) || sym == Keyword || sym == OperatorSequence
          || symIn(binaryOpSyms)) {
        MethodGenerationContext mgenc = new MethodGenerationContext(cgenc, language);

        ExpressionWithTagsNode methodBody = method(mgenc);
        DynamicObject method = mgenc.assemble(methodBody, lastMethodsSourceSection);
        cgenc.addClassMethod(method);
        if (structuralProbe != null) {
          structuralProbe.recordNewMethod(method);
        }
      }
    }
    expect(EndTerm, null);
  }

  private void superclass(final ClassGenerationContext cgenc) throws ParseError {
    SSymbol superName;
    if (sym == Identifier) {
      superName = Symbols.symbolFor(text);
      accept(Identifier, KeywordTag.class);
    } else {
      superName = Symbols.symbolFor("Object");
    }
    cgenc.setSuperName(superName);

    // Load the super class, if it is not nil (break the dependency cycle)
    if (!superName.getString().equals("nil")) {
      DynamicObject superClass = Universe.getCurrent().loadClass(superName);
      if (superClass == null) {
        throw new ParseError("Super class " + superName.getString() +
            " could not be loaded", NONE, this);
      }

      cgenc.setInstanceFieldsOfSuper(SClass.getInstanceFields(superClass));
      cgenc.setClassFieldsOfSuper(SClass.getInstanceFields(SObject.getSOMClass(superClass)));
    }
  }

  private boolean symIn(final List<Symbol> ss) {
    return ss.contains(sym);
  }

  private boolean accept(final Symbol s, final Class<? extends Tags> tag) {
    if (sym == s) {
      SourceCoordinate coord = tag == null ? null : getCoordinate();
      getSymbolFromLexer();
      if (tag != null) {
        Universe.reportSyntaxElement(tag, getSource(coord, 0));
      }
      return true;
    }
    return false;
  }

  private boolean acceptOneOf(final List<Symbol> ss) {
    if (symIn(ss)) {
      getSymbolFromLexer();
      return true;
    }
    return false;
  }

  private void expect(final Symbol s, final String msg,
      final Class<? extends Tags> tag) throws ParseError {
    if (accept(s, tag)) { return; }

    throw new ParseError(msg, s, this);
  }

  private void expect(final Symbol s, final Class<? extends Tags> tag) throws ParseError {
    expect(s, "Unexpected symbol. Expected %(expected)s, but found %(found)s", tag);
  }

  private boolean expectOneOf(final List<Symbol> ss) throws ParseError {
    if (acceptOneOf(ss)) { return true; }

    throw new ParseErrorWithSymbolList("Unexpected symbol. Expected one of " +
        "%(expected)s, but found %(found)s", ss, this);
  }

  private void instanceFields(final ClassGenerationContext cgenc) throws ParseError {
    if (accept(Or, DelimiterOpeningTag.class)) {
      while (isIdentifier(sym)) {
        String var = variable();
        cgenc.addInstanceField(Symbols.symbolFor(var));
      }
      expect(Or, DelimiterClosingTag.class);
    }
  }

  private void classFields(final ClassGenerationContext cgenc) throws ParseError {
    if (accept(Or, DelimiterOpeningTag.class)) {
      while (isIdentifier(sym)) {
        String var = variable();
        cgenc.addClassField(Symbols.symbolFor(var));
      }
      expect(Or, DelimiterClosingTag.class);
    }
  }

  private SourceSection getSource(final SourceCoordinate coord, final int extraChars) {
    assert lexer.getNumberOfCharactersRead() - coord.charIndex >= 0;
    int line = coord.startLine == 0 ? 1 : coord.startLine;
    int column = coord.startColumn == 0 ? 1 : coord.startColumn;
    while (source.getLineLength(line) < column) {
      line += 1;
      column = 1;
    }
    return source.createSection(line, column,
        lexer.getNumberOfCharactersRead() - coord.charIndex + extraChars);
  }

  private ExpressionWithTagsNode method(final MethodGenerationContext mgenc) throws ParseError {
    SourceCoordinate coord = getCoordinate();
    pattern(mgenc);
    expect(Equal, KeywordTag.class);
    if (sym == Primitive) {
      mgenc.markAsPrimitive();
      primitiveBlock();
      return null;
    } else {
      return methodBlock(mgenc, coord);
    }
  }

  private void primitiveBlock() throws ParseError {
    expect(Primitive, KeywordTag.class);
  }

  private void pattern(final MethodGenerationContext mgenc) throws ParseError {
    mgenc.addArgumentIfAbsent("self"); // TODO: can we do that optionally?
    switch (sym) {
      case Identifier:
      case Primitive:
        unaryPattern(mgenc);
        break;
      case Keyword:
        keywordPattern(mgenc);
        break;
      default:
        binaryPattern(mgenc);
        break;
    }
  }

  private void unaryPattern(final MethodGenerationContext mgenc) throws ParseError {
    mgenc.setSignature(unarySelector());
  }

  private void binaryPattern(final MethodGenerationContext mgenc) throws ParseError {
    mgenc.setSignature(binarySelector());
    mgenc.addArgumentIfAbsent(argument());
  }

  private void keywordPattern(final MethodGenerationContext mgenc) throws ParseError {
    StringBuffer kw = new StringBuffer();
    do {
      kw.append(keyword());
      mgenc.addArgumentIfAbsent(argument());
    }
    while (sym == Keyword);

    mgenc.setSignature(Symbols.symbolFor(kw.toString()));
  }

  private ExpressionWithTagsNode methodBlock(final MethodGenerationContext mgenc, final SourceCoordinate coord) throws ParseError {
    expect(NewTerm, null);
    ExpressionWithTagsNode methodBody = blockContents(mgenc);
    lastMethodsSourceSection = getSource(coord, 1);
    expect(EndTerm, null);
    return methodBody;
  }

  private SSymbol unarySelector() throws ParseError {
    return Symbols.symbolFor(identifier());
  }

  private SSymbol binarySelector() throws ParseError {
    String s = new String(text);

    // Checkstyle: stop
    if (accept(Or, null)) {
    } else if (accept(Comma, null)) {
    } else if (accept(Minus, null)) {
    } else if (accept(Equal, null)) {
    } else if (acceptOneOf(singleOpSyms)) {
    } else if (accept(OperatorSequence, null)) {
    } else { expect(NONE, null); }
    // Checkstyle: resume

    return Symbols.symbolFor(s);
  }

  private String identifier() throws ParseError {
    String s = new String(text);
    boolean isPrimitive = accept(Primitive, KeywordTag.class);
    if (!isPrimitive) {
      expect(Identifier, null);
    }
    return s;
  }

  private String keyword() throws ParseError {
    String s = new String(text);
    expect(Keyword, null);
    return s;
  }

  private String argument() throws ParseError {
    return variable();
  }

  private ExpressionWithTagsNode blockContents(final MethodGenerationContext mgenc) throws ParseError {
    if (accept(Or, DelimiterOpeningTag.class)) {
      locals(mgenc);
      expect(Or, DelimiterClosingTag.class);
    }
    return blockBody(mgenc);
  }

  private void locals(final MethodGenerationContext mgenc) throws ParseError {
    while (isIdentifier(sym)) {
      mgenc.addLocalIfAbsent(variable());
    }
  }

  private ExpressionWithTagsNode blockBody(final MethodGenerationContext mgenc) throws ParseError {
    SourceCoordinate coord = getCoordinate();
    List<ExpressionWithTagsNode> expressions = new ArrayList<ExpressionWithTagsNode>();

    while (true) {
      if (accept(Exit, KeywordTag.class)) {
        expressions.add(result(mgenc));
        return createSequenceNode(coord, expressions);
      } else if (sym == EndBlock) {
        return createSequenceNode(coord, expressions);
      } else if (sym == EndTerm) {
        // the end of the method has been found (EndTerm) - make it implicitly
        // return "self"
        ExpressionWithTagsNode self = variableRead(mgenc, "self", getSource(getCoordinate(), 0));
        expressions.add(self);
        return createSequenceNode(coord, expressions);
      }

      expressions.add(expression(mgenc));
      accept(Period, StatementSeparatorTag.class);
    }
  }

  private ExpressionWithTagsNode createSequenceNode(final SourceCoordinate coord,
      final List<ExpressionWithTagsNode> expressions) {
    if (expressions.size() == 0) {
      return createGlobalRead("nil", getSource(coord, 0));
    } else if (expressions.size() == 1)  {
      return expressions.get(0);
    }
    return createSequence(expressions, getSource(coord, 0));
  }

  private ExpressionWithTagsNode result(final MethodGenerationContext mgenc) throws ParseError {
    SourceCoordinate coord = getCoordinate();

    ExpressionWithTagsNode exp = expression(mgenc);
    accept(Period, StatementSeparatorTag.class);

    if (mgenc.isBlockMethod()) {
      return mgenc.getNonLocalReturn(exp, getSource(coord, 0));
    } else {
      return exp;
    }
  }

  private ExpressionWithTagsNode expression(final MethodGenerationContext mgenc) throws ParseError {
    peekForNextSymbolFromLexer();

    if (nextSym == Assign) {
      return assignation(mgenc);
    } else {
      return evaluation(mgenc);
    }
  }

  private ExpressionWithTagsNode assignation(final MethodGenerationContext mgenc) throws ParseError {
    return assignments(mgenc);
  }

  private ExpressionWithTagsNode assignments(final MethodGenerationContext mgenc) throws ParseError {
    SourceCoordinate coord = getCoordinate();

    if (!isIdentifier(sym)) {
      throw new ParseError("Assignments should always target variables or" +
                           " fields, but found instead a %(found)s",
                           Symbol.Identifier, this);
    }
    String variable = assignment();

    peekForNextSymbolFromLexer();

    ExpressionWithTagsNode value;
    if (nextSym == Assign) {
      value = assignments(mgenc);
    } else {
      value = evaluation(mgenc);
    }

    return variableWrite(mgenc, variable, value, getSource(coord, 0));
  }

  private String assignment() throws ParseError {
    String v = variable();
    expect(Assign, KeywordTag.class);
    return v;
  }

  private CascadeMessageSendNode cascadeMessages(final MethodGenerationContext mgenc,
      final ExpressionWithTagsNode firstMessage, final ExpressionWithTagsNode receiver,
      final SourceCoordinate coord, final SourceSection section) throws ParseError {
    List<ExpressionWithTagsNode> expressions = new ArrayList<ExpressionWithTagsNode>();
    expressions.add(firstMessage);
    while (accept(SemiColon, StatementSeparatorTag.class)) {
      ExpressionWithTagsNode message = messages(mgenc, receiver);
      expressions.add(message);
    }
    return createCascadeMessageSend(receiver, expressions, section);
  }

  private ExpressionWithTagsNode evaluation(final MethodGenerationContext mgenc) throws ParseError {
    ExpressionWithTagsNode exp = primary(mgenc);
    if (isIdentifier(sym) || sym == Keyword || sym == OperatorSequence
        || symIn(binaryOpSyms)) {
      ExpressionWithTagsNode receiver = exp;
      SourceCoordinate coord = getCoordinate();
      SourceSection section = getSource(coord, 0);

      exp = messages(mgenc, exp);

      if (SemiColon == sym) {
        return cascadeMessages(mgenc, exp, receiver, coord, section);
      }
    }
    return exp;
  }

  private ExpressionWithTagsNode primary(final MethodGenerationContext mgenc) throws ParseError {
    switch (sym) {
      case Identifier:
      case Primitive: {
        SourceCoordinate coord = getCoordinate();
        String v = variable();
        return variableRead(mgenc, v, getSource(coord, 0));
      }
      case NewTerm: {
        return nestedTerm(mgenc);
      }
      case NewBlock: {
        SourceCoordinate coord = getCoordinate();
        MethodGenerationContext bgenc = new MethodGenerationContext(mgenc.getHolder(), mgenc, language);

        ExpressionWithTagsNode blockBody = nestedBlock(bgenc);

        DynamicObject blockMethod = bgenc.assemble(blockBody, lastMethodsSourceSection);
        mgenc.addEmbeddedBlockMethod(blockMethod);

        if (bgenc.requiresContext()) {
          return new BlockNodeWithContext(blockMethod).initialize(getSource(coord, 0));
        } else {
          return new BlockNode(blockMethod).initialize(getSource(coord, 0));
        }
      }
      default: {
        return literal();
      }
    }
  }

  private String variable() throws ParseError {
    return identifier();
  }

  private ExpressionWithTagsNode messages(final MethodGenerationContext mgenc,
      final ExpressionWithTagsNode receiver) throws ParseError {
    ExpressionWithTagsNode msg;
    if (isIdentifier(sym)) {
      msg = unaryMessage(receiver);

      while (isIdentifier(sym)) {
        msg = unaryMessage(msg);
      }

      while (sym == OperatorSequence || symIn(binaryOpSyms)) {
        msg = binaryMessage(mgenc, msg);
      }

      if (sym == Keyword) {
        msg = keywordMessage(mgenc, msg);
      }
    } else if (sym == OperatorSequence || symIn(binaryOpSyms)) {
      msg = binaryMessage(mgenc, receiver);

      while (sym == OperatorSequence || symIn(binaryOpSyms)) {
        msg = binaryMessage(mgenc, msg);
      }

      if (sym == Keyword) {
        msg = keywordMessage(mgenc, msg);
      }
    } else {
      msg = keywordMessage(mgenc, receiver);
    }
    return msg;
  }

  private AbstractMessageSendNode unaryMessage(final ExpressionNode receiver) throws ParseError {
    SourceCoordinate coord = getCoordinate();
    SSymbol selector = unarySelector();
    return createMessageSend(selector, new ExpressionNode[] {receiver},
        getSource(coord, 0));
  }

  private AbstractMessageSendNode binaryMessage(final MethodGenerationContext mgenc,
      final ExpressionNode receiver) throws ParseError {
    SourceCoordinate coord = getCoordinate();
    SSymbol msg = binarySelector();
    ExpressionNode operand = binaryOperand(mgenc);

    return createMessageSend(msg, new ExpressionNode[] {receiver, operand},
        getSource(coord, 0));
  }

  private ExpressionWithTagsNode binaryOperand(final MethodGenerationContext mgenc) throws ParseError {
    ExpressionWithTagsNode operand = primary(mgenc);

    // a binary operand can receive unaryMessages
    // Example: 2 * 3 asString
    //   is evaluated as 2 * (3 asString)
    while (isIdentifier(sym)) {
      operand = unaryMessage(operand);
    }
    return operand;
  }

  private ExpressionWithTagsNode keywordMessage(final MethodGenerationContext mgenc,
      final ExpressionWithTagsNode receiver) throws ParseError {
    SourceCoordinate coord = getCoordinate();
    List<ExpressionWithTagsNode> arguments = new ArrayList<ExpressionWithTagsNode>();
    StringBuffer         kw        = new StringBuffer();

    arguments.add(receiver);

    do {
      kw.append(keyword());
      arguments.add(formula(mgenc));
    }
    while (sym == Keyword);

    String msgStr = kw.toString();
    SSymbol msg = Symbols.symbolFor(msgStr);

    SourceSection source = getSource(coord, 0);

    if (msg.getNumberOfSignatureArguments() == 2) {
      if (arguments.get(1) instanceof LiteralNode) {
        if ("ifTrue:".equals(msgStr)) {
          ExpressionNode condition = arguments.get(0);
          if (condition.getSourceSection() == null) {
            int i = 1;
          }
          condition.markAsControlFlowCondition();
          ExpressionNode inlinedBody = ((LiteralNode) arguments.get(1)).inline(mgenc);
          return new IfInlinedLiteralNode(condition, true, inlinedBody,
              arguments.get(1)).initialize(source);
        } else if ("ifFalse:".equals(msgStr)) {
          ExpressionNode condition = arguments.get(0);
          condition.markAsControlFlowCondition();
          ExpressionNode inlinedBody = ((LiteralNode) arguments.get(1)).inline(mgenc);
          return new IfInlinedLiteralNode(condition, false, inlinedBody,
              arguments.get(1)).initialize(source);
        } else if ("whileTrue:".equals(msgStr)) {
          ExpressionNode inlinedCondition = ((LiteralNode) arguments.get(0)).inline(mgenc);
          inlinedCondition.markAsControlFlowCondition();
          ExpressionNode inlinedBody      = ((LiteralNode) arguments.get(1)).inline(mgenc);
          inlinedBody.markAsLoopBody();
          return new WhileInlinedLiteralsNode(inlinedCondition, inlinedBody,
              true, arguments.get(0), arguments.get(1)).initialize(source);
        } else if ("whileFalse:".equals(msgStr)) {
          ExpressionNode inlinedCondition = ((LiteralNode) arguments.get(0)).inline(mgenc);
          inlinedCondition.markAsControlFlowCondition();
          ExpressionNode inlinedBody      = ((LiteralNode) arguments.get(1)).inline(mgenc);
          inlinedBody.markAsLoopBody();
          return new WhileInlinedLiteralsNode(inlinedCondition, inlinedBody,
              false, arguments.get(0), arguments.get(1)).initialize(source);
        } else if ("or:".equals(msgStr) || "||".equals(msgStr)) {
          ExpressionNode inlinedArg = ((LiteralNode) arguments.get(1)).inline(mgenc);
          return new OrInlinedLiteralNode(arguments.get(0), inlinedArg, arguments.get(1)).initialize(source);
        } else if ("and:".equals(msgStr) || "&&".equals(msgStr)) {
          ExpressionNode inlinedArg = ((LiteralNode) arguments.get(1)).inline(mgenc);
          return new AndInlinedLiteralNode(arguments.get(0), inlinedArg, arguments.get(1)).initialize(source);
        }
      }
    } else if (msg.getNumberOfSignatureArguments() == 3) {
      if ("ifTrue:ifFalse:".equals(msgStr) &&
          arguments.get(1) instanceof LiteralNode && arguments.get(2) instanceof LiteralNode) {
        ExpressionNode condition = arguments.get(0);
        condition.markAsControlFlowCondition();
        ExpressionNode inlinedTrueNode  = ((LiteralNode) arguments.get(1)).inline(mgenc);
        ExpressionNode inlinedFalseNode = ((LiteralNode) arguments.get(2)).inline(mgenc);
        return new IfTrueIfFalseInlinedLiteralsNode(condition,
            inlinedTrueNode, inlinedFalseNode, arguments.get(1), arguments.get(2)).initialize(source);
      } else if (!VmSettings.DYNAMIC_METRICS && "to:do:".equals(msgStr) &&
          arguments.get(2) instanceof LiteralNode) {
        Local loopIdx = mgenc.addLocal("i:" + source.getCharIndex());
        ExpressionNode inlinedBody = ((LiteralNode) arguments.get(2)).inline(mgenc, loopIdx);
        inlinedBody.markAsLoopBody();
        return IntToDoInlinedLiteralsNodeGen.create(inlinedBody, loopIdx.getSlot(),
            arguments.get(2), arguments.get(0), arguments.get(1)).initialize(source);
      }
    }

    return createMessageSend(msg, arguments.toArray(new ExpressionNode[0]),
        source);
  }

  private ExpressionWithTagsNode formula(final MethodGenerationContext mgenc) throws ParseError {
    ExpressionWithTagsNode operand = binaryOperand(mgenc);

    while (sym == OperatorSequence || symIn(binaryOpSyms)) {
      operand = binaryMessage(mgenc, operand);
    }
    return operand;
  }

  private ExpressionWithTagsNode nestedTerm(final MethodGenerationContext mgenc) throws ParseError {
    expect(NewTerm, DelimiterOpeningTag.class);
    ExpressionWithTagsNode exp = expression(mgenc);
    expect(EndTerm, DelimiterClosingTag.class);
    return exp;
  }

  private LiteralNode literal() throws ParseError {
    SourceCoordinate coord = getCoordinate();
    switch (sym) {
      case Pound:
        try { peekForNextSymbolFromLexer(); } catch (IllegalStateException e) { /*Come from a trace that already peeked*/ }
        if (nextSym == NewTerm) {
          expect(Pound, null);
          return new ArrayLiteralNode(this.literalArray()).initialize(getSource(coord, 0));
        } else {
          return new SymbolLiteralNode(literalSymbol()).initialize(getSource(coord, 0));
        }
      case STString:  return new StringLiteralNode(literalString()).initialize(getSource(coord, 0));
      case STChar:    return new CharLiteralNode(literalChar()).initialize(getSource(coord, 0));
      default:
        boolean isNegative = isNegativeNumber();
        if (sym == Integer) {
          long value = literalInteger(isNegative);
          if (value < Long.MIN_VALUE || value > Long.MAX_VALUE) {
            return new BigIntegerLiteralNode(BigInteger.valueOf(value)).initialize(getSource(coord, 0));
          } else {
            return new IntegerLiteralNode(value).initialize(getSource(coord, 0));
          }
        } else {
          assert sym == Double;
          return new DoubleLiteralNode(literalDouble(isNegative)).initialize(getSource(coord, 0));
        }
    }
  }

  private boolean isNegativeNumber() throws ParseError {
    boolean isNegative = false;
    if (sym == Minus) {
      expect(Minus, null);
      isNegative = true;
    }
    return isNegative;
  }
  private long literalInteger(final boolean isNegative) throws ParseError {
    try {
       long i = Long.parseLong(text);
       if (isNegative) {
         i = 0 - i;
       }
       expect(Integer, null);
       return i;
    } catch (NumberFormatException e) {
      throw new ParseError("Could not parse integer. Expected a number but " +
                           "got '" + text + "'", NONE, this);
    }
  }

  private double literalDouble(final boolean isNegative) throws ParseError {
    try {
      double d = java.lang.Double.parseDouble(text);
      if (isNegative) {
        d = 0.0 - d;
      }
      expect(Double, null);
      return d;
    } catch (NumberFormatException e) {
      throw new ParseError("Could not parse double. Expected a number but " +
          "got '" + text + "'", NONE, this);
    }
  }

  private SSymbol literalSymbol() throws ParseError {
    SSymbol symb;
    expect(Pound, null);
    if (sym == STString) {
      String s = string();
      symb = Symbols.symbolFor(s);
    } else {
      symb = selector();
    }
    return symb;
  }

  private SArray literalArray() throws ParseError {
    List<Object> literals = new ArrayList<Object>();
    expect(NewTerm, null);
    while (sym != EndTerm) {
      literals.add(this.getObjectForCurrentLiteral());
    }
    expect(EndTerm, null);
    return SArray.create(literals.toArray());
  }

  private Object getObjectForCurrentLiteral() throws ParseError {
    switch (sym) {
      case NewTerm:
        return this.literalArray();
      case Pound:
        try { this.peekForNextSymbolFromLexer(); } catch (IllegalStateException e) { /*Come from a trace that already peeked*/ }
        if (nextSym == NewTerm) {
          expect(Pound, null);
          return this.literalArray();
        } else {
          return literalSymbol();
        }
      case STString:
        return literalString();
      case STChar:
        return literalChar();
      case Integer:
        return literalInteger(isNegativeNumber());
      case Double:
        return literalDouble(isNegativeNumber());
      case Identifier:
        if (text.equals("nil")) {
          selector(); // Consume the text from the parser state
          return Nil.nilObject;
        } else if (text.equals("true")) {
          selector(); // Consume the text from the parser state
          return Universe.getCurrent().getTrueObject();
        } else if (text.equals("false")) {
          selector(); // Consume the text from the parser state
          return Universe.getCurrent().getFalseObject();
        }
        return selector();
      case OperatorSequence:
      case Keyword:
      case KeywordSequence:
        return selector();
      default:
        throw new ParseError("Could not parse literal array value", NONE, this);
    }
  }

  private String literalString() throws ParseError {
    return string();
  }

  private Character literalChar() throws ParseError {
    char value = text.charAt(0);
    expect(STChar, null);
    return value;
  }

  private SSymbol selector() throws ParseError {
    if (sym == OperatorSequence || symIn(singleOpSyms)) {
      return binarySelector();
    } else if (sym == Keyword || sym == KeywordSequence) {
      return keywordSelector();
    } else {
      return unarySelector();
    }
  }

  private SSymbol keywordSelector() throws ParseError {
    String s = new String(text);
    expectOneOf(keywordSelectorSyms);
    SSymbol symb = Symbols.symbolFor(s);
    return symb;
  }

  private String string() throws ParseError {
    String s = new String(text);
    expect(STString, null);
    return s;
  }

  private ExpressionWithTagsNode nestedBlock(final MethodGenerationContext mgenc) throws ParseError {
    SourceCoordinate coord = getCoordinate();
    expect(NewBlock, DelimiterOpeningTag.class);

    mgenc.addArgumentIfAbsent("$blockSelf");

    if (sym == Colon) {
      blockPattern(mgenc);
    }

    // generate Block signature
    String blockSig = "$blockMethod@" + lexer.getCurrentLineNumber() + "@" + lexer.getCurrentColumn();
    int argSize = mgenc.getNumberOfArguments();
    for (int i = 1; i < argSize; i++) {
      blockSig += ":";
    }

    mgenc.setSignature(Symbols.symbolFor(blockSig));

    ExpressionWithTagsNode expressions = blockContents(mgenc);

    lastMethodsSourceSection = getSource(coord, 0);

    expect(EndBlock, DelimiterClosingTag.class);
    return expressions;
  }

  private void blockPattern(final MethodGenerationContext mgenc) throws ParseError {
    blockArguments(mgenc);
    expect(Or, KeywordTag.class);
  }

  private void blockArguments(final MethodGenerationContext mgenc) throws ParseError {
    do {
      expect(Colon, KeywordTag.class);
      mgenc.addArgumentIfAbsent(argument());
    } while (sym == Colon);
  }

  private ExpressionWithTagsNode variableRead(final MethodGenerationContext mgenc,
                                      final String variableName,
                                      final SourceSection source) {
    // we need to handle super special here
    if ("super".equals(variableName)) {
      return mgenc.getSuperReadNode(source);
    }

    // we need to handle thisContext special here
    if ("thisContext".equals(variableName)) {
      return mgenc.getThisContextNode(source);
    }

    // now look up first local variables, or method arguments
    Variable variable = mgenc.getVariable(variableName);
    if (variable != null) {
      return mgenc.getLocalReadNode(variableName, source);
    }

    // then object fields
    SSymbol varName = Symbols.symbolFor(variableName);
    FieldReadNode fieldRead = mgenc.getObjectFieldRead(varName, source);

    if (fieldRead != null) {
      return fieldRead;
    }

    // and finally assume it is a global
    return mgenc.getGlobalRead(varName, source);
  }

  private ExpressionWithTagsNode variableWrite(final MethodGenerationContext mgenc,
      final String variableName, final ExpressionWithTagsNode exp, final SourceSection source) {
    Local variable = mgenc.getLocal(variableName);
    if (variable != null) {
      return mgenc.getLocalWriteNode(variableName, exp, source);
    }

    SSymbol fieldName = Symbols.symbolFor(variableName);
    FieldWriteNode fieldWrite = mgenc.getObjectFieldWrite(fieldName, exp, source);

    if (fieldWrite != null) {
      return fieldWrite;
    } else {
      throw new RuntimeException("Neither a variable nor a field found "
          + "in current scope that is named " + variableName + ". Arguments are read-only.");
    }
  }

  private void getSymbolFromLexer() {
    sym  = lexer.getSym();
    text = lexer.getText();
  }

  private void peekForNextSymbolFromLexer() {
    nextSym = lexer.peek();
  }

  private static boolean isIdentifier(final Symbol sym) {
    return sym == Identifier || sym == Primitive;
  }

  private static boolean printableSymbol(final Symbol sym) {
    return sym == Integer || sym == Double || sym.compareTo(STString) >= 0;
  }
}
