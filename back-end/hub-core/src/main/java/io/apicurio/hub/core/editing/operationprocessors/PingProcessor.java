package io.apicurio.hub.core.editing.operationprocessors;

import io.apicurio.hub.core.editing.ApiDesignEditingSession;
import io.apicurio.hub.core.editing.sessionbeans.BaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.websocket.Session;

/**
 * @author Marc Savy {@literal <marc@rhymewithgravy.com>}
 */
@Singleton
public class PingProcessor implements IOperationProcessor {

    private static Logger logger = LoggerFactory.getLogger(PingProcessor.class);

    @Override
    public void process(ApiDesignEditingSession editingSession, Session session, BaseOperation bo) {
        logger.debug("PING message received."); // TODO expand logging -- careful with session id
    }

    @Override
    public String getOperationName() {
        return "ping";
    }

    @Override
    public Class<? extends BaseOperation> unmarshallKlazz() {
        return BaseOperation.class;
    }
}
