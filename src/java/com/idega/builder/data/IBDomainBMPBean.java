/*
 * $Id: IBDomainBMPBean.java,v 1.8 2003/08/05 19:45:36 tryggvil Exp $
 *
 * Copyright (C) 2001 Idega hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 *
 */
package com.idega.builder.data;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.ejb.FinderException;

import com.idega.builder.business.BuilderLogic;
import com.idega.data.GenericEntity;
import com.idega.data.IDOLookup;
import com.idega.data.IDORelationshipException;
import com.idega.user.data.GroupDomainRelation;
import com.idega.user.data.GroupDomainRelationHome;
import com.idega.user.data.GroupDomainRelationTypeBMPBean;

/**
 * @author <a href="tryggvi@idega.is">Tryggvi Larusson</a>
 * @version 1.0
 */
public class IBDomainBMPBean extends GenericEntity implements IBDomain {
  public static final String tableName = "ib_domain";
  public static final String domain_name = "domain_name";
  public static final String domain_url = "url";
  public static final String start_page = "start_ib_page_id";
  public static final String start_template = "start_ib_template_id";
  public static final String COLUMNNAME_GROUP_ID = "group_id";

  private static Map cachedDomains;

  public IBDomainBMPBean() {
    super();
  }

  private IBDomainBMPBean(int id) throws java.sql.SQLException {
    super(id);
  }

  public void initializeAttributes() {
    addAttribute(getIDColumnName());

    addAttribute(getColumnDomainName(),"Domain name",true,true,String.class);
    addAttribute(getColumnURL(),"Domain URL",true,true,String.class,1000);
    addAttribute(getColumnStartPage(),"Start Page",true,true,Integer.class,"many-to-one",IBPage.class);
    addAttribute(getColumnStartTemplate(),"Start Template",true,true,Integer.class,"many-to-one",IBPage.class);
//    this.addManyToManyRelationShip(Group.class);
//    addAttribute(COLUMNNAME_GROUP_ID,"Group ID",true,true,Integer.class,"one-to-one",Group.class);

  }

  public static IBDomain getDomain(int id)throws SQLException {
    IBDomain theReturn;
    theReturn = (IBDomain)getDomainsMap().get(new Integer(id));
    if (theReturn == null) {
      theReturn = ((IBDomainHome)IDOLookup.getHomeLegacy(IBDomain.class)).findByPrimaryKeyLegacy(id);
      if (theReturn != null) {
        getDomainsMap().put(new Integer(id),theReturn);
      }
    }
    return(theReturn);
  }

  private static Map getDomainsMap() {
    if (cachedDomains==null) {
      cachedDomains = new HashMap();
    }
    return(cachedDomains);
  }

  public void insertStartData() throws Exception {
    BuilderLogic instance = BuilderLogic.getInstance();
    IBDomainHome dHome = (IBDomainHome)getIDOHome(IBDomain.class);
    IBDomain domain = dHome.create();
    domain.setName("Default Site");

	IBPageHome pageHome = (IBPageHome)getIDOHome(IBPage.class);

    IBPage page = pageHome.create();
    page.setName("Web root");
    page.setType(com.idega.builder.data.IBPageBMPBean.PAGE);
    page.store();
    instance.unlockRegion(Integer.toString(page.getID()),"-1",null);

    IBPage page2 = pageHome.create();
    page2.setName("Default Template");
    page2.setType(com.idega.builder.data.IBPageBMPBean.TEMPLATE);
    page2.store();

    instance.unlockRegion(Integer.toString(page2.getID()),"-1",null);

    page.setTemplateId(page2.getID());
    page.store();

    domain.setIBPage(page);
    domain.setStartTemplate(page2);
    domain.store();

    instance.setTemplateId(Integer.toString(page.getID()),Integer.toString(page2.getID()));
    instance.getIBXMLPage(page2.getID()).addUsingTemplate(Integer.toString(page.getID()));
  }

  public String getEntityName() {
    return(tableName);
  }

  public static String getColumnDomainName() {
    return(domain_name);
  }

  public static String getColumnURL() {
    return(domain_url);
  }

  public static String getColumnStartPage() {
    return(start_page);
  }

  public static String getColumnStartTemplate() {
    return(start_template);
  }

  public IBPage getStartPage() {
    return((IBPage)getColumnValue(getColumnStartPage()));
  }

  public int getStartPageID() {
    return(getIntColumnValue(getColumnStartPage()));
  }

  public IBPage getStartTemplate() {
    return((IBPage)getColumnValue(getColumnStartTemplate()));
  }

  public int getStartTemplateID() {
    return(getIntColumnValue(getColumnStartTemplate()));
  }

//  public Group getGroup() {
//    return((Group)getColumnValue(COLUMNNAME_GROUP_ID));
//  }
//
//  public int getGroupID() {
//    return(getIntColumnValue(COLUMNNAME_GROUP_ID));
//  }

  public String getName() {
    return(getDomainName());
  }

  public String getDomainName() {
    return(getStringColumnValue(getColumnDomainName()));
  }

  public String getURL() {
    return(getStringColumnValue(getColumnURL()));
  }

  public Collection getTopLevelGroupsUnderDomain() throws IDORelationshipException, RemoteException, FinderException{

    Collection relations = ((GroupDomainRelationHome)IDOLookup.getHome(GroupDomainRelation.class)).findGroupsRelationshipsUnderDomainByRelationshipType(this.getID(),GroupDomainRelationTypeBMPBean.RELATION_TYPE_TOP_NODE);
//TODO do this in one sql command like in groupbmpbean and grouprelation
    Iterator iter = relations.iterator();
    Collection groups = new Vector();
    while (iter.hasNext()) {
      GroupDomainRelation item = (GroupDomainRelation)iter.next();
        groups.add(item.getRelatedGroup());
    }

    return groups;
  }

  public void setIBPage(IBPage page) {
     setColumn(getColumnStartPage(),page);
  }

  public void setStartTemplate(IBPage template) {
    setColumn(getColumnStartTemplate(),template);
  }

//  public void setGroup(Group group) {
//     setColumn(COLUMNNAME_GROUP_ID,group);
//  }

  public void setName(String name) {
    setColumn(getColumnDomainName(),name);
  }

  public Collection ejbFindAllDomains() throws FinderException {
    String sql = "select * from " + getTableName();
    return super.idoFindIDsBySQL(sql);
  }
}