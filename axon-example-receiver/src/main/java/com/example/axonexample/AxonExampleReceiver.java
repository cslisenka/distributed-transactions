package com.example.axonexample;

import com.rabbitmq.client.Channel;
import org.axonframework.amqp.eventhandling.DefaultAMQPMessageConverter;
import org.axonframework.amqp.eventhandling.spring.SpringAMQPMessageSource;
import org.axonframework.serialization.Serializer;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication
public class AxonExampleReceiver {

	public static void main(String[] args) {
		SpringApplication.run(AxonExampleReceiver.class, args);
	}

	@Bean
	public SpringAMQPMessageSource statisticsSource(Serializer serializer) {
		return new SpringAMQPMessageSource(new DefaultAMQPMessageConverter(serializer)) {
		    @RabbitListener(queues = "ComplaintEvents")
            @Override
            public void onMessage(Message message, Channel channel) throws Exception {
                super.onMessage(message, channel);
            }
        };
	}
}