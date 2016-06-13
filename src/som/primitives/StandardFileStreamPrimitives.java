/*
 * Copyright (c) 2016 Guido Chari, gchari@dc.uba.ar
 */

package som.primitives;

import som.primitives.FilePluginPrimsFactory.AtEndFilePrimFactory;
import som.primitives.FilePluginPrimsFactory.GetPositionFilePrimFactory;
import som.primitives.FilePluginPrimsFactory.OpenFilePrimFactory;
import som.primitives.FilePluginPrimsFactory.ReadIntoFilePrimFactory;
import som.primitives.FilePluginPrimsFactory.SetPositionFilePrimFactory;
import som.primitives.FilePluginPrimsFactory.SizeFilePrimFactory;


public final class StandardFileStreamPrimitives extends Primitives {
  public StandardFileStreamPrimitives(final boolean displayWarning) { super(displayWarning); }

  @Override
  public void installPrimitives() {
    installInstancePrimitive("primOpen:writable:", OpenFilePrimFactory.getInstance());
    installInstancePrimitive("primGetPosition:", GetPositionFilePrimFactory.getInstance());
    installInstancePrimitive("primSetPosition:to:", SetPositionFilePrimFactory.getInstance());
    installInstancePrimitive("primSize:", SizeFilePrimFactory.getInstance());
    installInstancePrimitive("primRead:into:startingAt:count:", ReadIntoFilePrimFactory.getInstance());
    installInstancePrimitive("primAtEnd:", AtEndFilePrimFactory.getInstance());
  }
}
