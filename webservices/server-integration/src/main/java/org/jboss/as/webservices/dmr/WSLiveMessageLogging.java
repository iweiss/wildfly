package org.jboss.as.webservices.dmr;

import org.apache.cxf.Bus;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.wsf.spi.deployment.Endpoint;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.AccessController;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Enables endpoint SOAP message logging on-the-fly
 *
 * @author <a href="mailto:ingo@redhat.com">Ingo Weiss</a>
 */
public class WSLiveMessageLogging implements OperationStepHandler {

    static final WSLiveMessageLogging INSTANCE = new WSLiveMessageLogging();
    static final String CXF_LOGGING_ENABLE = "org.apache.cxf.logging.enable";

    static final AttributeDefinition LIVE_MESSAGE_LOGGING = new SimpleAttributeDefinitionBuilder("enable-message-logging",
            ModelType.BOOLEAN, true).setDefaultValue(new ModelNode().set(false)).setAllowExpression(false).build();

    static final AttributeDefinition[] ATTRIBUTES = { LIVE_MESSAGE_LOGGING };

    private WSLiveMessageLogging() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.isNormalServer()) {
            context.addStep((operationContext, modelNodeOperation) -> {
                try {
                    enableEndpointLiveMessageLogging(context, operation);
                } catch (Exception ex) {
                    throw new OperationFailedException(WSLogger.ROOT_LOGGER.noMetricsAvailable() + ":" + ex.getMessage());
                }
            }, OperationContext.Stage.RUNTIME);
        } else {
            context.getResult().set(WSLogger.ROOT_LOGGER.noMetricsAvailable());
        }
    }

    private void enableEndpointLiveMessageLogging(final OperationContext context, final ModelNode operation)
            throws OperationFailedException {

        if ("read-attribute".equals(operation.get("operation").asString())) {
            return;
        }

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String endpointID;

        try {
            endpointID = URLDecoder.decode(address.getLastElement().getValue(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        final String webContext = endpointID.substring(0, endpointID.indexOf(":"));
        final String endpointName = endpointID.substring(endpointID.indexOf(":") + 1);
        ServiceName endpointServiceName = WSServices.ENDPOINT_SERVICE.append("context=" + webContext).append(endpointName);
        ServiceController<Endpoint> service = (ServiceController<Endpoint>) currentServiceContainer()
                .getService(endpointServiceName);
        Endpoint endpoint = service.getValue();

        if (endpoint == null) {
            throw new OperationFailedException(WSLogger.ROOT_LOGGER.noMetricsAvailable());
        }

        if ("write-attribute".equals(operation.get("operation").asString())) {
            Bus bus = endpoint.getService().getDeployment().getAttachment(org.apache.cxf.Bus.class);
            bus.setProperty(CXF_LOGGING_ENABLE, operation.get("value").asBoolean());
        }
    }

    private static ServiceContainer currentServiceContainer() {
        if (System.getSecurityManager() == null) {
            return CurrentServiceContainer.getServiceContainer();
        }
        return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
    }
}
