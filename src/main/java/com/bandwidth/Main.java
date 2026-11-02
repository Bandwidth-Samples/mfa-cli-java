package com.bandwidth;

import com.bandwidth.sdk.ApiClient;
import com.bandwidth.sdk.api.MfaApi;
import com.bandwidth.sdk.model.CodeRequest;
import com.bandwidth.sdk.model.VerifyCodeRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;

public class Main {

    private static final String SCOPE = "sample";

    private static final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

    private static String prompt(String text) throws IOException {
        System.out.print(text);
        System.out.flush();
        String line = stdin.readLine();
        if (line == null) {
            System.exit(1);
        }
        return line.strip();
    }

    public static void main(String[] args) throws IOException {
        String accountId = System.getenv("BW_ACCOUNT_ID");
        String clientId = System.getenv("BW_CLIENT_ID");
        String clientSecret = System.getenv("BW_CLIENT_SECRET");
        String number = System.getenv("BW_NUMBER");
        String voiceApplicationId = System.getenv("BW_VOICE_APPLICATION_ID");
        String messagingApplicationId = System.getenv("BW_MESSAGING_APPLICATION_ID");
        if (accountId == null || clientId == null || clientSecret == null || number == null
                || voiceApplicationId == null || messagingApplicationId == null) {
            System.err.println("Please set the environmental variables defined in the README");
            System.exit(1);
        }

        MfaApi mfaApi = new MfaApi(new ApiClient(clientId, clientSecret, null));

        String phoneNumber = prompt("Enter the phone number to send the code to (E.164, e.g. +15554443333): ");
        if (!phoneNumber.matches("\\+[1-9]\\d{1,14}")) {
            System.err.println("Phone number must be in E.164 format, e.g. +15554443333");
            System.exit(1);
        }

        String method = prompt("Send the code by messaging or voice? [messaging]: ").toLowerCase();
        if (method.isEmpty()) {
            method = "messaging";
        }
        if (!method.equals("messaging") && !method.equals("voice")) {
            System.err.println("Method must be messaging or voice");
            System.exit(1);
        }

        CodeRequest codeRequest = new CodeRequest()
                .to(phoneNumber)
                .from(number)
                .applicationId(method.equals("voice") ? voiceApplicationId : messagingApplicationId)
                .scope(SCOPE)
                .message("Your temporary {NAME} {SCOPE} code is {CODE}")
                .digits(6);

        System.out.println("Sending a code by " + method + " to " + phoneNumber);
        try {
            if (method.equals("voice")) {
                mfaApi.generateVoiceCode(accountId, codeRequest);
            } else {
                mfaApi.generateMessagingCode(accountId, codeRequest);
            }
        } catch (Exception e) {
            System.err.println("Failed to send code: " + e.getMessage());
            System.exit(1);
        }

        VerifyCodeRequest verifyRequest = new VerifyCodeRequest()
                .to(phoneNumber)
                .scope(SCOPE)
                .expirationTimeInMinutes(new BigDecimal(3))
                .code(prompt("Enter the code you received: "));

        boolean valid = false;
        try {
            valid = Boolean.TRUE.equals(mfaApi.verifyCode(accountId, verifyRequest).getValid());
        } catch (Exception e) {
            System.err.println("Failed to verify code: " + e.getMessage());
            System.exit(1);
        }

        System.out.println(valid ? "Code verified" : "Code not verified");
        System.exit(valid ? 0 : 1);
    }
}
