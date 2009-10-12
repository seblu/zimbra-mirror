package com.zimbra.cs.mailbox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.IdentityBy;
import com.zimbra.cs.account.offline.OfflineDataSource;
import com.zimbra.cs.account.offline.OfflineProvisioning;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.offline.OfflineLC;
import com.zimbra.cs.offline.OfflineLog;
import com.zimbra.cs.offline.OfflineSyncManager;
import com.zimbra.cs.offline.common.OfflineConstants;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.zimbrasync.client.ExchangeSyncFactory;
import com.zimbra.zimbrasync.client.cmd.Request;

public class ExchangeMailbox extends ChangeTrackingMailbox {

    public ExchangeMailbox(MailboxData data) throws ServiceException {
        super(data);
    }
    
    @Override
    boolean isPushType(byte type) {
        switch (type) {
        case MailItem.TYPE_MESSAGE:
//        case MailItem.TYPE_APPOINTMENT:
//        case MailItem.TYPE_CONTACT:
//        case MailItem.TYPE_TASK:
//        case MailItem.TYPE_WIKI:
            return true;
        }
        return false;
    }
    
    /** The bitmask of all message changes that we propagate to the server. */
    static final int MESSAGE_CHANGES = Change.MODIFIED_UNREAD | Change.MODIFIED_FLAGS | Change.MODIFIED_TAGS | Change.MODIFIED_FOLDER;

    /** The bitmask of all chat changes that we propagate to the server. */
    //static final int CHAT_CHANGES = Change.MODIFIED_UNREAD | Change.MODIFIED_FLAGS | Change.MODIFIED_TAGS | Change.MODIFIED_FOLDER;

    /** The bitmask of all contact changes that we propagate to the server. */
    static final int CONTACT_CHANGES = Change.MODIFIED_FLAGS | Change.MODIFIED_TAGS | Change.MODIFIED_FOLDER | Change.MODIFIED_CONTENT;

    /** The bitmask of all folder changes that we propagate to the server. */
    static final int FOLDER_CHANGES = Change.MODIFIED_FLAGS | Change.MODIFIED_FOLDER | Change.MODIFIED_NAME;
    
    /** The bitmask of all appointment changes that we propagate to the server. */
    static final int APPOINTMENT_CHANGES = Change.MODIFIED_FLAGS | Change.MODIFIED_TAGS | Change.MODIFIED_FOLDER |
                                           Change.MODIFIED_CONTENT | Change.MODIFIED_INVITE;
    
    /** The bitmask of all document changes that we propagate to the server. */
    static final int DOCUMENT_CHANGES = Change.MODIFIED_FLAGS | Change.MODIFIED_TAGS | Change.MODIFIED_FOLDER |
                                        Change.MODIFIED_CONTENT | Change.MODIFIED_NAME;
    
    @Override
    int getChangeMaskFilter(byte type) {
        switch (type) {
        case MailItem.TYPE_MESSAGE:       return MESSAGE_CHANGES;     
        //case MailItem.TYPE_CHAT:          return PushChanges.CHAT_CHANGES;     
        case MailItem.TYPE_CONTACT:       return PushChanges.CONTACT_CHANGES;     
        case MailItem.TYPE_FOLDER:        return PushChanges.FOLDER_CHANGES;      
        case MailItem.TYPE_APPOINTMENT:
        case MailItem.TYPE_TASK:          return PushChanges.APPOINTMENT_CHANGES; 
        case MailItem.TYPE_WIKI:
        case MailItem.TYPE_DOCUMENT:      return PushChanges.DOCUMENT_CHANGES;
        default:                          return 0;
        }
    }

    OfflineDataSource getDataSource() throws ServiceException {
        return (OfflineDataSource)OfflineProvisioning.getOfflineInstance().getDataSource(getAccount());
    }
    
    @Override
    public MailSender getMailSender() throws ServiceException {
        return new OfflineMailSender();
    }
    
    private static final OperationContext sContext = new TracelessContext();
    private static final Map<Integer, Pair<Integer, String>> sSendUIDs = new HashMap<Integer, Pair<Integer, String>>();
    
    private int sendPendingMessages(boolean isOnRequest) throws ServiceException {
        int sentCount = 0;
        for (Iterator<Integer> iterator = OutboxTracker.iterator(this, isOnRequest ? 0 : 5 * Constants.MILLIS_PER_MINUTE); iterator.hasNext(); ) {
            int id = iterator.next();

            Message msg;
            try {
                msg = getMessageById(sContext, id);
            } catch (NoSuchItemException x) { //message deleted
                OutboxTracker.remove(this, id);
                continue;
            }
            if (msg == null || msg.getFolderId() != ID_FOLDER_OUTBOX) {
                OutboxTracker.remove(this, id);
                continue;
            }

            OfflineDataSource ds = getDataSource();
            if (!isOnRequest && isAutoSyncDisabled(ds))
                continue;

            ZimbraLog.xsync.debug("sending pending mail (id=%d): %s", msg.getId(), msg.getSubject());
            
            
            Identity identity = Provisioning.getInstance().get(getAccount(), IdentityBy.id, msg.getDraftIdentityId());
            // try to avoid repeated sends of the same message by tracking "send UIDs" on SendMsg requests
            Pair<Integer, String> sendRecord = sSendUIDs.get(id);
            String sendUID = sendRecord == null || sendRecord.getFirst() != msg.getSavedSequence() ? UUID.randomUUID().toString() : sendRecord.getSecond();
            sSendUIDs.put(id, new Pair<Integer, String>(msg.getSavedSequence(), sendUID));

            String origId = msg.getDraftOrigId();
            ItemId origMsgId = StringUtil.isNullOrEmpty(origId) ? null : new ItemId(origId, ds.getAccountId());

            // Do we need to save a copy of the message ourselves to the Sent folder?
            boolean saveToSent = (ds.isSaveToSent()) && getAccount().isPrefSaveToSent();
            
            try {
                new Request(ExchangeSyncFactory.getInstance().getSyncSettings(ds, 0)).doSendMail(msg.getContentStream(), msg.getSize(), saveToSent); //TODO: PolicyKey
            } catch (ServiceException x) {
                //TODO:
                ZimbraLog.xsync.warn("send mail failure (id=%d)", msg.getId(), x);
            } catch (IOException x) {
                //TODO:
                ZimbraLog.xsync.warn("send mail failure (id=%d)", msg.getId(), x);
            }

            ZimbraLog.xsync.debug("sent pending mail (id=%d)", msg.getId());

            // remove the draft from the outbox
            delete(sContext, id, MailItem.TYPE_MESSAGE);
            OutboxTracker.remove(this, id);

            // the draft is now gone, so remove it from the "send UID" hash and the list of items to push
            sSendUIDs.remove(id);
            sentCount++;
        }

        return sentCount;
    }

    private boolean isAutoSyncDisabled(DataSource ds) {
        return ds.getSyncFrequency() <= 0;
    }
    
    private boolean isTimeToSync(DataSource ds) throws ServiceException {
        OfflineSyncManager syncMan = OfflineSyncManager.getInstance();
        if (isAutoSyncDisabled(ds) || !syncMan.reauthOK(ds) || !syncMan.retryOK(ds))
            return false;
        long freqLimit = syncMan.getSyncFrequencyLimit();
        long frequency = ds.getSyncFrequency() < freqLimit ? freqLimit : ds.getSyncFrequency();
        return System.currentTimeMillis() - syncMan.getLastSyncTime(ds.getName()) >= frequency;
    }
    
    @Override
    public boolean isAutoSyncDisabled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void syncOnTimer() {
        // TODO Auto-generated method stub

    }

    @Override protected synchronized void initialize() throws ServiceException {
        super.initialize();
        getCachedItem(ID_FOLDER_CALENDAR).setColor(new MailItem.Color((byte)1));
    }

    @Override
    public void sync(boolean isOnRequest, boolean isDebugTraceOn) throws ServiceException {
        if (lockMailboxToSync()) {
            synchronized (syncLock) {
                if (isOnRequest && isDebugTraceOn) {
                    OfflineLog.offline.debug("============================== SYNC DEBUG TRACE START ==============================");
                    getOfflineAccount().setRequestScopeDebugTraceOn(true);
                }

                try {
                    int count = sendPendingMessages(isOnRequest);
                    syncDataSource(count > 0, isOnRequest);
                } catch (Exception x) {
                    if (isDeleting())
                        OfflineLog.offline.info("Mailbox \"%s\" is being deleted", getAccountName());
                    else
                        OfflineLog.offline.error("exception encountered during sync", x);
                } finally {
                    if (isOnRequest && isDebugTraceOn) {
                        getOfflineAccount().setRequestScopeDebugTraceOn(false);
                        OfflineLog.offline.debug("============================== SYNC DEBUG TRACE END ================================");
                    }
                    unlockMailbox();
                }
            } //synchronized (syncLock)
        } else if (isOnRequest) {
            OfflineLog.offline.debug("sync already in progress");
        }
    }

    private void syncDataSource(boolean force, boolean isOnRequest) throws ServiceException {
        OfflineDataSource ds = getDataSource();
        if (!force && !isOnRequest && !isTimeToSync(ds))
            return;
        
        OfflineSyncManager syncMan = OfflineSyncManager.getInstance();
        try {
            OfflineLog.offline.info(">>>>>>>> name=%s;version=%s;build=%s;release=%s;os=%s;type=%s",
                    ds.getAccount().getName(), OfflineLC.zdesktop_version.value(), OfflineLC.zdesktop_buildid.value(), OfflineLC.zdesktop_relabel.value(),
                    System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version"), ds.getType());

            syncMan.syncStart(ds.getName());
            DataSourceManager.importData(ds, null, true);
            syncMan.syncComplete(ds.getName());
            OfflineProvisioning.getOfflineInstance().setDataSourceAttribute(ds, OfflineConstants.A_zimbraDataSourceLastSync, Long.toString(System.currentTimeMillis()));
        } catch (Exception x) {
            if (isDeleting())
                OfflineLog.offline.info("Mailbox \"%s\" is being deleted", getAccountName());
            else
                syncMan.processSyncException(ds, x);
        }
    }
}
