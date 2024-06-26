package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PubSubService {


    /*
    todo remove code that posts to pubsub
    todo remove test ui thing
    todo put code in proper classes, put classes in proper directories
    todo put a lot of stuff into config/env variables
    todo tests
     */

    Logger logger = LoggerFactory.getLogger(PubSubService.class);

    private static final ObjectMapper OBJECT_MAPPER;
    public static final String TOPIC = "EventGroupProject"; //"testTopic";
    public static final String SUBSCRIPTION = "CheckEventSub"; //"testSubscription";

    static {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

        OBJECT_MAPPER = mapper;
    }



    @Autowired
    EventsRepository eventsRepository;

    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(
            @Qualifier("pubsubInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, SUBSCRIPTION);
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
            logger.info("Message arrived! Payload test: " + payload); //todo remove test logs

            /*
            business logic:
            on valid request where
                there is an event matching id in db
                event in db has at least as many available tickets as the request
                then
                    decrement event in db, log
             else
                log error, no change to db
             */

            try {
                EventDto eventDto = OBJECT_MAPPER.readValue(payload, EventDto.class);

                //get matching thing from db
                Optional<EventModel> byId = eventsRepository.findById(eventDto.eventId());
                if (byId.isPresent()) {
                    EventModel eventModel = byId.get();
                    if (eventModel.numberoftickets >= eventDto.numberOfTickets()) {
                        EventModel save = eventsRepository.save(new EventModel(eventDto.eventId(), eventDto.eventName(), eventModel.numberoftickets - eventDto.numberOfTickets(), eventDto.email()));
                        logger.info("Order successful, number of tickets for event " +save.name +" remaining: " + save.numberoftickets);
                    } else {
                        logger.info("Not enough tickets in db (" + eventModel.numberoftickets + ") to complete order: " + eventDto);
                    }
                } else {
                    logger.info("Event matching ID not present in DB: " + eventDto);
                }

            } catch (JsonProcessingException e) { //todo handle errors correctly
                logger.info("object mapper fail " + e.getMessage());
            } catch (Exception e) {
                logger.error("save failed " + e.getMessage());
            }

            BasicAcknowledgeablePubsubMessage originalMessage =
                    message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);

            originalMessage.ack();
        };
    }

    @Bean
    @ServiceActivator(inputChannel = "pubsubOutputChannel")
    public MessageHandler messageSender(PubSubTemplate pubsubTemplate) {
        return new PubSubMessageHandler(pubsubTemplate, TOPIC);
    }

    @MessagingGateway(defaultRequestChannel = "pubsubOutputChannel")
    public interface PubsubOutboundGateway {

        void sendToPubsub(String text);
    }
}
