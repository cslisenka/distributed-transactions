package com.slisenko.examples.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;

@Configuration
@EnableStateMachine
public class StateMachineConfig extends EnumStateMachineConfigurerAdapter<OrderState, OrderEvent> {

    @Override
    public void configure(StateMachineConfigurationConfigurer<OrderState, OrderEvent> config) throws Exception {
        config.withConfiguration()
            .autoStartup(true).listener(listener());
    }

    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states.withStates()
            .initial(OrderState.NEW)
            .states(EnumSet.allOf(OrderState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions) throws Exception {
        transitions
            .withExternal()
                .source(OrderState.NEW).target(OrderState.CANCELLED).event(OrderEvent.CANCEL)
                .and()
            .withExternal()
                .source(OrderState.WAITING_FOR_PAYMENT).target(OrderState.CANCELLED).event(OrderEvent.CANCEL)
                .and()
            .withExternal()
                .source(OrderState.NEW).target(OrderState.WAITING_FOR_PAYMENT).event(OrderEvent.PAY)
                .and()
            .withExternal()
                .source(OrderState.WAITING_FOR_PAYMENT).target(OrderState.COMPLETED).event(OrderEvent.COMPLETE)
                .and()
            .withExternal()
                .source(OrderState.WAITING_FOR_PAYMENT).target(OrderState.REJECTED).event(OrderEvent.REJECT);
    }

    @Bean
    public StateMachineListener<OrderState, OrderEvent> listener() {
        return new StateMachineListenerAdapter<OrderState, OrderEvent>() {
            @Override
            public void stateChanged(State<OrderState, OrderEvent> from, State<OrderState, OrderEvent> to) {
                System.out.println("State changed from " + from + " to " + to);
            }
        };
    }
}