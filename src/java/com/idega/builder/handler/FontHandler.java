/*
 * $Id: FontHandler.java,v 1.5 2004/06/28 11:18:12 thomas Exp $
 *
 * Copyright (C) 2001 Idega hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 *
 */
package com.idega.builder.handler;

import java.util.List;
import com.idega.core.builder.data.ICPropertyHandler;
import com.idega.presentation.PresentationObject;
import com.idega.presentation.IWContext;
import com.idega.presentation.ui.DropdownMenu;
import com.idega.presentation.text.Text;

/**
 * @author <a href="tryggvi@idega.is">Tryggvi Larusson</a>
 * @version 1.0
 */
public class FontHandler implements ICPropertyHandler {
  /**
   *
   */
  public FontHandler() {
  }

  /**
   *
   */
  public List getDefaultHandlerTypes() {
    return(null);
  }

  /**
   *
   */
  public PresentationObject getHandlerObject(String name, String value, IWContext iwc) {
    DropdownMenu menu = new DropdownMenu(name);
    menu.addMenuElement("","Select:");
    menu.addMenuElement(Text.FONT_FACE_ARIAL,"Arial-Helvetica");
    menu.addMenuElement(Text.FONT_FACE_TIMES,"Times");
    menu.addMenuElement(Text.FONT_FACE_COURIER,"Courier");
    menu.addMenuElement(Text.FONT_FACE_GENEVA,"Geneva");
    menu.addMenuElement(Text.FONT_FACE_VERDANA,"Verdana");
    menu.setSelectedElement(value);
    return(menu);
  }

  /**
   *
   */
  public void onUpdate(String values[], IWContext iwc) {
  }
}
