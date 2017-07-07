package com.slisenko.examples.test;

public enum OrderState {

    NEW, // start state -> pay, cancel
    CANCELLED, // finish state
    REJECTED, // finish state
    WAITING_FOR_PAYMENT, // -> completed, rejected
    COMPLETED // finish state
}
