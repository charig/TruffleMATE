package som.interpreter;

import java.io.IOException;

import som.vm.MateUniverse;
import som.vm.NotYetImplementedException;
import tools.highlight.Tags.ArgumentTag;
import tools.highlight.Tags.CommentTag;
import tools.highlight.Tags.DelimiterClosingTag;
import tools.highlight.Tags.DelimiterOpeningTag;
import tools.highlight.Tags.IdentifierTag;
import tools.highlight.Tags.KeywordTag;
import tools.highlight.Tags.LiteralTag;
import tools.highlight.Tags.LocalVariableTag;
import tools.highlight.Tags.StatementSeparatorTag;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags.CallTag;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;

@TruffleLanguage.Registration(name = "TruffleMate", version = "0.1.0", mimeType = SomLanguage.MIME_TYPE)
@ProvidedTags({RootTag.class, StatementTag.class, CallTag.class,
  KeywordTag.class, LiteralTag.class,
  CommentTag.class, IdentifierTag.class, ArgumentTag.class,
  LocalVariableTag.class, StatementSeparatorTag.class,
  DelimiterOpeningTag.class, DelimiterClosingTag.class})
public class SomLanguage extends TruffleLanguage<MateUniverse> {

  public static final String MIME_TYPE = "application/x-mate-som";

  public static final SomLanguage INSTANCE = new SomLanguage();

  @Override
  protected MateUniverse createContext(final Env env) {
    throw new NotYetImplementedException();
  }

  @Override
  protected CallTarget parse(final Source code, final Node context,
      final String... argumentNames) throws IOException {
    throw new NotYetImplementedException();
  }

  @Override
  protected Object findExportedSymbol(final MateUniverse context,
      final String globalName, final boolean onlyExplicit) {
    throw new NotYetImplementedException();
  }

  @Override
  protected Object getLanguageGlobal(final MateUniverse context) {
    throw new NotYetImplementedException();
  }

  @Override
  protected boolean isObjectOfLanguage(final Object object) {
    throw new NotYetImplementedException();
  }

  @Override
  protected boolean isInstrumentable(final Node node) {
    throw new NotYetImplementedException();
  }

  @Override
  protected Object evalInContext(final Source source, final Node node,
      final MaterializedFrame mFrame) throws IOException {
    throw new NotYetImplementedException();
  }
}
