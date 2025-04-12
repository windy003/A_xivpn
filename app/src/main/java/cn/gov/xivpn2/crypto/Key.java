/*
 * Copyright © 2017-2023 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package cn.gov.xivpn2.crypto;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Represents a WireGuard public or private key. This class uses specialized constant-time base64
 * and hexadecimal codec implementations that resist side-channel attacks.
 * <p>
 * Instances of this class are immutable.
 */
@SuppressWarnings("MagicNumber")
public final class Key {
    private final byte[] key;

    /**
     * Constructs an object encapsulating the supplied key.
     *
     * @param key an array of bytes containing a binary key. Callers of this constructor are
     *            responsible for ensuring that the array is of the correct length.
     */
    private Key(final byte[] key) {
        // Defensively copy to ensure immutability.
        this.key = Arrays.copyOf(key, key.length);
    }

    /**
     * Encodes a single 4-character base64 chunk from 3 consecutive bytes in constant time.
     *
     * @param src        an array of at least 3 bytes
     * @param srcOffset  the offset of the beginning of the chunk in {@code src}
     * @param dest       an array of at least 4 characters
     * @param destOffset the offset of the beginning of the chunk in {@code dest}
     */
    private static void encodeBase64(final byte[] src, final int srcOffset,
                                     final char[] dest, final int destOffset) {
        final byte[] input = {
                (byte) ((src[srcOffset] >>> 2) & 63),
                (byte) ((src[srcOffset] << 4 | ((src[1 + srcOffset] & 0xff) >>> 4)) & 63),
                (byte) ((src[1 + srcOffset] << 2 | ((src[2 + srcOffset] & 0xff) >>> 6)) & 63),
                (byte) ((src[2 + srcOffset]) & 63),
        };
        for (int i = 0; i < 4; ++i) {
            dest[i + destOffset] = (char) (input[i] + 'A'
                    + (((25 - input[i]) >>> 8) & 6)
                    - (((51 - input[i]) >>> 8) & 75)
                    - (((61 - input[i]) >>> 8) & 15)
                    + (((62 - input[i]) >>> 8) & 3));
        }
    }

    /**
     * Generates a private key using the system's {@link SecureRandom} number generator.
     *
     * @return a well-formed random private key
     */
    public static Key generatePrivateKey() {
        final SecureRandom secureRandom = new SecureRandom();
        final byte[] privateKey = new byte[Format.BINARY.getLength()];
        secureRandom.nextBytes(privateKey);
        privateKey[0] &= 248;
        privateKey[31] &= 127;
        privateKey[31] |= 64;
        return new Key(privateKey);
    }

    /**
     * Generates a public key from an existing private key.
     *
     * @param privateKey a private key
     * @return a well-formed public key that corresponds to the supplied private key
     */
    public static Key generatePublicKey(final Key privateKey) {
        final byte[] publicKey = new byte[Format.BINARY.getLength()];
        Curve25519.eval(publicKey, 0, privateKey.getBytes(), null);
        return new Key(publicKey);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != getClass())
            return false;
        final Key other = (Key) obj;
        return MessageDigest.isEqual(key, other.key);
    }

    /**
     * Returns the key as an array of bytes.
     *
     * @return an array of bytes containing the raw binary key
     */
    public byte[] getBytes() {
        // Defensively copy to ensure immutability.
        return Arrays.copyOf(key, key.length);
    }

    @Override
    public int hashCode() {
        int ret = 0;
        for (int i = 0; i < key.length / 4; ++i)
            ret ^= (key[i * 4 + 0] >> 0) + (key[i * 4 + 1] >> 8) + (key[i * 4 + 2] >> 16) + (key[i * 4 + 3] >> 24);
        return ret;
    }

    /**
     * Encodes the key to base64.
     *
     * @return a string containing the encoded key
     */
    public String toBase64() {
        final char[] output = new char[Format.BASE64.length];
        int i;
        for (i = 0; i < key.length / 3; ++i)
            encodeBase64(key, i * 3, output, i * 4);
        final byte[] endSegment = {
                key[i * 3],
                key[i * 3 + 1],
                0,
        };
        encodeBase64(endSegment, 0, output, i * 4);
        output[Format.BASE64.length - 1] = '=';
        return new String(output);
    }

    /**
     * Encodes the key to hexadecimal ASCII characters.
     *
     * @return a string containing the encoded key
     */
    public String toHex() {
        final char[] output = new char[Format.HEX.length];
        for (int i = 0; i < key.length; ++i) {
            output[i * 2] = (char) (87 + (key[i] >> 4 & 0xf)
                    + ((((key[i] >> 4 & 0xf) - 10) >> 8) & ~38));
            output[i * 2 + 1] = (char) (87 + (key[i] & 0xf)
                    + ((((key[i] & 0xf) - 10) >> 8) & ~38));
        }
        return new String(output);
    }

    /**
     * The supported formats for encoding a WireGuard key.
     */
    public enum Format {
        BASE64(44),
        BINARY(32),
        HEX(64);

        private final int length;

        Format(final int length) {
            this.length = length;
        }

        public int getLength() {
            return length;
        }
    }

}
