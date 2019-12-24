package com.termux.app

import java.lang.reflect.UndeclaredThrowableException
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


object TOTPK {
    private fun hmac_sha(crypto: String, keyBytes: ByteArray, text: ByteArray): ByteArray {
        return try {
            val hmac: Mac
            hmac = Mac.getInstance(crypto)
            val macKey = SecretKeySpec(keyBytes, "RAW")
            hmac.init(macKey)
            hmac.doFinal(text)
        } catch (gse: GeneralSecurityException) {
            throw UndeclaredThrowableException(gse)
        }
    }

    private fun hexStr2Bytes(hex: String): ByteArray {
        val bArray = BigInteger("10$hex", 16).toByteArray()
        val ret = ByteArray(bArray.size - 1)
        for (i in ret.indices) ret[i] = bArray[i + 1]
        return ret
    }

    private val DIGITS_POWER // 0 1  2   3    4     5      6       7        8
        = intArrayOf(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)

    @JvmOverloads
    fun generateTOTP(key: String, time: String, returnDigits: String?, crypto: String = "HmacSHA1"): String? {
        var time = time
        val codeDigits = Integer.decode(returnDigits).toInt()
        var result: String? = null
        while (time.length < 16) time = "0$time"
        // Get the HEX in a Byte[]
        val msg = hexStr2Bytes(time)
        val k = hexStr2Bytes(key)
        val hash = hmac_sha(crypto, k, msg)
        // put selected bytes into result int
        //FIXME Here java auto-convert Kotlin can not trans  "and"
//        val offset: Int = hash[hash.size - 1] and 0xf
//        val binary: Int = hash[offset] and 0x7f shl 24 or
//            (hash[offset + 1] and 0xff shl 16) or
//            (hash[offset + 2] and 0xff shl 8) or
//            (hash[offset + 3] and 0xff)
//        val otp = binary % DIGITS_POWER[codeDigits]
//        result = Integer.toString(otp)
        while (result!!.length < codeDigits) {
            result = "0$result"
        }
        return result
    }

    @JvmStatic
    fun main(args: String) { // Seed for HMAC-SHA1 - 20 bytes
        val seed = "3132333435363738393031323334353637383930"
        // Seed for HMAC-SHA256 - 32 bytes
        val seed32 = "3132333435363738393031323334353637383930" +
            "313233343536373839303132"
        // Seed for HMAC-SHA512 - 64 bytes
        val seed64 = "3132333435363738393031323334353637383930" +
            "3132333435363738393031323334353637383930" +
            "3132333435363738393031323334353637383930" +
            "31323334"
        val T0: Long = 0
        val X: Long = 30
        val testTime = longArrayOf(59L, 1111111109L, 1111111111L,
            1234567890L, 2000000000L, 20000000000L)
        var steps = "0"
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        df.timeZone = TimeZone.getTimeZone("UTC")
        try {
            println(
                "+---------------+-----------------------+" +
                    "------------------+--------+--------+")
            println(
                "|  Time(sec)    |   Time (UTC format)   " +
                    "| Value of T(Hex)  |  TOTP  | Mode   |")
            println(
                "+---------------+-----------------------+" +
                    "------------------+--------+--------+")
            for (i in testTime.indices) {
                val T = (testTime[i] - T0) / X
                steps = java.lang.Long.toHexString(T).toUpperCase()
                while (steps.length < 16) steps = "0$steps"
                val fmtTime = String.format("%1$-11s", testTime[i])
                val utcTime = df.format(Date(testTime[i] * 1000))
                print("|  " + fmtTime + "  |  " + utcTime +
                    "  | " + steps + " |")
                println(generateTOTP(seed, steps, "8",
                    "HmacSHA1") + "| SHA1   |")
                print("|  " + fmtTime + "  |  " + utcTime +
                    "  | " + steps + " |")
                println(generateTOTP(seed32, steps, "8",
                    "HmacSHA256") + "| SHA256 |")
                print("|  " + fmtTime + "  |  " + utcTime +
                    "  | " + steps + " |")
                println(generateTOTP(seed64, steps, "8",
                    "HmacSHA512") + "| SHA512 |")
                println(
                    "+---------------+-----------------------+" +
                        "------------------+--------+--------+")
            }
        } catch (e: Exception) {
            println("Error : $e")
        }
    }
}
