using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Globalization;
using System.Linq;
using System.Reflection;
using System.Text.RegularExpressions;
using System.Threading;

namespace Flabbergast {
public delegate Future ComputeValue(
    TaskMaster task_master, SourceReference reference, Context context, Frame self, Frame container);

public delegate Future ComputeOverride(
    TaskMaster task_master, SourceReference reference, Context context, Frame self, Frame container,
    Future original);

/**
 * Delegate for the callback from a computation.
 */

public delegate void ConsumeResult(Object result);

/**
 * A generic computation to be worked on by the TaskMaster.
 */

public abstract class Future {
    /**
    * The delegate(s) to be invoked when the computation is complete.
    */
    private ConsumeResult consumer;
    /**
    * The return value of the computation.
    *
    * This should be assigned by the subclass.
    */
    protected object result;

    protected readonly TaskMaster task_master;

    private Object ex = new Object();
    private bool virgin = true;

    public Future(TaskMaster task_master) {
        this.task_master = task_master;
    }
    /**
    * Called by the TaskMaster to start or continue computation.
    */
    internal void Compute() {
        if (result == null) {
            Run();
            if (result == null) {
                return;
            }
            WakeupListeners();
        }
    }

    /**
    * Attach a callback when the computation is complete. If already complete,
    * the callback is immediately invoked.
    */

    public void Notify(ConsumeResult new_consumer) {
        Notify(new_consumer, true);
    }

    public void NotifyDelayed(ConsumeResult new_consumer) {
        Notify(new_consumer, false);
    }

    private void Notify(ConsumeResult new_consumer, bool needs_slot) {
        Monitor.Enter(ex);
        if (result == null) {
            if (consumer == null) {
                consumer = new_consumer;
            } else {
                consumer += new_consumer;
            }
            if (needs_slot) {
                SlotHelper();
            }
            Monitor.Exit(ex);
        } else {
            Monitor.Exit(ex);
            new_consumer(result);
        }
    }

    /**
    * Apply an override to a normal computation resulting in another normal computation.
    */

    public static ComputeValue PerformOverride(string filename, int start_line, int start_column, int end_line,
            int end_column, ComputeOverride wrapper, ComputeValue original) {
        if (original == null) {
            return (task_master, reference, context, self, container) =>
                   new FailureFuture(task_master, new BasicSourceReference("used by override", filename,
                                          start_line, start_column, end_line, end_column, reference), "override of non-existant attribute");
        }
        return
            (task_master, reference, context, self, container) =>
            wrapper(task_master, reference, context, self, container,
                    original(task_master,
                             new BasicSourceReference("used by override", filename, start_line, start_column, end_line,
                                     end_column, reference), context, self, container));
    }

    /**
    * The method that will be invoked when the result is needed. If the method
    * returns true, the computation is finished. Otherwise, it is assumed that
    * the computation needs to wait another value.
    */
    protected abstract void Run();

    public void Slot() {
        Monitor.Enter(ex);
        if (result == null) {
            SlotHelper();
        }
        Monitor.Exit(ex);
    }

    private void SlotHelper() {
        if (virgin && task_master != null) {
            virgin = false;
            task_master.Slot(this);
        }
    }

    protected void WakeupListeners() {
        if (result == null) {
            throw new InvalidOperationException();
        }
        Monitor.Enter(ex);
        var consumer_copy = consumer;
        consumer = null;
        Monitor.Exit(ex);
        if (consumer_copy != null) {
            consumer_copy(result);
        }
    }
}

/**
 * Scheduler for computations.
 */

public abstract class TaskMaster : IEnumerable<Lookup> {
    private readonly Queue<Future> computations = new Queue<Future>();
    private readonly Dictionary<string, Future> external_cache = new Dictionary<string, Future>();
    private readonly List<UriHandler> handlers = new List<UriHandler>();
    /**
    * These are computations that have not completed.
    */
    private readonly Dictionary<Lookup, bool> inflight = new Dictionary<Lookup, bool>();
    private long next_id;

    public bool HasInflightLookups {
        get {
            return inflight.Count > 0;
        }
    }

    protected void ClearInFlight() {
        inflight.Clear();
    }

    public IEnumerator<Lookup> GetEnumerator() {
        return inflight.Keys.GetEnumerator();
    }

    IEnumerator IEnumerable.GetEnumerator() {
        return GetEnumerator();
    }
    public void AddAllUriHandlers(ResourcePathFinder finder, LoadRule rules) {
        var uri = new Uri(Assembly.GetExecutingAssembly().GetName().CodeBase);
        var directory = Path.GetDirectoryName(uri.LocalPath);
        handlers.AddRange(Directory.GetFiles(directory).Where(file => file.EndsWith(".urlhandler")).SelectMany(file => File.ReadLines(file)).Where(line => !line.StartsWith("#")).Select(line => ((UriService) Activator.CreateInstance(Type.GetType(line))).Create(finder, rules)).Where(u => u != null));

        AddUriHandler(new CurrentInformation(rules.HasFlag(LoadRule.Interactive)));
        AddUriHandler(BuiltInLibraries.INSTANCE);
        if (!rules.HasFlag(LoadRule.Sandboxed)) {
            AddUriHandler(SettingsHandler.INSTANCE);
            AddUriHandler(EnvironmentUriHandler.INSTANCE);
            AddUriHandler(HttpHandler.INSTANCE);
            AddUriHandler(FtpHandler.INSTANCE);
            AddUriHandler(FileHandler.INSTANCE);
            var resource_handler = new ResourceHandler();
            resource_handler.Finder = finder;
            AddUriHandler(resource_handler);
        }
        if (rules.HasFlag(LoadRule.Precompiled)) {
            var precomp = new LoadPrecompiledLibraries();
            precomp.Finder = finder;
            AddUriHandler(precomp);
        }

    }
    public void AddUriHandler(UriHandler handler) {
        handlers.Add(handler);
    }

    public void AddUriHandler(UriLoader loader) {
        handlers.Add(new UriInstaniator(loader));
    }

    public virtual void GetExternal(string uri, ConsumeResult target) {
        if (external_cache.ContainsKey(uri)) {
            external_cache[uri].Notify(target);
            return;
        }
        if (uri.StartsWith("lib:")) {
            if (uri.Length < 5) {
                external_cache[uri] = BlackholeFuture.INSTANCE;
                ReportExternalError(uri, LibraryFailure.BadName);
                return;
            }
            for (var it = 5; it < uri.Length; it++) {
                if (uri[it] != '/' && !char.IsLetterOrDigit(uri[it])) {
                    external_cache[uri] = BlackholeFuture.INSTANCE;
                    ReportExternalError(uri, LibraryFailure.BadName);
                    return;
                }
            }
        }

        foreach (var handler in handlers) {
            LibraryFailure reason;
            var computation = handler.ResolveUri(this, uri, out reason);
            if (reason == LibraryFailure.Missing) {
                continue;
            }
            if (reason != LibraryFailure.None || computation == null) {
                external_cache[uri] = BlackholeFuture.INSTANCE;
                ReportExternalError(uri, reason);
                return;
            }
            external_cache[uri] = computation;
            computation.Notify(target);
            return;
        }
        external_cache[uri] = BlackholeFuture.INSTANCE;
        ReportExternalError(uri, LibraryFailure.Missing);
    }

    public long NextId() {
        return Interlocked.Increment(ref next_id);
    }

    public abstract void ReportExternalError(string uri, LibraryFailure reason);
    /**
    * Report an error during lookup.
    */

    public virtual void ReportLookupError(Lookup lookup, Type fail_type) {
        if (fail_type == null) {
            ReportOtherError(lookup.SourceReference,
                             String.Format("Undefined name “{0}”. Lookup was as follows:", lookup.Name));
        } else {
            ReportOtherError(lookup.SourceReference,
                             String.Format("Non-frame type {1} while resolving name “{0}”. Lookup was as follows:", lookup.Name,
                                           fail_type));
        }
    }

    /**
    * Report an error during execution of the program.
    */
    public abstract void ReportOtherError(SourceReference reference, string message);
    /**
    * Perform computations until the Flabbergast program is complete or deadlocked.
    */

    public void Run() {
        handlers.Sort((a, b) => a.Priority - b.Priority);
        while (computations.Count > 0) {
            var task = computations.Dequeue();
            task.Compute();
        }
    }

    /**
    * Add a computation to be executed.
    */

    public void Slot(Future computation) {
        if (computation is Lookup) {
            var lookup = (Lookup) computation;
            inflight[lookup] = true;
            computation.NotifyDelayed(x => inflight.Remove(lookup));
        }
        computations.Enqueue(computation);
    }

    private delegate void ReportError(string error_msg);

    public static bool VerifySymbol(Stringish strish) {
        return VerifySymbol(strish.ToString());
    }
    public static bool VerifySymbol(string str) {
        return VerifySymbol(str, error_msg => {});
    }
    public bool VerifySymbol(SourceReference source_reference, Stringish strish) {
        return VerifySymbol(source_reference, strish.ToString());
    }
    public bool VerifySymbol(SourceReference source_reference, string str) {
        return VerifySymbol(str, msg => ReportOtherError(source_reference, msg));
    }
    private static bool VerifySymbol(string str, ReportError error) {
        if (str.Length < 1) {
            error("An attribute name cannot be empty.");
            return false;
        }
        switch (Char.GetUnicodeCategory(str[0])) {
        case UnicodeCategory.LowercaseLetter:
        case UnicodeCategory.OtherLetter:
            break;
        default:
            error(String.Format("The name “{0}” is unbecoming of an attribute; it cannot start with “{1}”.",
                                str, str[0]));
            return false;
        }
        for (var it = 1; it < str.Length; it++) {
            if (str[it] == '_') {
                continue;
            }
            switch (Char.GetUnicodeCategory(str[it])) {
            case UnicodeCategory.DecimalDigitNumber:
            case UnicodeCategory.LetterNumber:
            case UnicodeCategory.LowercaseLetter:
            case UnicodeCategory.OtherLetter:
            case UnicodeCategory.OtherNumber:
            case UnicodeCategory.TitlecaseLetter:
            case UnicodeCategory.UppercaseLetter:
                continue;
            default:
                error(String.Format("The name “{0}” is unbecoming of an attribute; it cannot contain “{1}”.",
                                    str, str[it]));
                return false;
            }
        }
        return true;
    }
}

}
