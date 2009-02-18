package com.zimbra.cs.account.accesscontrol.generated;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.UserRight;

//
// DO NOT MODIFY - generated by RightManager
//
// To generate, under ZimbraServer, run: 
//     ant generate-rights
//


public abstract class ZUserRight {
    
    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 5.0 pshao 20090218-1439 */


    public static UserRight R_invite;
    public static UserRight R_loginAs;
    public static UserRight R_sendAs;
    public static UserRight R_viewFreeBusy;


    public static void init(RightManager rm) throws ServiceException {
        R_invite                               = rm.getUserRight(Right.RT_invite);
        R_loginAs                              = rm.getUserRight(Right.RT_loginAs);
        R_sendAs                               = rm.getUserRight(Right.RT_sendAs);
        R_viewFreeBusy                         = rm.getUserRight(Right.RT_viewFreeBusy);
    }

    ///// END-AUTO-GEN-REPLACE
}
