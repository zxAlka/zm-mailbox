/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public interface Account extends NamedEntry {

    public static enum CalendarUserType {
        USER,       // regular person account
        RESOURCE    // calendar resource
    }

    public String getUid();
    
    /**
     * @return the domain name for this account (foo.com), or null if an admin account. 
     */
    public String getDomainName();
    
    /**
     * @return the domain this account, or null if an admin account. 
     * @throws ServiceException
     */    
    public Domain getDomain() throws ServiceException;    
    
    public String getAccountStatus();
    
    /**
     * combines all zimbraPref* attributes from the Account and the COS and returns as a map. Account preferences
     * override COS preferences.
     *  
     * @param prefsOnly return only zimbraPref* attrs.
     * @param applyCos apply COS attributes
     * @return
     * @throws ServiceException
     */
    public Map<String, Object> getAttrs(boolean prefsOnly, boolean applyCos) throws ServiceException;    
    

    /**
     * @return the COS object for this account, or null if account has no COS
     * 
     * @throws ServiceException
     */
    public Cos getCOS() throws ServiceException;
   
    public String[] getAliases() throws ServiceException;
    
    /**
     * @param zimbraId the zimbraId of the dl we are checking for
     * @return true if this account (or one of the dl it belongs to) is a member of the specified dl.
     * @throws ServiceException
     */
    public boolean inDistributionList(String zimbraId) throws ServiceException;

    /**
     * @return set of all the zimbraId's of lists this account belongs to, including any list in other list. 
     * @throws ServiceException
     */
    public Set<String> getDistributionLists() throws ServiceException; 

    /**
     *      
     * @param directOnly return only DLs this account is a direct member of
     * @param via if non-null and directOnly is false, this map will containing a mapping from a DL name to the DL it was a member of, if 
     *            member was indirect.
     * @return all the DLs
     * @throws ServiceException
     */
    public List<DistributionList> getDistributionLists(boolean directOnly, Map<String,String> via) throws ServiceException; 

    /**
     * @return the Server object where this account's mailbox is homed
     * @throws ServiceException
     */
    public Server getServer() throws ServiceException;

    /**
     * Returns account's time zone
     * @return
     * @throws ServiceException
     */
    public ICalTimeZone getTimeZone() throws ServiceException;

    /**
     * Returns calendar user type
     * @return USER (default) or RESOURCE
     * @throws ServiceException
     */
    public CalendarUserType getCalendarUserType() throws ServiceException;

    public boolean saveToSent() throws ServiceException;
}
