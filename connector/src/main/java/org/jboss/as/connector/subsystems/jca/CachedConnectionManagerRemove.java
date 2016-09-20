package org.jboss.as.connector.subsystems.jca;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="ingo@redhat.com">Ingo Weiss</a>
 */
class CachedConnectionManagerRemove extends AbstractRemoveStepHandler {

    static final CachedConnectionManagerRemove INSTANCE = new CachedConnectionManagerRemove();

    private CachedConnectionManagerRemove() {}

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        // We need to undefine any parameters and then set install to false, basically resetting the CCM settings
        model.get(JcaCachedConnectionManagerDefinition.CcmParameters.DEBUG.getAttribute().getName()).set(ModelType.UNDEFINED);
        model.get(JcaCachedConnectionManagerDefinition.CcmParameters.ERROR.getAttribute().getName()).set(ModelType.UNDEFINED);
        model.get(JcaCachedConnectionManagerDefinition.CcmParameters.IGNORE_UNKNOWN_CONNECTIONS.getAttribute().getName()).set(ModelType.UNDEFINED);
        model.get(JcaCachedConnectionManagerDefinition.CcmParameters.INSTALL.getAttribute().getName()).set(false);
        context.reloadRequired();
    }
}
