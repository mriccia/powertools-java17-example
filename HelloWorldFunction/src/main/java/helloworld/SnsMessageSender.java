
package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import software.amazon.sns.AmazonSNSExtendedClient;
import software.amazon.sns.SNSExtendedClientConfiguration;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class SnsMessageSender implements RequestHandler<ScheduledEvent, String> {

    private static final Logger log = LogManager.getLogger(SnsMessageSender.class);

    private static final String queueUrl = System.getenv("QUEUE_URL");
    private static final String bucketName = System.getenv("BUCKET_NAME");
    private static final String topicArn = System.getenv("TOPIC_ARN");

    public static final SdkHttpClient HTTP_CLIENT = UrlConnectionHttpClient.create();
    private static final SnsClient snsClient = SnsClient.builder()
            .httpClient(HTTP_CLIENT)
            .build();
    private static final SetSubscriptionAttributesRequest subscriptionAttributesRequest = SetSubscriptionAttributesRequest.builder()
            .subscriptionArn(topicArn)
            .attributeName("RawMessageDelivery")
            .attributeValue("TRUE")
            .build();

    private static final S3Client s3Client = S3Client.builder().httpClient(HTTP_CLIENT).build();

    private static final int EXTENDED_STORAGE_MESSAGE_SIZE_THRESHOLD = 32;

    private static final SNSExtendedClientConfiguration snsExtendedClientConfiguration = new SNSExtendedClientConfiguration()
            .withPayloadSupportEnabled(s3Client, bucketName)
            .withPayloadSizeThreshold(EXTENDED_STORAGE_MESSAGE_SIZE_THRESHOLD);
    private static final AmazonSNSExtendedClient snsExtendedClient = new AmazonSNSExtendedClient(snsClient, snsExtendedClientConfiguration);

    private static final Random random = new SecureRandom();

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        LoggingUtils.defaultObjectMapper(objectMapper);
    }

    @Logging(logEvent = true)
    public String handleRequest(final ScheduledEvent input, final Context context) {
//        snsClient.setSubscriptionAttributes(subscriptionAttributesRequest);

        //Publish message via SNS with storage in S3
        final String message = "This message is stored in S3 as it exceeds the threshold of 32 bytes set above.";
        PublishResponse publish = snsExtendedClient.publish(PublishRequest.builder().topicArn(topicArn).message(message).build());

        log.info(publish);
        return "Success";
    }
}