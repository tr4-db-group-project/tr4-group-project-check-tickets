package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

@SpringBootApplication
@EnableJpaRepositories
public class CheckTicketsApplication {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

        OBJECT_MAPPER = mapper;
    }


    /*
    get messages from correct topic
    don't send
    refactor - don't have everything in main class omg
    remove test ui thing
    db connection?
    todo model/object for post create booking request
    dockerise
    deploy to gcp
     */

    Logger logger = LoggerFactory.getLogger(CheckTicketsApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CheckTicketsApplication.class, args);
    }

    @Autowired
    EventsRepository eventsRepository;

    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(
            @Qualifier("pubsubInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, "testSubscription");
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);

        return adapter;
    }

    @Bean
    public MessageChannel pubsubInputChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "pubsubInputChannel")
    public MessageHandler messageReceiver() {
        return message -> {
            String payload = new String((byte[]) message.getPayload());
            logger.info("Message arrived! Payload test: " + payload);

            logger.info("testlog ??? ");

            EventDto eventDto;
            try {
                eventDto = OBJECT_MAPPER.readValue(payload, EventDto.class);
            } catch (JsonProcessingException e) {
                logger.info("object mapper fail");
                logger.error(e.getMessage());
                eventDto = new EventDto(UUID.randomUUID(), "fakeevent", 1, "fakeemail");
            }
            EventDto extracted = eventDto;

            logger.info("extracted " + extracted);

            EventModel eventModel = new EventModel(extracted.eventId(), extracted.eventName(), extracted.numberOfTickets(), extracted.email());

            logger.info("eventModel " + eventModel);

            try {
                EventModel save = eventsRepository.save(eventModel);
                logger.info("save succesful! <3 " + save);
            } catch (Exception e) {
                logger.error("save failed " + e.getMessage());
            }

            logger.info("db count = " + eventsRepository.count());

            //save to db

            BasicAcknowledgeablePubsubMessage originalMessage =
                    message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);

            originalMessage.ack();
        };
    }

    @Bean
    @ServiceActivator(inputChannel = "pubsubOutputChannel")
    public MessageHandler messageSender(PubSubTemplate pubsubTemplate) {
        return new PubSubMessageHandler(pubsubTemplate, "testTopic");
    }

    @MessagingGateway(defaultRequestChannel = "pubsubOutputChannel")
    public interface PubsubOutboundGateway {

        void sendToPubsub(String text);
    }

}