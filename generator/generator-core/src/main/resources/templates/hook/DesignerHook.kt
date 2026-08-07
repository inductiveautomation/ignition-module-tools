package <PACKAGE_ROOT>.designer

import com.inductiveautomation.ignition.common.licensing.LicenseState
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook
import com.inductiveautomation.ignition.designer.model.DesignerContext


/**
 * This is the Designer-scope module hook.  The minimal implementation contains a startup method.
 */
class <MODULE_CLASSNAME>DesignerHook : AbstractDesignerModuleHook() {

    // override additional methods as required

    @Throws(Exception::class)
    override fun startup(context: DesignerContext, activationState: LicenseState) {
        // implement functionality as required
    }

    override fun shutdown() {
        // cleanup as required
    }
}
