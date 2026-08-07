package <PACKAGE_ROOT>.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;

/**
 * Class which is instantiated by the Ignition platform when the module is loaded in the gateway scope.
 * Override additional AbstractGatewayModuleHook methods as needed for your module.
 */
public class <MODULE_CLASSNAME>GatewayHook extends AbstractGatewayModuleHook {
    /**
     * Called before startup. This is the chance for the module to add its extension points and update persistent
     * records and schemas. None of the managers will be started up at this point, but the extension point managers will
     * accept extension point types.
     */
    @Override
    public void setup(GatewayContext context) {

    }

    /**
     * Called to initialize the module. Will only be called once. Persistence interface is available, but only in
     * read-only mode.
     */
    @Override
    public void startup(LicenseState activationState) {

    }

    /**
     * Called to shutdown this module. Note that this instance will never be started back up - a new one will be created
     * if a restart is desired.
     */
    @Override
    public void shutdown() {

    }

    /**
     * @return {@code true} if this is a "free" module, i.e. it does not participate in the licensing system.
     */
    @Override
    public boolean isFreeModule() {
        return true;
    }
}
