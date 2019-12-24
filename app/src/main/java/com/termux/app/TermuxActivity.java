package com.termux.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.termux.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class TermuxActivity extends Activity {
    // Seed for HMAC-SHA1 - 20 bytes
    String seed = "3132333435363738393031323334353637383930";
    // Seed for HMAC-SHA256 - 32 bytes
    String seed32 = "3132333435363738393031323334353637383930" +
        "313233343536373839303132";
    // Seed for HMAC-SHA512 - 64 bytes
    String seed64 = "3132333435363738393031323334353637383930" +
        "3132333435363738393031323334353637383930" +
        "3132333435363738393031323334353637383930" +
        "31323334";
    long T0 = 0;
    long X = 30;
    long testTime[] = {59L, 1111111109L, 1111111111L, 1234567890L, 2000000000L, 20000000000L};

    String steps = "0";
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.drawer_layout);
        Button button_java = findViewById(R.id.button_java);
        Button button_conversion = findViewById(R.id.button_conversion);
        Button button_modify_kotlin = findViewById(R.id.button_modify_kotlin);
        Button button_totp = findViewById(R.id.button_totp);
        Button button_totp_pure = findViewById(R.id.button_totp_pure);
        Button button_totp_kotlin = findViewById(R.id.button_totp_kotlin);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                if (id == R.id.button_java) {
                    TermuxInstaller.setupIfNeeded(TermuxActivity.this, null);
                } else if (id == R.id.button_conversion) {
                    //FIXME  java code is static void  modify
//                        TermuxInstallerK.setupIfNeeded(TermuxActivity.this, null);
                } else if (id == R.id.button_modify_kotlin) {
                    //FIXME  java code is static void  modify
//                        TermuxInstallerKM.setupIfNeeded(TermuxActivity.this, null);
                } else if (id == R.id.button_totp) {
                    try {
                        System.out.println(
                            "+---------------+-----------------------+" +
                                "------------------+--------+--------+");
                        System.out.println(
                            "|  Time(sec)    |   Time (UTC format)   " +
                                "| Value of T(Hex)  |  TOTP  | Mode   |");
                        System.out.println(
                            "+---------------+-----------------------+" +
                                "------------------+--------+--------+");

                        for (int i=0; i<testTime.length; i++) {
                            long T = (testTime[i] - T0)/X;
                            steps = Long.toHexString(T).toUpperCase();
                            while (steps.length() < 16) steps = "0" + steps;
                            String fmtTime = String.format("%1$-11s", testTime[i]);
                            String utcTime = df.format(new Date(testTime[i]*1000));
                            System.out.print("|  " + fmtTime + "  |  " + utcTime +
                                "  | " + steps + " |");
                            System.out.println(TOTPAndroid.generateTOTP(seed, steps, "8",
                                "HmacSHA1") + "| SHA1   |");
                            System.out.print("|  " + fmtTime + "  |  " + utcTime +
                                "  | " + steps + " |");
                            System.out.println(TOTPAndroid.generateTOTP(seed32, steps, "8",
                                "HmacSHA256") + "| SHA256 |");
                            System.out.print("|  " + fmtTime + "  |  " + utcTime +
                                "  | " + steps + " |");
                            System.out.println(TOTPAndroid.generateTOTP(seed64, steps, "8",
                                "HmacSHA512") + "| SHA512 |");

                            System.out.println(
                                "+---------------+-----------------------+" +
                                    "------------------+--------+--------+");
                        }
                    }catch (final Exception e){
                        System.out.println("Error : " + e);
                    }
                } else if (id == R.id.button_totp_pure) {
                    TOTP.main("");
                } else if (id == R.id.button_totp_kotlin) {
                    try {
                        System.out.println(
                            "+---------------+-----------------------+" +
                                "------------------+--------+--------+");
                        System.out.println(
                            "|  Time(sec)    |   Time (UTC format)   " +
                                "| Value of T(Hex)  |  TOTP  | Mode   |");
                        System.out.println(
                            "+---------------+-----------------------+" +
                                "------------------+--------+--------+");

                        for (int i=0; i<testTime.length; i++) {
                            long T = (testTime[i] - T0)/X;
                            steps = Long.toHexString(T).toUpperCase();
                            while (steps.length() < 16) steps = "0" + steps;
                            String fmtTime = String.format("%1$-11s", testTime[i]);
                            String utcTime = df.format(new Date(testTime[i]*1000));
                            System.out.print("|  " + fmtTime + "  |  " + utcTime +
                                "  | " + steps + " |");
                            //FIXME java is static method
                            System.out.println(TOTPKAndroid.generateTOTP(seed, steps, "8",
                                "HmacSHA1") + "| SHA1   |");
                            System.out.print("|  " + fmtTime + "  |  " + utcTime +
                                "  | " + steps + " |");
                            System.out.println(TOTPAndroid.generateTOTP(seed32, steps, "8",
                                "HmacSHA256") + "| SHA256 |");
                            System.out.print("|  " + fmtTime + "  |  " + utcTime +
                                "  | " + steps + " |");
                            //FIXME java is static method
                            System.out.println(TOTPKAndroid.generateTOTP(seed64, steps, "8",
                                "HmacSHA512") + "| SHA512 |");

                            System.out.println(
                                "+---------------+-----------------------+" +
                                    "------------------+--------+--------+");
                        }
                    }catch (final Exception e){
                        System.out.println("Error : " + e);
                    }

                }
            }
        };
        button_java.setOnClickListener(click);
        button_conversion.setOnClickListener(click);
        button_modify_kotlin.setOnClickListener(click);
        button_totp.setOnClickListener(click);
        button_totp_pure.setOnClickListener(click);
        button_totp_kotlin.setOnClickListener(click);
    }
}
