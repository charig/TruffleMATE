package som.mateTests;

import som.tests.SomTests;
import som.vm.MateUniverse;
import som.vm.Universe;

public class MateSOMTests extends SomTests {

  public MateSOMTests(String testName) {
    super(testName);
  }
  
  @Override
  protected String[] getArguments(){
    String[] args = {"-activateMate", "-cp", "Smalltalk:Smalltalk/Mate:Smalltalk/Mate/MOP:Smalltalk/FileSystem/Core:Smalltalk/FileSystem/Disk:Smalltalk/FileSystem/Streams:Smalltalk/FileSystem/Directories:Smalltalk/Collections/Streams::Smalltalk/Languages:TestSuite/FileSystem", "TestSuite/TestHarness.som", testName};
    return args;
  }
  
  static{
    if (!(Universe.getCurrent() instanceof MateUniverse)){
      Universe.setCurrent(new MateUniverse());
    }
    SomTests.u = Universe.current();
  }
}
