package de.qabel.qabelbox.services;


import de.qabel.ackack.MessageInfo;
import de.qabel.core.drop.DropActor;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.module.Module;
import de.qabel.core.module.ModuleManager;

/**
 * ReceiverModule is launched by the QabelService to receive DropMessages from the DropActor.
 * A Module is used, to utilize the provided means of the qabel-core.
 */
public class ReceiverModule extends Module {

    private MessageReceivedInterface messageReceivedInterface;

    public ReceiverModule(ModuleManager moduleManager) {
        super(moduleManager);
    }

    public void setMessageReceivedInterface(MessageReceivedInterface messageReceivedInterface) {
        this.messageReceivedInterface = messageReceivedInterface;
    }

    @Override
    public void init() {
    }

    public void registerEvent(String event) {
        on(DropActor.EVENT_DROP_MESSAGE_RECEIVED_PREFIX + event, this);
    }

    @Override
    public void moduleMain() {
    }

    @Override
    public void onEvent(String event, MessageInfo info, Object... data) {
        if(data[0] instanceof DropMessage) {
            DropMessage dropMessage = (DropMessage) data[0];
            if (messageReceivedInterface != null) {
                messageReceivedInterface.onMessageReceived(dropMessage);
            }
        }
    }

    @Override
    public synchronized void stopModule() {
        super.stopModule();
    }

    public interface MessageReceivedInterface {
        void onMessageReceived(DropMessage dropMessage);
    }
}
