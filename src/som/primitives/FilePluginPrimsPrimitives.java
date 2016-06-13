package som.primitives;

import som.primitives.FilePluginPrimsFactory.ImageFilePrimFactory;


public final class FilePluginPrimsPrimitives extends Primitives {
  public FilePluginPrimsPrimitives(final boolean displayWarning) { super(displayWarning); }

  @Override
  public void installPrimitives() {
    installInstancePrimitive("imageFile",   ImageFilePrimFactory.getInstance());
  }
}
