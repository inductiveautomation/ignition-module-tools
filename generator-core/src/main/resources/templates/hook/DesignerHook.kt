package <PACKAGE_ROOT>.designer

import com.inductiveautomation.ignition.common.licensing.LicenseState
import com.inductiveautomation.ignition.designer.model.AbstractDesignerModuleHook
import com.inductiveautomation.ignition.designer.model.DesignerContext


/**
 * This is the Designer-scope module hook.  The minimal implementation contains a startup method.
 */
class <MODULE_CLASSNAME>DesignerHook: AbstractDesignerModuleHook() {

    // override additonal methods as requried

    @Throws(Exception)
    override fun startup(context: DesignerContext, activationState: LicenseState) {
        // implelement functionality as required
    }
}
