/**
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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;

import som.compiler.Parser.ParseError;
import som.interpreter.SomLanguage;
import som.vm.ObjectMemory;
import som.vm.Universe;
import som.vmobjects.SClass;
import som.vmobjects.SSymbol;
import tools.language.StructuralProbe;

public final class SourcecodeCompiler {
  private final SomLanguage language;

  public SourcecodeCompiler(final SomLanguage language) {
    this.language = language;
  }

  public SomLanguage getLanguage() { return language; }

  @TruffleBoundary
  public DynamicObject compileClass(final Source source, final DynamicObject systemClass,
      final ObjectMemory memory, final StructuralProbe structuralProbe) throws ParseError {

    Parser parser = new Parser(source.getCharacters().toString(), source.getLength(), source, memory, structuralProbe, language);

    DynamicObject result = compile(parser, systemClass, memory, structuralProbe);

    SSymbol cname = SClass.getName(result);
    String cnameC = cname.getString();

    if (source.getName() != cnameC) {
      throw new IllegalStateException("File name " + source.getName()
          + " does not match class name " + cnameC);
    }

    return result;
  }

  @TruffleBoundary
  public DynamicObject compileClass(final String stmt,
      final DynamicObject systemClass, final ObjectMemory memory, final StructuralProbe structuralProbe) throws ParseError {
    Parser parser = new Parser(stmt, stmt.length(), null, memory, structuralProbe, language);

    DynamicObject result = compile(parser, systemClass, memory, structuralProbe);
    return result;
  }

  private static DynamicObject compile(final Parser parser,
      final DynamicObject systemClass, final ObjectMemory memory, final StructuralProbe structuralProbe) {
    ClassGenerationContext cgc = new ClassGenerationContext();

    DynamicObject result = systemClass;
    try {
      parser.classdef(cgc);
    } catch (ParseError pe) {
      Universe.errorExit(pe.toString());
    }

    if (systemClass == null) {
      result = cgc.assemble();
    } else {
      cgc.assembleSystemClass(result);
    }
    if (structuralProbe != null) {
      structuralProbe.recordNewClass(result);
    }
    return result;
  }
}
