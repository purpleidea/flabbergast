using System;
using System.Collections.Generic;
using System.Threading;

namespace Flabbergast {
/**
 * A computation that never completes.
 */
public class BlackholeComputation : Computation {
    public readonly static Computation INSTANCE = new BlackholeComputation();
    private BlackholeComputation() : base(null) {
    }
    protected override bool Run() {
        return false;
    }
}

public class FailureComputation : Computation {
    private string message;
    private SourceReference source_reference;
    public FailureComputation(TaskMaster task_master, SourceReference reference, string message) : base(task_master) {
        this.source_reference = reference;
        this.message = message;
    }

    protected override bool Run() {
        task_master.ReportOtherError(source_reference, message);
        return false;
    }
}

/**
 * Holds a value for inclusion of a pre-computed value in a template.
 */
public class Precomputation : Computation {
    public static ComputeValue Capture(object result) {
        return new Precomputation(result).ComputeValue;
    }
    public Precomputation(object result) : base(null) {
        this.result = result;
    }
    public Computation ComputeValue(
        TaskMaster task_master, SourceReference reference, Context context, Frame self, Frame container) {
        return this;
    }
    protected override bool Run() {
        return true;
    }
}
/**
 * Adapt C#'s async/await system to Flabbergast's TaskMaster.
 */
public abstract class AsyncComputation : Computation {
    private class AsyncResult : IAsyncResult {
        private readonly object state;
        private readonly AsyncCallback callback;
        private readonly AsyncComputation owner;
        private ManualResetEvent reset_event = new ManualResetEvent(false);
        private int interlock = 2;

        public AsyncResult(AsyncComputation owner, AsyncCallback callback, object state) {
            this.owner = owner;
            this.callback = callback;
            this.state = state;
        }

        public object AsyncState {
            get {
                return state;
            }
        }

        public WaitHandle AsyncWaitHandle {
            get {
                return reset_event;
            }
        }

        public bool CompletedSynchronously {
            get;
            internal set;
        }

        public bool HasCallback {
            get {
                return this.callback != null;
            }
        }

        public bool IsCompleted {
            get {
                return Result != null;
            }
        }

        internal object Result = null;

        public void Continue() {
            callback(this);
        }

        public void Prepare(Computation c) {
            c.Notify(result => {
                this.Result = result;
                reset_event.Set();
                if (Interlocked.Decrement(ref interlock) == 0) {
                    owner.task_master.Slot(owner);
                }
            });
            if (Interlocked.Decrement(ref interlock) == 0) {
                CompletedSynchronously = true;
                Continue();
            }
        }
    }

    private AsyncResult state = null;

    public AsyncComputation(TaskMaster task_master) : base(task_master) {
    }

    /**
     * Asynchronously wait for the result of a computation.
     *
     * Call `await Go(computation)` to wait for the result.
     */
    public IAsyncResult BeginGo(Computation c, AsyncCallback callback, object asyncState) {
        state = new AsyncResult(this, callback, asyncState);
        state.Prepare(c);
        return state;
    }

    public object EndGo(IAsyncResult r) {
        AsyncResult ar = r as AsyncResult;
        return ar.Result;
    }

    /**
     * Override with a user computation.
     */
    protected abstract void RunAsync();

    protected override bool Run() {
        if (state == null) {
            Action action = RunAsync;
            action.BeginInvoke((ar) => {
                action.EndInvoke(ar);
                if (result != null) WakeupListeners();
            }, this);
        } else {
            var x = state;
            state = null;
            x.Continue();
        }
        return false;
    }
}
}
