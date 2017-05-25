/*
 * Copyright (c) 2004-2012 The YAWL Foundation. All rights reserved.
 * The YAWL Foundation is a collaboration of individuals and
 * organisations who are committed to improving workflow technology.
 *
 * This file is part of YAWL. YAWL is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation.
 *
 * YAWL is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with YAWL. If not, see <http://www.gnu.org/licenses/>;.
 */

package org.yawlfoundation.test;

import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.util.EntityUtils;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.stereotype.Component;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.interfce.Marshaller;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.engine.interfce.interfaceB.InterfaceBWebsideController;
import org.yawlfoundation.yawl.engine.time.YTimer;
import org.yawlfoundation.yawl.exceptions.YAWLException;
import springframework.boot.actuate.metrrics.InfluxDBGaugeWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A simple service that provides for status updates to the YAWL Twitter account
 *
 * @author Michael Adams
 * @date 25/07/2009
 */
@Component
public class TestService extends InterfaceBWebsideController {

    // holds a session handle to the engine
    private String _handle = null;
    public static int count=0;
    private final Logger logger= LoggerFactory.getLogger(TestService.class);

    private final Map<String,Integer> counts=new HashMap<String, Integer>();

    private final Map<String,Long> last_task_timestamps=new HashMap<String, Long>();


    private final InfluxDBGaugeWriter writer;

    public TestService(InfluxDBGaugeWriter writer) {
        this.writer = writer;
    }

    private Executor executor=Executors.newCachedThreadPool();

    private Random random=new Random();

    public void handleEnabledWorkItemEvent(final WorkItemRecord wir) {
        try {

            // connect only if not already connected
            while (! connected()) _handle = connect("admin:1", "YAWL");

           final String handle=_handle;

            _interfaceBClient.asyncHandleWorkItem(wir.getID(), _handle, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse httpResponse) {
                    try {
                        //logger.info("aaa");
                        String msg = EntityUtils.toString(httpResponse.getEntity());
                        //result=_interfaceBClient.stripOuterElement(result);
                        WorkItemRecord resultItem;
                        if (successful(msg)) {
                            msg = _interfaceBClient.stripOuterElement(msg);
                            msg = _interfaceBClient.stripOuterElement(msg);
                            resultItem = Marshaller.unmarshalWorkItem(msg);
                            _ibCache.addWorkItem(resultItem);
                        }
                        else throw new RuntimeException(msg);

                        String caseId=wir.getRootCaseID();

                        int value=counts.get(caseId)==null?0:counts.get(caseId);
                        counts.put(caseId, value + 1);
                        if(last_task_timestamps.get(caseId)!=null){
                            writer.set(new Metric<Number>("time_span",System.currentTimeMillis()-last_task_timestamps.get(caseId)));
                        }

                        logger.info(caseId);



                        String data = counts.get(caseId) == 100 ? "Y" : "N";

                        TimeUnit.MILLISECONDS.sleep(Math.abs(random.nextInt(100)));


                        asyncCheckInWorkItem(resultItem.getID(),
                                resultItem.getDataList(),
                                getOutputData(resultItem.getTaskName(), data), null, handle);

                        last_task_timestamps.put(caseId,System.currentTimeMillis());
                    } catch (IOException | JDOMException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void failed(Exception e) {
                    logger.info(e.getMessage());
                }

                @Override
                public void cancelled() {
                    logger.info("cancel");
                }
            });



            /*
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    WorkItemRecord workItemRecord=wir;
                    String caseId=workItemRecord.getRootCaseID();

                    int value=counts.get(caseId)==null?0:counts.get(caseId);

                    try {
                        long start = System.currentTimeMillis();
                        workItemRecord = checkOut(workItemRecord.getID(), handle);
                        writer.set(new Metric<Number>("response_time", System.currentTimeMillis() - start));

                        counts.put(caseId, value + 1);
                        if(last_task_timestamps.get(caseId)!=null){
                            writer.set(new Metric<Number>("time_span",System.currentTimeMillis()-last_task_timestamps.get(caseId)));
                        }


                        String data = counts.get(caseId) == 100 ? "Y" : "N";

                        TimeUnit.MILLISECONDS.sleep(Math.abs(random.nextInt(100)));

                        start = System.currentTimeMillis();
                        checkInWorkItem(workItemRecord.getID(),
                                workItemRecord.getDataList(),
                                getOutputData(workItemRecord.getTaskName(), data), null, handle);

                        last_task_timestamps.put(caseId,System.currentTimeMillis());
                        writer.set(new Metric<Number>("response_time", System.currentTimeMillis() - start));
                    }catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (IOException | JDOMException | YAWLException e) {
                        throw new RuntimeException(e);
                    }

                    logger.info(String.format("caseId is %s, count is %d",caseId,value));
                }
            });
            */

        }
        catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }

    // have to implement abstract method, but have no need for this event
    public void handleCancelledWorkItemEvent(WorkItemRecord workItemRecord) {  }


    // these parameters are automatically inserted (in the Editor) into a task
    // decomposition when this service is selected from the list
    public YParameter[] describeRequiredParams() {
        YParameter[] params = new YParameter[2];
        params[0] = new YParameter(null, YParameter._INPUT_PARAM_TYPE);
        params[0].setDataTypeAndName("string", "data", XSD_NAMESPACE);


        params[1] = new YParameter(null, YParameter._OUTPUT_PARAM_TYPE);
        params[1].setDataTypeAndName("string", "data", XSD_NAMESPACE);

        return params;
    }


    //********************* PRIVATE METHODS *************************************//




    private Element getOutputData(String taskName,String data){
        Element output=new Element(taskName);
        Element result=new Element("data");
        result.setText(data);
        output.addContent(result);
        return output;
    }




    private boolean connected() throws IOException {
        return _handle != null && checkConnection(_handle);
    }


}
