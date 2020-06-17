package le.examp;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook;
import com.inductiveautomation.ignition.designer.model.DesignerContext;


/**
 * This is the Designer-scope module hook used for tests.
 */
public class DesignerHook extends AbstractDesignerModuleHook {

    public static final String MODULE_ID = "component-example";

    @Override
    public void startup(DesignerContext context, LicenseState activationState) throws Exception {
        // noop
    }
}
