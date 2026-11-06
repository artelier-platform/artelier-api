package com.artelier.api.integration.sms;


public interface SmsSender { void send(String phoneNumber, String message); }
