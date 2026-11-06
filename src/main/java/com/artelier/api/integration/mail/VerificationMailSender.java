package com.artelier.api.integration.mail;

public interface VerificationMailSender { void sendEmailVerification(String to, String token); }
