import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.example.EventDto;
import org.example.EventModel;
import org.junit.jupiter.api.Test;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.integration.outbound.PubSubMessageHandler;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

public class MyTest {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

        OBJECT_MAPPER = mapper;
    }


    @Test
    public void testDeserializePubSubData() {
        final String data = "{\"eventId\":\"d1b9c2e0-3e1f-4e4a-9c8a-6e5f7a8b3f7a\",\"eventName\":\"Classical Symphony Gala\",\"numberOfTickets\":5,\"email\":\"alori.glo@gmail.com\"}";

        final PubsubMessage message = PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(data.getBytes()))
                .build();

        final String dataResult = message.getData().toStringUtf8();

        final EventDto extracted = deserialize(dataResult);
        EventModel eventModel = new EventModel(extracted.eventId(), extracted.eventName(), extracted.numberOfTickets(), extracted.email());
//        EventDto eventDto = OBJECT_MAPPER.readValue("{\"eventId\":\"d1b9c2e0-3e1f-4e4a-9c8a-6e5f7a8b3f7a\",\"eventName\":\"Classical Symphony Gala\",\"numberOfTickets\":5,\"email\":\"alori.glo@gmail.com\"}", EventDto.class);

        // Access to the event type.
//        final String eventType = resultData.getEvent_type();

        EventDto expected = new EventDto(UUID.fromString("d1b9c2e0-3e1f-4e4a-9c8a-6e5f7a8b3f7a"), "Classical Symphony Gala", 5, "alori.glo@gmail.com");
        assertEquals(expected, extracted);
    }

    private EventDto deserialize(final String value) {
        try {
            return OBJECT_MAPPER.readValue(value, EventDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error !!", e);
        }
    }

}