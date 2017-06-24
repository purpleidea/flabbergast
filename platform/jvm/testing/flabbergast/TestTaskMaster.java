package flabbergast;

public class TestTaskMaster extends TaskMaster {
  @Override
  public void reportExternalError(String uri, LibraryFailure failure) {}

  @Override
  public void reportOtherError(SourceReference reference, String message) {}
}
