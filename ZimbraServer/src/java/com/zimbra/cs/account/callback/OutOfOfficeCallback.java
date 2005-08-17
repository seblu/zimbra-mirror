package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.db.DbOutOfOffice;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Notification;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.LiquidLog;

public class OutOfOfficeCallback implements AttributeCallback {

    private static final String KEY = OutOfOfficeCallback.class.getName();

    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
    }

    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
        if (!isCreate) {
            Object done = context.get(KEY);
            if (done == null) {
                context.put(KEY, KEY);
                LiquidLog.misc.info("need to reset vacation info");
                if (entry instanceof Account) 
                    handleOutOfOffice((Account)entry);
            }
        }
    }

    private void handleOutOfOffice(Account account) {
        Connection conn = null;
        try {
            // clear the OOF database for this account
            Mailbox mbox = Mailbox.getMailboxByAccount(account);
            conn = DbPool.getConnection();
            DbOutOfOffice.clear(conn, mbox);
            conn.commit();
            LiquidLog.misc.info("reset vacation info");
            //  Convenient place to prune old data, until we determine that this
            // needs to be a separate scheduled process.
            // TODO: only prune once a day?
            DbOutOfOffice.prune(conn, Notification.OUT_OF_OFFICE_CACHE_NUM_DAYS);
            conn.commit();
        } catch (ServiceException e) {
            DbPool.quietRollback(conn);
        } finally {
            DbPool.quietClose(conn);
        }
    }
}
