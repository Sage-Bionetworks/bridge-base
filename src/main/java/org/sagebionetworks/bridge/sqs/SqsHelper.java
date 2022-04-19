package org.sagebionetworks.bridge.sqs;

import java.util.List;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sagebionetworks.bridge.json.DefaultObjectMapper;

/** Helper class to wrap polling SQS and deleting messages. */
public class SqsHelper {
    private static final Logger LOG = LoggerFactory.getLogger(SqsHelper.class);

    private AmazonSQS sqsClient;

    /** SQS client. */
    public final void setSqsClient(AmazonSQS sqsClient) {
        this.sqsClient = sqsClient;
    }

    /**
     * Blocking call that polls SQS with a 20 second timeout. Returns at most 1 message. Returns null if no messages
     * are available within that 20 second timeout.
     *
     * @param sqsQueueUrl
     *         queue URL to poll
     * @return an SQS message, or null if no messages are available
     */
    public Message poll(String sqsQueueUrl) {
        ReceiveMessageResult sqsResult = sqsClient.receiveMessage(new ReceiveMessageRequest().withQueueUrl(sqsQueueUrl)
                .withMaxNumberOfMessages(1).withWaitTimeSeconds(20));

        List<Message> sqsMessageList = sqsResult.getMessages();
        int numMessages = sqsMessageList.size();
        if (numMessages == 0) {
            // Poll returned no messages. This is normal. Return null to signal no message.
            return null;
        } else if (numMessages > 1) {
            LOG.warn("Asked SQS for at most 1 message, but got " + numMessages + ", ignoring all but the first");
        }

        Message sqsMessage = sqsMessageList.get(0);
        return sqsMessage;
    }

    /**
     * Deletes the message from SQS. Should only be called after processing is complete, to guarantee at-least-once
     * semantics.
     *
     * @param sqsQueueUrl
     *         queue URL to poll
     * @param receiptHandle
     *         SQS message receipt handle
     */
    public void deleteMessage(String sqsQueueUrl, String receiptHandle) {
        sqsClient.deleteMessage(sqsQueueUrl, receiptHandle);
    }

    /**
     * Wrapper for sending a message to an SQS queue, using Jackson serialization to convert that object into JSON.
     *
     * @param sqsQueueUrl
     *         queue URL to send message to
     * @param messageObject
     *         object to send as JSON
     * @param delaySeconds
     *         optional delay in seconds before the message is made available in SQS
     */
    public void sendMessageAsJson(String sqsQueueUrl, Object messageObject, Integer delaySeconds)
            throws JsonProcessingException {
        String messageBody = DefaultObjectMapper.INSTANCE.writeValueAsString(messageObject);
        SendMessageRequest req = new SendMessageRequest(sqsQueueUrl, messageBody).withDelaySeconds(delaySeconds);
        sqsClient.sendMessage(req);
    }
}
