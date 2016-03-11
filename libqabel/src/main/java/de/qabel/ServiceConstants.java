package de.qabel;


/**
 * Interface for service related constants
 */
public interface ServiceConstants {

    String SERVICE_PACKAGE_NAME = "de.qabel.android";
    String SERVICE_CLASS_NAME = "de.qabel.android.service.QabelService";

    int MSG_REGISTER_ON_TYPE = 0;
    int MSG_DROP_MESSAGE = 1;

    String DROP_MESSAGE_TYPE = "DropMessageType";
    String DROP_MESSAGE = "DropMessage";

    String DROP_PAYLOAD_TYPE = "DropPayloadType";
    String DROP_PAYLOAD = "DropPayload";
    String DROP_RECIPIENT_ID = "DropRecipientId";
    String DROP_SENDER_ID = "DropSenderID";
}
