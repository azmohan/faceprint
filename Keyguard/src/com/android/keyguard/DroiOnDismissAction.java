package com.android.keyguard;

public interface DroiOnDismissAction {

	//*/ Add by shijiachen 20150713 for compile  keyguard , DroiOnDismissAction was  moved from KeyguardHostView.java
	    /**
	     * @return true if the dismiss should be deferred
	     */
	    boolean onDismiss();
}
