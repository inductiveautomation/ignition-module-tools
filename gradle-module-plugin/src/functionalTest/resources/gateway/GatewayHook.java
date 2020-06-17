package <PACKAGE_PATH>;

import java.util.List;

import com.inductiveautomation.ignition.common.expressions.ExpressionFunctionManager;
import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.common.script.ScriptManager;
import com.inductiveautomation.ignition.common.xmlserialization.deserialization.XMLDeserializer;
import com.inductiveautomation.ignition.common.xmlserialization.serialization.XMLSerializer;
import com.inductiveautomation.ignition.gateway.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.clientcomm.ClientReqSession;
import com.inductiveautomation.ignition.gateway.web.models.INamedTab;

class <MODULE_NAME>GatewayHook extends AbstractGatewayModuleHook {
    @Override
    public Object getRPCHandler(ClientReqSession session, String projectName) {
        return null;
    }

    @Override
    public List<? extends IHomepagePanelDescriptor> getHomepagePanels() {
        return null;
    }

    @Override
    public List<? extends INamedTab> getStatusPanels() {
        return null;
    }

    @Override
    public void configureDeserializer(XMLDeserializer deserializer) {

    }

    @Override
    public void configureSerializer(XMLSerializer serializer) {

    }

    @Override
    public void configureFunctionFactory(ExpressionFunctionManager factory) {

    }

    @Override
    public void notifyLicenseStateChanged(LicenseState licenseState) {

    }

    @Override
    public void initializeScriptManager(ScriptManager manager) {

    }
}
