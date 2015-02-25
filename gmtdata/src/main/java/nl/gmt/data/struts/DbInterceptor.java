package nl.gmt.data.struts;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import com.opensymphony.xwork2.interceptor.PreResultListener;
import nl.gmt.data.DbConnection;
import nl.gmt.data.DbContext;
import org.apache.commons.lang.Validate;

public class DbInterceptor extends AbstractInterceptor {
    private static final ThreadLocal<DbContext> CONTEXT = new ThreadLocal<>();
    private static volatile DbConnection db;

    public static DbContext getContext() {
        return CONTEXT.get();
    }

    public static DbConnection getDb() {
        return db;
    }

    public static void setDb(DbConnection db) {
        DbInterceptor.db = db;
    }

    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        if (invocation.getAction().getClass().getAnnotation(DbAware.class) == null) {
            return invocation.invoke();
        }

        Validate.notNull(db, "The database has not yet been assigned");

        try (final DbContext ctx = db.openContext()) {
            assert CONTEXT.get() == null;

            CONTEXT.set(ctx);

            // We add a pre-result listener to try to flush the request before the redirect. If we don't do this,
            // exceptions from the session commit won't be shown because we aren't allowed to do something after the
            // redirect has been committed.

            invocation.addPreResultListener(new PreResultListener() {
                @Override
                public void beforeResult(ActionInvocation invocation, String resultCode) {
                    ctx.getSession().flush();
                }
            });

            String result = invocation.invoke();

            ctx.commit();

            return result;
        } finally {
            CONTEXT.remove();
        }
    }
}
