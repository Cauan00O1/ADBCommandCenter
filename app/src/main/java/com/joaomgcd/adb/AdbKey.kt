package com.joaomgcd.adb

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.conscrypt.Conscrypt
import java.io.File
import java.math.BigInteger
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Principal
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.Arrays
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509ExtendedTrustManager

class AdbKey private constructor(
    private val privateKey: RSAPrivateKey,
    private val publicKey: RSAPublicKey,
    private val certificate: X509Certificate,
    private val pairingName: String
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        suspend fun create(context: Context, pairingName: String): AdbKey = withContext(Dispatchers.IO) {
            val keyFile = File(context.filesDir, "adbkey")
            var loaded = false
            var tempPrivate: RSAPrivateKey? = null
            var tempPublic: RSAPublicKey? = null

            if (keyFile.exists()) {
                try {
                    val keyBytes = keyFile.readBytes()
                    val kf = KeyFactory.getInstance("RSA")
                    tempPrivate = kf.generatePrivate(PKCS8EncodedKeySpec(keyBytes)) as RSAPrivateKey
                    (tempPrivate as? RSAPrivateCrtKey)?.let {
                        tempPublic = kf.generatePublic(RSAPublicKeySpec(it.modulus, it.publicExponent)) as RSAPublicKey
                        loaded = true
                    }
                } catch (e: Exception) {
                    Log.e("AdbKey", "Failed to load keys", e)
                }
            }

            val finalPrivateKey: RSAPrivateKey
            val finalPublicKey: RSAPublicKey

            val tempPublicTemp = tempPublic
            if (loaded && tempPrivate != null && tempPublicTemp != null) {
                finalPrivateKey = tempPrivate
                finalPublicKey = tempPublicTemp
            } else {
                val generator = KeyPairGenerator.getInstance("RSA")
                generator.initialize(2048)
                val keys = generator.generateKeyPair()
                finalPrivateKey = keys.private as RSAPrivateKey
                finalPublicKey = keys.public as RSAPublicKey
                try {
                    keyFile.writeBytes(finalPrivateKey.encoded)
                } catch (e: Exception) {
                    Log.e("AdbKey", "Failed to save keys", e)
                }
            }

            val certificate = generateCertificate(finalPrivateKey, finalPublicKey)
            AdbKey(finalPrivateKey, finalPublicKey, certificate, pairingName)
        }

        private fun generateCertificate(priv: PrivateKey, pub: PublicKey): X509Certificate {
            val signer = JcaContentSignerBuilder("SHA256withRSA").build(priv)
            val builder = X509v3CertificateBuilder(
                X500Name("CN=ADBPairing"),
                BigInteger.ONE,
                Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24),
                Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10),
                Locale.ROOT,
                X500Name("CN=ADBPairing"),
                SubjectPublicKeyInfo.getInstance(pub.encoded)
            )
            val holder = builder.build(signer)
            return JcaX509CertificateConverter().getCertificate(holder)
        }
    }

    private val adbPublicKeyDeferred = scope.async(start = CoroutineStart.LAZY) {
        val modulus = publicKey.modulus
        val r32 = BigInteger.ZERO.setBit(32)
        val n0inv = modulus.remainder(r32).modInverse(r32).negate()
        val r = BigInteger.ZERO.setBit(2048)
        val rr = r.modPow(BigInteger.valueOf(2), modulus)

        fun toArray(bi: BigInteger): IntArray {
            val arr = IntArray(64)
            var tmp = bi
            for (i in 0 until 64) {
                val res = tmp.divideAndRemainder(r32)
                tmp = res[0]
                arr[i] = res[1].toInt()
            }
            return arr
        }

        val buf = ByteBuffer.allocate(524 + 100).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(64)
        buf.putInt(n0inv.toInt())
        toArray(modulus).forEach { buf.putInt(it) }
        toArray(rr).forEach { buf.putInt(it) }
        buf.putInt(publicKey.publicExponent.toInt())
        val rawKey = Arrays.copyOf(buf.array(), 524)
        val b64 = Base64.encode(rawKey, Base64.NO_WRAP)
        val name = " $pairingName\u0000".toByteArray()
        val res = ByteArray(b64.size + name.size)
        System.arraycopy(b64, 0, res, 0, b64.size)
        System.arraycopy(name, 0, res, b64.size, name.size)
        res
    }

    private val sslContextDeferred = scope.async(start = CoroutineStart.LAZY) {
        val km = object : X509ExtendedKeyManager() {
            override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?) = "adb"
            override fun getCertificateChain(alias: String?) = arrayOf(certificate)
            override fun getPrivateKey(alias: String?) = privateKey
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?) = arrayOf("adb")
            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?) = null
            override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?) = null
        }
        val tm = object : X509ExtendedTrustManager() {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {}
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {}
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val provider = Conscrypt.newProvider()
        val context = SSLContext.getInstance("TLSv1.3", provider)
        context.init(arrayOf(km), arrayOf(tm), SecureRandom())
        context
    }

    suspend fun getAdbPublicKey(): ByteArray = adbPublicKeyDeferred.await()

    suspend fun getSslContext(): SSLContext = sslContextDeferred.await()

    suspend fun sign(data: ByteArray?): ByteArray = withContext(Dispatchers.IO) {
        val s = Signature.getInstance("SHA256withRSA")
        s.initSign(privateKey)
        if (data != null) {
            s.update(data)
        }
        s.sign()
    }
}