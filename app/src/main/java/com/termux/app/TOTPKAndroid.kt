package com.termux.app

import org.apache.commons.codec.binary.Base32
import java.lang.reflect.UndeclaredThrowableException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


object TOTPKAndroid {
    private val DIGITS_POWER // 0 1 2 3 4 5 6 7 8
        = intArrayOf(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)

    private const val SECRET_SIZE = 10

    private const val PASS_CODE_LENGTH = 6

    private const val INTERVAL = 30

    private const val WINDOW = 15

    private const val DEFAULT_CRYPTO = "HmacSHA1"

    private val rand = Random()

    private const val TOTP_URL = "TOTP_URI_FORMAT = \"otpauth://totp/tdx:2fa_demo?secret=%s&issuer=tdx&algorithm=SHA1&digits=6&period=30\""

    fun generateSecret(): String? { // Allocating the buffer
        val buffer = ByteArray(SECRET_SIZE)
        // Filling the buffer with random numbers.
        rand.nextBytes(buffer)
        // Getting the key and converting it to Base32
        val codec = Base32()
        val secretKey = Arrays.copyOf(buffer, SECRET_SIZE)
        val encodedKey = codec.encode(secretKey)
        return String(encodedKey)
    }

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun checkCode(secret: String?, code: Long): Boolean {
        val codec = Base32()
        val decodedKey = codec.decode(secret)
        // Window is used to check codes generated in the near past.
// You can use this value to tune how far you're willing to go.
        val window = WINDOW
        val currentInterval = getCurrentInterval()
        for (i in -window..window) {
            val hash = generateTOTP(decodedKey, currentInterval + i, PASS_CODE_LENGTH, DEFAULT_CRYPTO).toLong()
            if (hash == code) {
                return true
            }
        }
        // The validation code is invalid.
        return false
    }

    private fun getCurrentInterval(): Long {
        val currentTimeSeconds = System.currentTimeMillis() / 1000
        return currentTimeSeconds / INTERVAL
    }

    /**
     * This method uses the JCE to provide the crypto algorithm. HMAC computes a
     * Hashed Message Authentication Code with the crypto hash algorithm as a
     * parameter.
     *
     * @param crypto
     * : the crypto algorithm (HmacSHA1, HmacSHA256, HmacSHA512)
     * @param keyBytes
     * : the bytes to use for the HMAC key
     * @param text
     * : the message or text to be authenticated
     */
    private fun hmacSha(crypto: String, keyBytes: ByteArray, text: ByteArray): ByteArray {
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

    /**
     * This method generates a net.tdx.TOTP value for the given set of parameters.
     *
     * @param key
     * : the shared secret
     * @param time
     * : a value that reflects a time
     * @param digits
     * : number of digits to return
     * @param crypto
     * : the crypto function to use
     *
     * @return: digits
     */
    fun generateTOTP(key: ByteArray, time: Long, digits: Int, crypto: String): Int {
        val msg = ByteBuffer.allocate(8).putLong(time).array()
        val hash = hmacSha(crypto, key, msg)
        // put selected bytes into result int
        //FIXME Here java auto-convert Kotlin can not trans  "and"
//        val offset: Int = hash[hash.size - 1] and 0xf
//        val binary: Int = (hash[offset] and 0x7f shl 24
//            or (hash[offset + 1] and 0xff shl 16)
//            or (hash[offset + 2] and 0xff shl 8)
//            or (hash[offset + 3] and 0xff))
//            return binary % DIGITS_POWER[digits]
        return 0
    }

    /**
     * This method generates a net.tdx.TOTP value for the given set of parameters.
     *
     * @param key
     * : the shared secret
     * @param time
     * : a value that reflects a time
     * @param returnDigits
     * : number of digits to return
     * @param crypto
     * : the crypto function to use
     *
     * @return: digits
     */
    fun generateTOTP(key: String, time: String, returnDigits: String?, crypto: String): String? {
        var time = time
        val codeDigits = Integer.decode(returnDigits).toInt()
        var result: String? = null
        while (time.length < 16) time = "0$time"
        // Get the HEX in a Byte[]
        val msg = hexStr2Bytes(time)
        val k = hexStr2Bytes(key)
        val hash = hmacSha(crypto, k, msg)
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

    fun generateTOTP(key: String?): String? {
        val codec = Base32()
        val decodedKey = codec.decode(key)
        val totpNumber = generateTOTP(decodedKey, getCurrentInterval(), PASS_CODE_LENGTH, DEFAULT_CRYPTO)
        val format = "%0" + PASS_CODE_LENGTH + "d"
        return String.format(format, totpNumber)
    }

    fun generateTOTP(key: String, time: String, returnDigits: String?): String? {
        return generateTOTP(key, time, returnDigits, DEFAULT_CRYPTO)
    }

    fun generateTOTP256(key: String, time: String, returnDigits: String?): String? {
        return generateTOTP(key, time, returnDigits, "HmacSHA256")
    }

    fun generateTOTP512(key: String, time: String, returnDigits: String?): String? {
        return generateTOTP(key, time, returnDigits, "HmacSHA512")
    }

    private fun hexStr2Bytes(hex: String): ByteArray {
        val bArray = BigInteger("10$hex", 16).toByteArray()
        val ret = ByteArray(bArray.size - 1)
        for (i in ret.indices) ret[i] = bArray[i + 1]
        return ret
    }
}
