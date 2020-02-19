package com.bonial;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS Lambda function with S3 trigger.
 */
public class SummaryApp implements RequestHandler<S3Event, String> {

    static final Logger log = LoggerFactory.getLogger(SummaryApp.class);
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Handles S3 events
     *
     * @param s3Event
     * @param context
     * @return status message
     */
    @Override
    public String handleRequest(S3Event s3Event, Context context) {
        log.info("Received Lambda event:{}", s3Event);
        //read bucket details
        S3EventNotificationRecord record = s3Event.getRecords().get(0);
        String srcBucket = record.getS3().getBucket().getName();
        String srcKey = record.getS3().getObject().getUrlDecodedKey();
        String partnerId = srcKey.substring(srcKey.indexOf('/'));
        String dstKey = "summary" + partnerId;
        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

        try {
            //Read transport data from the bucket
            List<Map<String, Object>> transports = readTransportRecords(s3Client, srcBucket, srcKey);

            //calculate transport mobility summary data
            String summaryJson = calculatePassengersOnTransport(transports);
            log.debug("Summary:{}", summaryJson);

            //Write transport mobility summary data to the same bucket
            writeSummaryJson(s3Client, srcBucket, dstKey, summaryJson);

        } catch (IOException io) {
            log.error("Error processing json.");
            return io.getMessage();
        }

        return "Summary uploaded successfully!";
    }

    /**
     * Uploads summary json to the same bucket
     *
     * @param s3Client
     * @param srcBucket
     * @param dstKey
     * @param summaryJson
     * @throws UnsupportedEncodingException
     */
    public void writeSummaryJson(AmazonS3 s3Client, String srcBucket, String dstKey, String summaryJson) throws UnsupportedEncodingException {
        log.info("Uploading to:{}/{} ", srcBucket, dstKey);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/json");
        s3Client.putObject(srcBucket, dstKey, new StringInputStream(summaryJson), metadata);
    }

    /**
     * Calculates total number of passengers that each transport type is able to mobilize
     *
     * @param transports
     * @return summary json
     * @throws JsonProcessingException
     */
    public String calculatePassengersOnTransport(List<Map<String, Object>> transports) throws JsonProcessingException {
        Map<String, Integer> summary = new HashMap<>();
        summary.put("cars", carPassengers(transports));
        summary.put("trains", trainPassengers(transports));
        summary.put("planes", planePassengers(transports));
        return mapper.writeValueAsString(summary);
    }

    /**
     * Reads transport mobility records on each partner
     *
     * @param s3Client
     * @param srcBucket
     * @param srcKey
     * @return list of transport records
     * @throws IOException
     */
    public List<Map<String, Object>> readTransportRecords(AmazonS3 s3Client, String srcBucket, String srcKey) throws IOException {
        log.info("Reading from:{}/{} ",srcBucket, srcKey);
        S3Object s3Object = s3Client.getObject(new GetObjectRequest(srcBucket, srcKey));
        InputStream inputStream = s3Object.getObjectContent();
        Map<String, Object> inputJson = mapper.readValue(inputStream, new TypeReference<HashMap<String, Object>>() {
        });
        log.debug("Transport records:{}", inputJson);
        return (List<Map<String, Object>>) inputJson.get("transports");
    }

    /**
     * calculate the total number of passengers on cars
     *
     * @param transports
     * @return total no of pax on cars
     */
    public Integer carPassengers(List<Map<String, Object>> transports) {
        return transports.stream()
                .filter(transport -> transport.containsKey("passenger-capacity"))
                .mapToInt(transport -> (int) transport.get("passenger-capacity"))
                .sum();
    }

    /**
     * calculate the total number of passengers on trains
     *
     * @param transports
     * @return total no of pax on trains
     */
    public Integer trainPassengers(List<Map<String, Object>> transports) {
        return transports.stream()
                .filter(transport -> transport.containsKey("number-wagons") && transport.containsKey("w-passenger-capacity"))
                .mapToInt(transport -> ((int) transport.get("number-wagons")) * ((int) transport.get("w-passenger-capacity")))
                .sum();
    }

    /**
     * calculate the total number of passengers on planes - Assuming plane object either have both or one of them(e-passenger-capacity and b-passenger-capacity)
     *
     * @param transports
     * @return total no of pax on planes
     */
    public Integer planePassengers(List<Map<String, Object>> transports) {
        return transports.stream()
                .filter(transport -> transport.containsKey("b-passenger-capacity") || transport.containsKey("e-passenger-capacity"))
                .mapToInt(transport -> ((int) transport.getOrDefault("b-passenger-capacity", 0)) + ((int) transport.getOrDefault("e-passenger-capacity", 0)))
                .sum();
    }


}