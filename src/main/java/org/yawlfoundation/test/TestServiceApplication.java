package org.yawlfoundation.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.metrics.writer.GaugeWriter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.yawlfoundation.yawl.engine.interfce.interfaceB.InterfaceBWebsideController;
import org.yawlfoundation.yawl.engine.interfce.interfaceB.InterfaceB_EngineBasedClient;
import org.yawlfoundation.yawl.engine.interfce.interfaceB.InterfaceB_EnvironmentBasedServer;
import springframework.boot.actuate.metrrics.InfluxDBGaugeWriter;

import javax.servlet.http.HttpServlet;

/**
 * Created by gary on 26/02/2017.
 */
@SpringBootApplication
public class TestServiceApplication {



    public static void main(String[] args) {
        SpringApplication.run(TestServiceApplication.class, args);

        InterfaceB_EngineBasedClient client=new InterfaceB_EngineBasedClient();

    }



    @Bean
    ServletRegistrationBean testServletRegistrationBean(){
        InterfaceB_EnvironmentBasedServer servlet=new InterfaceB_EnvironmentBasedServer();


        servlet.set_controller(new TestService(influxDBGaugeWriter()));
        return new ServletRegistrationBean(servlet,"/test/*");
    }




    @Bean
    public InfluxDBGaugeWriter influxDBGaugeWriter(){
        return new InfluxDBGaugeWriter();
    }



}
