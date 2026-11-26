package name.monwf.customiuizer.mods.utils;

import java.lang.reflect.Executable;
import java.lang.reflect.Member;

import io.github.libxposed.api.XposedInterface;

/**
 * Adapts the legacy callback-based hook helpers used by the module to the
 * interceptor-chain API introduced by libxposed API 101.
 */
public final class HookerClassHelper {
    private HookerClassHelper() {}

    public interface CustomMethodUnhooker {
        void unhook();
    }

    public static final class BeforeHookCallback {
        private final XposedInterface.Chain chain;
        private final Object[] args;
        private boolean skipped;
        private Object result;

        private BeforeHookCallback(XposedInterface.Chain chain, Object[] args) {
            this.chain = chain;
            this.args = args;
        }

        public Object getThisObject() {
            return chain.getThisObject();
        }

        public Object[] getArgs() {
            return args;
        }

        public Member getMember() {
            return chain.getExecutable();
        }

        public void returnAndSkip(Object result) {
            this.result = result;
            this.skipped = true;
        }

        private boolean isSkipped() {
            return skipped;
        }

        private Object getResult() {
            return result;
        }
    }

    public static final class AfterHookCallback {
        private final Object thisObject;
        private final Member member;
        private final Object[] args;
        private Object result;
        private Throwable throwable;

        private AfterHookCallback(Object thisObject, Member member, Object[] args,
                                  Object result, Throwable throwable) {
            this.thisObject = thisObject;
            this.member = member;
            this.args = args;
            this.result = result;
            this.throwable = throwable;
        }

        public Object getThisObject() {
            return thisObject;
        }

        public Object[] getArgs() {
            return args;
        }

        public Member getMember() {
            return member;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
            this.throwable = null;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public void setThrowable(Throwable throwable) {
            this.throwable = throwable;
            if (throwable != null) {
                this.result = null;
            }
        }
    }

    public static class MethodHook {
        public int mPriority;

        public MethodHook() {
            this(XposedInterface.PRIORITY_DEFAULT);
        }

        public MethodHook(int priority) {
            mPriority = priority;
        }

        public final void beforeHook(BeforeHookCallback callback) {
            try {
                before(callback);
            } catch (Throwable t) {
                XposedHelpers.log(t);
            }
        }

        public final void afterHook(AfterHookCallback callback) {
            try {
                after(callback);
            } catch (Throwable t) {
                XposedHelpers.log(t);
            }
        }

        protected void before(BeforeHookCallback callback) throws Throwable {
        }

        protected void after(AfterHookCallback callback) throws Throwable {
        }
    }

    public static CustomMethodUnhooker hook(Member member, MethodHook callback) {
        if (!(member instanceof Executable)) {
            throw new IllegalArgumentException("Only methods and constructors can be hooked");
        }

        XposedInterface.HookHandle handle = XposedHelpers.moduleInst
            .hook((Executable) member)
            .setPriority(callback.mPriority)
            .intercept(new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object[] args = chain.getArgs().toArray();
                    BeforeHookCallback before = new BeforeHookCallback(chain, args);
                    callback.beforeHook(before);

                    Object result = before.getResult();
                    Throwable throwable = null;
                    if (!before.isSkipped()) {
                        try {
                            result = chain.proceed(args);
                        } catch (Throwable t) {
                            throwable = t;
                        }
                    }

                    AfterHookCallback after = new AfterHookCallback(
                        chain.getThisObject(), chain.getExecutable(), args, result, throwable
                    );
                    callback.afterHook(after);

                    if (after.getThrowable() != null) {
                        throw after.getThrowable();
                    }
                    return after.getResult();
                }
            });

        return handle::unhook;
    }
    /**
     * Skip the hooked method and return null, matching the old helper behavior.
     */
    public static final MethodHook DO_NOTHING = new MethodHook(XposedInterface.PRIORITY_HIGHEST) {
        @Override
        protected void before(BeforeHookCallback callback) {
            callback.returnAndSkip(null);
        }
    };

    /**
     * Create a hook that skips the original method and returns a constant value.
     */
    public static MethodHook returnConstant(Object result) {
        return returnConstant(XposedInterface.PRIORITY_HIGHEST, result);
    }

    public static MethodHook returnConstant(int priority, Object result) {
        return new MethodHook(priority) {
            @Override
            protected void before(BeforeHookCallback callback) {
                callback.returnAndSkip(result);
            }
        };
    }

}
