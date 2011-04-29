/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZAttrProvisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.prov.ldap.LdapHelper;
import com.zimbra.cs.prov.ldap.LdapProv;

import static org.junit.Assert.*;

public class TestLdapProvDomain {
    
    private static Provisioning prov;
    
    @BeforeClass
    public static void init() throws Exception {
        TestLdap.manualInit();
        
        prov = Provisioning.getInstance();
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        String baseDomainName = baseDomainName();
        String parts[] = baseDomainName.split("\\.");
        String[] dns = ((LdapProv) prov).getDIT().domainToDNs(parts);
        String topMostRDN = dns[dns.length-1];
        TestLdap.deleteEntireBranchInDIT(topMostRDN);
    }
    
    private static String baseDomainName() {
        return TestLdapProvDomain.class.getName().toLowerCase();
    }
    
    private static String makeDomainName(String prefix) {
        String baseDomainName = baseDomainName();
        if (prefix == null) {
            return baseDomainName;
        } else {
            return prefix.toLowerCase() + "." + baseDomainName;
        }
    }

    @Test
    public void createTopDomain() throws Exception {
        String DOMAIN_NAME = makeDomainName(null);
        Domain domain = prov.get(DomainBy.name, DOMAIN_NAME);
        assertNull(domain);
        
        domain = prov.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        assertNotNull(domain);
        
        prov.deleteDomain(domain.getId());
    }
    
    @Test
    public void createSubDomain() throws Exception {
        String DOMAIN_NAME = makeDomainName("createSubDomain.sub1.sub2");
        Domain domain = prov.get(DomainBy.name, DOMAIN_NAME);
        assertNull(domain);
        
        domain = prov.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        assertNotNull(domain);
        
        prov.deleteDomain(domain.getId());
    }
    
    @Test
    public void deleteNonEmptyDomain() throws Exception {
        String DOMAIN_NAME = makeDomainName("deleteNonEmptyDomain");
        Domain domain = prov.get(DomainBy.name, DOMAIN_NAME);
        assertNull(domain);
        
        domain = prov.createDomain(DOMAIN_NAME, new HashMap<String, Object>());
        assertNotNull(domain);
        
        String ACCT_NAME = TestUtil.getAddress("acct", DOMAIN_NAME);
        Account acct = prov.createAccount(ACCT_NAME, "test123", null);
        
        boolean caughtException = false;
        try {
            prov.deleteDomain(domain.getId());
        } catch (ServiceException e) {
            if (AccountServiceException.DOMAIN_NOT_EMPTY.equals(e.getCode())) {
                caughtException = true;
            }
        }
        assertTrue(caughtException);
        
        // now should able to delete domain
        prov.deleteAccount(acct.getId());
        prov.deleteDomain(domain.getId());
    }
    
    private void verifyAllDomains(List<Domain> allDomains) throws Exception {
        // domains created by r-t-w
        // TODO: this verification is very fragile
        Set<String> expectedDomains = new HashSet<String>();
        String defaultDomainName = prov.getInstance().getConfig().getDefaultDomainName();
        
        expectedDomains.add(defaultDomainName);
        expectedDomains.add("example.com");
        
        assertEquals(expectedDomains.size(), allDomains.size());
        
        for (Domain domain : allDomains) {
            assertTrue(expectedDomains.contains(domain.getName()));
        }
        
        //
        // another verification
        //
        LdapHelper ldapHelper = ((LdapProv) prov).getHelper();
        final List<String /* zimbraId */> domainIds = new ArrayList<String>();
        SearchLdapOptions.SearchLdapVisitor visitor = new SearchLdapOptions.SearchLdapVisitor() {
            @Override
            public void visit(String dn, Map<String, Object> attrs,
                    IAttributes ldapAttrs) {
                try {
                    domainIds.add(ldapAttrs.getAttrString(Provisioning.A_zimbraId));
                } catch (ServiceException e) {
                    fail();
                }
            }
        };
        
        SearchLdapOptions searchOpts = new SearchLdapOptions(LdapConstants.DN_ROOT_DSE, 
                "(objectclass=zimbraDomain)", new String[]{Provisioning.A_zimbraId}, null, visitor);
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext();
            ldapHelper.searchLdap(zlc, searchOpts);
        } finally {
            LdapClient.closeContext(zlc);
        }
        
        assertEquals(domainIds.size(), allDomains.size());
        
        for (Domain domain : allDomains) {
            assertTrue(domainIds.contains(domain.getId()));
        }
    }
    
    @Test
    public void getAllDomain() throws Exception {
        List<Domain> allDomains = prov.getAllDomains();
        verifyAllDomains(allDomains);
    }
    
    @Test
    public void getAllDomainVisitor() throws Exception {
        final List<Domain> allDomains = new ArrayList<Domain>();
        
        NamedEntry.Visitor visitor = new NamedEntry.Visitor() {
            @Override
            public void visit(NamedEntry entry) throws ServiceException {
                allDomains.add((Domain) entry);
            }
        };
        
        prov.getAllDomains(visitor, new String[]{Provisioning.A_zimbraId});
        
        verifyAllDomains(allDomains);
    }
    
    @Test
    public void testAliasDomain() throws Exception {
        String TARGET_DOMAIN_NAME = makeDomainName("testAliasDomain-target");
        String ALIAS_DOMAIN_NAME = makeDomainName("testAliasDomain-alias");
        String USER_LOCAL_PART = "user";
        
        Domain targetDomain = prov.get(DomainBy.name, TARGET_DOMAIN_NAME);
        assertNull(targetDomain);
        Domain aliasDomain = prov.get(DomainBy.name, ALIAS_DOMAIN_NAME);
        assertNull(aliasDomain);
        
        targetDomain = prov.createDomain(TARGET_DOMAIN_NAME, new HashMap<String, Object>());
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainType, ZAttrProvisioning.DomainType.alias.name());
        attrs.put(Provisioning.A_zimbraDomainAliasTargetId, targetDomain.getId());
        aliasDomain = prov.createDomain(ALIAS_DOMAIN_NAME, attrs);
        
        String realEmail = prov.getEmailAddrByDomainAlias(TestUtil.getAddress(USER_LOCAL_PART, ALIAS_DOMAIN_NAME));
        assertEquals(TestUtil.getAddress(USER_LOCAL_PART, TARGET_DOMAIN_NAME), realEmail);
        
        prov.deleteDomain(aliasDomain.getId());
        prov.deleteDomain(targetDomain.getId());
    }
    
    @Test
    public void getEmailAddrByDomainAlias() throws Exception {
        // tested in testAliasDomain
    }
    
    private void getDomainById(String id) throws Exception {
        Domain domain = prov.get(DomainBy.id, id);
        assertEquals(id, domain.getId());
    }
    
    private void getDomainByName(String name) throws Exception {
        Domain domain = prov.get(DomainBy.name, name);
        assertEquals(name, domain.getName());
    }
    
    private void getDomainByVirtualHostname(String virtualHostname, String expectedDomainId) 
    throws Exception {
        Domain domain = prov.get(DomainBy.virtualHostname, virtualHostname);
        assertEquals(expectedDomainId, domain.getId());
    }
    
    private void getDomainByKrb5Realm(String krb5Realm, String expectedDomainId) throws Exception {
        Domain domain = prov.get(DomainBy.krb5Realm, krb5Realm);
        assertEquals(expectedDomainId, domain.getId());
    }
    
    private void getDomainByForeignName(String foreignName, String expectedDomainId) throws Exception {
        Domain domain = prov.get(DomainBy.foreignName, foreignName);
        assertEquals(expectedDomainId, domain.getId());
    }

    @Test
    public void getDomain() throws Exception {
        String DOMAIN_NAME = makeDomainName("getDomain");
        Domain domain = prov.get(DomainBy.name, DOMAIN_NAME);
        assertNull(domain);
        
        String VIRTUAL_HOSTNAME = "virtual.com";
        String KRB5_REALM = "KRB5.REALM";
        String FOREIGN_NAME = "foreignname";
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraVirtualHostname, VIRTUAL_HOSTNAME);
        attrs.put(Provisioning.A_zimbraAuthKerberos5Realm, KRB5_REALM);
        attrs.put(Provisioning.A_zimbraForeignName, FOREIGN_NAME);
        
        domain = prov.createDomain(DOMAIN_NAME, attrs);
        assertNotNull(domain);
        
        String domainId = domain.getId();
        
        getDomainById(domainId);
        getDomainByName(DOMAIN_NAME);
        getDomainByVirtualHostname(VIRTUAL_HOSTNAME, domainId);
        getDomainByKrb5Realm(KRB5_REALM, domainId);
        getDomainByForeignName(FOREIGN_NAME, domainId);
        
        prov.deleteDomain(domainId);
    }
    
}
