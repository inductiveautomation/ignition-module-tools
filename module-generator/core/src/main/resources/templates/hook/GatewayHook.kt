package <PACKAGE_ROOT>.gateway

import com.inductiveautomation.ignition.common.expressions.ExpressionFunctionManager
import com.inductiveautomation.ignition.common.licensing.LicenseState
import com.inductiveautomation.ignition.common.script.ScriptManager
import com.inductiveautomation.ignition.common.xmlserialization.deserialization.XMLDeserializer
import com.inductiveautomation.ignition.common.xmlserialization.serialization.XMLSerializer
import com.inductiveautomation.ignition.gateway.AbstractGatewayModuleHook
import com.inductiveautomation.ignition.gateway.clientcomm.ClientReqSession
import com.inductiveautomation.ignition.gateway.web.models.INamedTab

/**
 * Gateway scoped hook implementation
 */
class <MODULE_CLASSNAME>GatewayHook: AbstractGatewayModuleHook() {


    fun getRPCHandler(session: ClientReqSession?, projectName: String?): Any? {
        return null
    }

    val homepagePanels: List<Any?>?
        get() = null

    val statusPanels: List<Any?>?
        get() = null

    fun configureDeserializer(deserializer: XMLDeserializer?) {

    }

    fun configureSerializer(serializer: XMLSerializer?) {

    }
    fun configureFunctionFactory(factory: ExpressionFunctionManager?) {

    }

    fun notifyLicenseStateChanged(licenseState: LicenseState?) {

    }

    fun initializeScriptManager(manager: ScriptManager?) {

    }
}
