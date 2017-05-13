package flabbergast.lsp;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public final class Server implements LanguageServer, LanguageClientAware {

  public static void main(String[] args) throws InterruptedException, ExecutionException {
    var server = new Server();
    var launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);
    server.connect(launcher.getRemoteProxy());
    launcher.startListening().get();
  }

  private LanguageClient client;

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    var res = new InitializeResult();
    res.getCapabilities().setCompletionProvider(new CompletionOptions());
    res.getCapabilities().setCodeActionProvider(Boolean.TRUE);
    res.getCapabilities().setDefinitionProvider(Boolean.TRUE);
    res.getCapabilities().setHoverProvider(Boolean.TRUE);
    res.getCapabilities().setReferencesProvider(Boolean.TRUE);
    res.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);
    res.getCapabilities().setDocumentSymbolProvider(Boolean.TRUE);
    return CompletableFuture.supplyAsync(() -> res);
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void exit() {
    // TODO Auto-generated method stub

  }

  @Override
  public TextDocumentService getTextDocumentService() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    // TODO Auto-generated method stub
    return null;
  }
}
