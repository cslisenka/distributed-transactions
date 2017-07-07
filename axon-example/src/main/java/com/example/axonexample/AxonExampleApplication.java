package com.example.axonexample;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication
public class AxonExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(AxonExampleApplication.class, args);
	}

	// Queue inside message broker
	@Bean
	public Queue queue() {
		return QueueBuilder.durable("ComplaintEvents").build();
	}

	// TODO what is this?
	@Bean
	public Exchange exchange() {
		return ExchangeBuilder.fanoutExchange("ComplaintEvents").build();
	}

	// Links queue and exchange
	@Bean
	public Binding binding() {
		// routing doesn't matter - it says which events do we want to send into queue
		return BindingBuilder.bind(queue()).to(exchange()).with("*").noargs();
	}

	@Bean
	@Autowired
	public InitializingBean configure(AmqpAdmin admin) {
	    return () -> {
            admin.declareQueue(queue());
            admin.declareExchange(exchange());
            admin.declareBinding(binding());
        };
	}
}