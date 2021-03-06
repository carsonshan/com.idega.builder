package com.idega.builder.servlet;

import java.io.File;
import com.idega.builder.business.BuilderLogic;
import com.idega.presentation.IWContext;
import com.idega.presentation.Page;
import com.idega.servlet.IWPresentationServlet;

/**
 * Title:        idegaclasses
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      idega
 * @author
 * @version 1.0
 */

public class IBSimpleXMLReaderServlet extends IWPresentationServlet {

  public IBSimpleXMLReaderServlet(){
  }


  public void initializePage(){
    //String servletName = this.getServletConfig().getServletName();
    //System.out.println("Inside initializePage for "+servletName);
    IWContext iwc = getIWContext();
    boolean builderview=false;
    if(iwc.isParameterSet("view")){
      //if(iwc.getParameter("view").equals("builder")){
        builderview=true;
      //}
    }
    String fileName = iwc.getRequestURI();
    String prefix = iwc.getIWMainApplication().getApplicationRealPath();

    Page page = null;
    String pageKey = null;
    try{
      pageKey = prefix+File.separator+fileName;
      //page = getBuilderLogic().getPageCacher().getPage(pageKey);
      page = getBuilderLogic().getPageCacher().getComponentBasedPage(pageKey).getNewPageCloned();
    }
    catch(RuntimeException e){
      e.printStackTrace();
    }
    if(builderview){
      setPage(BuilderLogic.getInstance().getBuilderTransformed(pageKey,page,iwc));
    }
    else{
      setPage(page);
    }
    //setPage(new Page());
  }


  protected BuilderLogic getBuilderLogic(){
  	return BuilderLogic.getInstance();
  }


}
