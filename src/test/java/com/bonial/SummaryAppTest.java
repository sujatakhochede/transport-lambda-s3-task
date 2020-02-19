package com.bonial;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
@RunWith(MockitoJUnitRunner.class)
public class SummaryAppTest {

    private final String CONTENT_TYPE = "application/json";

    private S3Event event;

    @Mock
    private AmazonS3 s3Client;
    @Mock
    private S3Object s3Object;

    private ObjectMapper mapper;

    private SummaryApp summaryApp;

    private List<Map<String, Object>> transports;


    @Before
    public void setUp() throws IOException {
        mapper = new ObjectMapper();
        summaryApp = new SummaryApp();
        Map<String, Object> inputJson = mapper.readValue(this.getClass().getResourceAsStream("/1.json"), new TypeReference<HashMap<String, Object>>() {
        });
        transports = (List<Map<String, Object>>) inputJson.get("transports");
    }

    @Test
    public void testCarPassengers() {
        assertEquals("Total no of passengers on car not matched!", Integer.valueOf(14), summaryApp.carPassengers(transports));
    }

    @Test
    public void testTrainPassengers() {
        assertEquals("Total no of passengers on train not matched!", Integer.valueOf(150), summaryApp.trainPassengers(transports));
    }

    @Test
    public void testPlanePassengers() {
        assertEquals("Total no of passengers on plane not matched!", Integer.valueOf(524), summaryApp.planePassengers(transports));
    }

    @Test
    public void testCalculatePassengersOnTransport() throws JsonProcessingException {
        assertNotNull("Failed to prepare summary json!", summaryApp.calculatePassengersOnTransport(transports));
    }


}