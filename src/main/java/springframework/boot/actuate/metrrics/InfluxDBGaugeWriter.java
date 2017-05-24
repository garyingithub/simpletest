package springframework.boot.actuate.metrrics;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.GaugeWriter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gary on 18/05/2017.
 */

public class InfluxDBGaugeWriter implements GaugeWriter {


    public InfluxDBGaugeWriter(){

    }

    private final CloseableHttpClient client= HttpClients.createDefault();

    @Value("${influxdb.server.address}")
    private String uri;

    @Value("${influxdb.db.name}")
    private String dbName;

    private final Logger logger= LoggerFactory.getLogger(InfluxDBGaugeWriter.class);

    public void  sendRecords(List<Record> buffer){

        StringBuilder builder=new StringBuilder();

        for(Record record:buffer){

            builder.append(record);
            builder.append("\n");


        }
        // System.out.println(builder.toString());
        try {
            sendData(
                    dbName,
                    builder.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
            }
        //logger.info(builder.toString());


    }


    public void sendData(String dbName,String content) throws UnsupportedEncodingException {


        String tempUri=uri;
        if(!tempUri.startsWith("http")){
            tempUri="http://"+uri+"/write?db="+dbName+"&precision=ms";
        }
        HttpPost httpPost=new HttpPost(tempUri);
        HttpEntity entity=new StringEntity(content);
        httpPost.setEntity(entity);
        HttpContext httpContext=new BasicHttpContext();
        CloseableHttpResponse response;
        try {
            //logger.info(content);

            response=client.execute(httpPost,httpContext);
            //logger.info(response.toString());
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private List<Record> buffer=new ArrayList<>();
    private final int bufferSize=200;
    public final String TOPIC="ENGINE-METRIC";


    @Override
    public void set(Metric<?> metric) {
        final String key=metric.getName();
        final String value=String.valueOf(metric.getValue());

        Map<String,String> tags=new HashMap<>();

        // self-defined metric
        if(key.contains(":")){

            String name=key.substring(0,key.indexOf(':'));
            tags.put("name",name);

            String[] pairs=key.substring(key.indexOf(':')+1).split(" ");

            for(String pair:pairs){

                String[] kv=pair.split("\\.");
                tags.put(kv[0],kv[1]);
            }


        }else {
            tags.put("name",key);
        }



      
        buffer.add(new Record(TOPIC,tags,value,String.valueOf(metric.getTimestamp().getTime())));


        if(buffer.size()>=bufferSize){
            List<Record> temp=buffer;
            buffer=new ArrayList<>();
            sendRecords(temp);

        }

    }






    class Record {

        private String measurement;

        private Map<String,String> tags;

        private String value;
        private String timestamp;

        public Record(String measurement, Map<String, String> tags, String value, String timestamp) {
            this.measurement = measurement;

            this.tags = tags;
            this.value = value;
            this.timestamp = timestamp;

        }

        @Override
        public String toString() {
            StringBuilder builder=new StringBuilder();
            builder.append(measurement);
            for(String key:tags.keySet()){
                builder.append(",");
                builder.append(key);
                builder.append("=");
                builder.append(tags.get(key));
            }

            builder.append(" ");
            builder.append("value=");
            builder.append(value);
            builder.append(" ");
            builder.append(timestamp);;
            return builder.toString();
        }

        public String getMeasurement() {
            return measurement;
        }

        public void setMeasurement(String measurement) {
            this.measurement = measurement;
        }


        public Map<String, String> getTags() {
            return tags;
        }

        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

}
