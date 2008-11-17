package com.zimbra.cs.account.accesscontrol;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.GranteeBy;
import com.zimbra.cs.account.Provisioning.TargetBy;
import com.zimbra.cs.account.accesscontrol.RightChecker.EffectiveACL;

public class RightCommand {
    
    /*
     * ACL and ACE are aux class for ProvUtil.  We don't want to pass "live"(those actually being used 
     * in the server) ZimbraACL/ZimbraACE objects to ProvUtil, because:
     *     - Some methods(e.g. ZimbraACE.getGranteeDisplayName ) calls Provisioning.getInstance(), 
     *       which is an instance of LdapProvisioning, which should not be done from ProvUtil when 
     *       the command is via soap.
     *       
     *     - We really just want to pass a "static" object in that all data members are "burned in" 
     *       and cannot be manipulated, since the sole purpose for them is to be displayed/read.
     *       
     * Use String instead of TargetTyep/GranteeType/Right data members in those classes so they 
     * can be readily displayed/serialize without further dependency of any server side logic, e.g.
     * RightManager, which would access LDAP for custom rights that are defined in LDAP.
     */
    
    public static class ACL {
        Set<ACE> mACEs = new HashSet<ACE>();
        
        ACL() {
        }
        
        void addACE(ACE ace) {
            mACEs.add(ace);
        }
        
        public Set<ACE> getACEs() {
            return mACEs;
        }
        
        /*
         * ctor or parsing ACL from a SOAP response
         * called from CLI
         */
        public ACL(Element parent) throws ServiceException {
            for (Element eGrant : parent.listElements(AdminConstants.E_GRANT)) {
                String granteeType = eGrant.getAttribute(AdminConstants.A_TYPE);
                String granteeId = eGrant.getAttribute(AdminConstants.A_ID);
                String granteeName = eGrant.getAttribute(AdminConstants.A_NAME);
                String right = eGrant.getAttribute(AdminConstants.A_RIGHT);
                boolean deny = eGrant.getAttributeBool(AdminConstants.A_DENY, false);
                
                ACE ace = new ACE(granteeType, granteeId, granteeName, right, deny);
                addACE(ace);
            }
        }
        
        /*
         * ctor to construct an ACL from ZimbraACL
         * called in server
         */
        private ACL(ZimbraACL acl) {
            if (acl == null)
                return;
                
            for (ZimbraACE ace : acl.getAllACEs()) {
                addACE(new ACE(ace));
            }
        }
        
        public void toXML(Element parent) {
            for (ACE ace : mACEs) {
                Element eGrant = parent.addElement(AdminConstants.E_GRANT);
                eGrant.addAttribute(AdminConstants.A_TYPE, ace.granteeType());
                eGrant.addAttribute(AdminConstants.A_ID, ace.granteeId());
                eGrant.addAttribute(AdminConstants.A_NAME, ace.granteeName());
                eGrant.addAttribute(AdminConstants.A_RIGHT, ace.right());
                eGrant.addAttribute(AdminConstants.A_DENY, ace.deny());
            }
        }
    }
    
    public static class ACE {
        String mGranteeType;
        String mGranteeId;
        String mGranteeName;
        String mRight;
        boolean mDeny;
    
        /*
         * called from CLI
         */
        private ACE(String granteeType,
            String granteeId,
            String granteeName,
            String right,
            boolean deny) {
            
            mGranteeType = granteeType;
            mGranteeId = granteeId;
            mGranteeName = granteeName;
            mRight = right;
            mDeny = deny;
        }
        
        /*
         * called in server
         */
        private ACE(ZimbraACE ace) {
            mGranteeType = ace.getGranteeType().getCode();
            mGranteeId = ace.getGrantee();
            mGranteeName = ace.getGranteeDisplayName();
            mRight = ace.getRight().getName();
            mDeny = ace.deny();
        }
        
        public String granteeType() { return mGranteeType; }
        public String granteeId()   { return mGranteeId; }
        public String granteeName() { return mGranteeName; }
        public String right()       { return mRight; }
        public boolean deny()       { return mDeny; }
    }
    
    public static class EffectiveRights {
        String mTargetType;
        String mTargetId;
        String mTargetName;
        
        // preset
        Set<String> mPresetRights;
        
        // setAttrs
        boolean mCanSetAllAttrs;
        Set<String> mCanSetAttrs;
        Set<String> mCanSetAttrsWithLimit;
        
        // getAttrs
        boolean mCanGetAllAttrs;
        Set<String> mCanGetAttrs;
        
        EffectiveRights(String targetType, String targetId, String targetName) {
            mTargetType = targetType;
            mTargetId = targetId;
            mTargetName = targetName;
        }
        
        public EffectiveRights(Element parent) throws ServiceException {
            Element eTarget = parent.getElement(AdminConstants.E_TARGET);
            mTargetType = eTarget.getAttribute(AdminConstants.A_TYPE);
            mTargetId = eTarget.getAttribute(AdminConstants.A_ID);
            mTargetName= eTarget.getAttribute(AdminConstants.A_NAME);
            
            // preset rights
            mPresetRights = new HashSet<String>();
            for (Element eRight : eTarget.listElements(AdminConstants.E_RIGHT)) {
                mPresetRights.add(eRight.getAttribute(AdminConstants.A_N));
            }
                
            // setAttrs
            Element eSetAttrs = eTarget.getElement(AdminConstants.E_SET_ATTRS);
            if (eSetAttrs.getAttributeBool(AdminConstants.A_ALL, false)) {
                mCanSetAllAttrs = true;
            } else {
                mCanSetAttrs = new HashSet<String>();
                mCanSetAttrsWithLimit = new HashSet<String>();
                for (Element eAttr : eSetAttrs.listElements(AdminConstants.E_A)) {
                    String attrName = eAttr.getAttribute(AdminConstants.A_N);
                    mCanSetAttrs.add(attrName);
                    if (eAttr.getAttributeBool(AdminConstants.A_ATTR_LIMIT, false))
                        mCanSetAttrsWithLimit.add(attrName);
                }
            }
            
            // getAttrs
            Element eGetAttrs = eTarget.getElement(AdminConstants.E_GET_ATTRS);
            if (eGetAttrs.getAttributeBool(AdminConstants.A_ALL, false)) {
                mCanGetAllAttrs = true;
            } else {
                mCanGetAttrs = new HashSet<String>();
                for (Element eAttr : eSetAttrs.listElements(AdminConstants.E_A)) {
                    String attrName = eAttr.getAttribute(AdminConstants.A_N);
                    mCanGetAttrs.add(attrName);
                }
            }
        }
        
        public void toXML(Element parent) {
            Element eTarget = parent.addElement(AdminConstants.E_TARGET);
            eTarget.addAttribute(AdminConstants.A_TYPE, mTargetType);
            eTarget.addAttribute(AdminConstants.A_ID, mTargetId);
            eTarget.addAttribute(AdminConstants.A_NAME, mTargetName);
            
            // preset rights
            for (String r : mPresetRights) {
                Element eRight = eTarget.addElement(AdminConstants.E_RIGHT).addAttribute(AdminConstants.A_N, r);
            }
            
            // setAttrs
            Element eSetAttrs = eTarget.addElement(AdminConstants.E_SET_ATTRS);
            if (mCanSetAllAttrs) {
                eSetAttrs.addAttribute(AdminConstants.A_ALL, true);
            } else {
                for (String a : mCanSetAttrs) {
                    Element eAttr = eSetAttrs.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, a);
                    if (mCanSetAttrsWithLimit.contains(a))
                        eAttr.addAttribute(AdminConstants.A_ATTR_LIMIT, true);
                }
            }
            
            // getAttrs
            Element eGetAttrs = eTarget.addElement(AdminConstants.E_GET_ATTRS);
            if (mCanGetAllAttrs) {
                eGetAttrs.addAttribute(AdminConstants.A_ALL, true);
            } else {
                for (String a : mCanGetAttrs) {
                    Element eAttr = eGetAttrs.addElement(AdminConstants.E_A).addAttribute(AdminConstants.A_N, a);
                }
            }
        }
        
        void addPresetRight(String right) {
            if (mPresetRights == null)
                mPresetRights = new HashSet<String>();
            mPresetRights.add(right);
        }
        
        void setCanSetAllAttrs() { mCanSetAllAttrs = true; }
        void setCanSetAttrs(Set<String> canSetAttrs) { mCanSetAttrs = canSetAttrs; }
        void setCanSetAttrsWithLimit(Set<String> canSetAttrsWithLimit) { mCanSetAttrsWithLimit = canSetAttrsWithLimit; }
        void setCanGetAllAttrs() { mCanGetAllAttrs = true; }
        void setCanGetAttrs(Set<String> canGetAttrs) { mCanGetAttrs = canGetAttrs; }
        
        public Set<String> presetRights() { return mPresetRights; }
        public boolean canSetAllAttrs() { return mCanSetAllAttrs; } 
        public Set<String> canSetAttrs() { return mCanSetAttrs; }
        public Set<String> canSetAttrsWithLimit() { return mCanSetAttrsWithLimit; }
        public boolean canGetAllAttrs() { return mCanGetAllAttrs; } 
        public Set<String> canGetAttrs() { return mCanGetAttrs; }
    }
    
    
    public static boolean checkRight(Provisioning prov,
                                     String targetType, TargetBy targetBy, String target,
                                     GranteeBy granteeBy, String grantee,
                                     String right,
                                     AccessManager.ViaGrant via) throws ServiceException {
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        // grantee
        GranteeType gt = GranteeType.GT_USER;
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);  // grantee for check right must be an Account
        
        // right
        Right r = RightManager.getInstance().getRight(right);
        
        boolean canPerform = AccessManager.getInstance().canDo((Account)granteeEntry, targetEntry, r, true, false, via);
        return canPerform;
    }
    
    public static EffectiveRights getEffectiveRights(Provisioning prov,
                                               String targetType, TargetBy targetBy, String target,
                                               GranteeBy granteeBy, String grantee) throws ServiceException {

        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        // grantee
        GranteeType gt = GranteeType.GT_USER;
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);  
        // granteeEntry right must be an Account
        Account granteeAcct = (Account)granteeEntry;
        
        List<EffectiveACL> effectiveACLs = TargetType.expandTargetByGrantee(prov, targetEntry, granteeAcct);
        
        String targetId = (targetEntry instanceof NamedEntry)? ((NamedEntry)targetEntry).getId() : "";
        EffectiveRights er = new EffectiveRights(targetType, targetId, targetEntry.getLabel());
        
        if (effectiveACLs != null && effectiveACLs.size() > 0)
            RightChecker.getEffectiveRights(effectiveACLs, granteeAcct, TargetType.getAttributeClass(targetEntry), er);
        return er;
    }
    
    public static ACL getGrants(Provisioning prov,
                                String targetType, TargetBy targetBy, String target) throws ServiceException {
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        ZimbraACL zimbraAcl = RightUtil.getACL(targetEntry);
        return new ACL(zimbraAcl);
    }
            
    public static void grantRight(Provisioning prov,
                                  String targetType, TargetBy targetBy, String target,
                                  String granteeType, GranteeBy granteeBy, String grantee,
                                  String right, boolean deny) throws ServiceException {
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        // grantee
        GranteeType gt = GranteeType.fromCode(granteeType);
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
        
        // right
        Right r = RightManager.getInstance().getRight(right);
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        ZimbraACE ace = new ZimbraACE(granteeEntry.getId(), gt, r, deny, null);
        aces.add(ace);
        
        RightUtil.grantRight(prov, targetEntry, aces);
    }

    public static void revokeRight(Provisioning prov,
                                   String targetType, TargetBy targetBy, String target,
                                   String granteeType, GranteeBy granteeBy, String grantee,
                                   String right, boolean deny) throws ServiceException {
        
        // target
        TargetType tt = TargetType.fromString(targetType);
        Entry targetEntry = TargetType.lookupTarget(prov, tt, targetBy, target);
        
        // grantee
        GranteeType gt = GranteeType.fromCode(granteeType);
        NamedEntry granteeEntry = GranteeType.lookupGrantee(prov, gt, granteeBy, grantee);
        
        // right
        Right r = RightManager.getInstance().getRight(right);
        
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        ZimbraACE ace = new ZimbraACE(granteeEntry.getId(), gt, r, deny, null);
        aces.add(ace);
        
        RightUtil.revokeRight(prov, targetEntry, aces);
    }
}
