/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.iota.security;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RSA key-pair generator and RSA utils for encryption / decryption
 *
 * @author Theo Giovanna - Initial Contribution
 */
public class RSAUtils {

    private final Logger logger = LoggerFactory.getLogger(RSAUtils.class);

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public RSAUtils() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair pair = keyGen.generateKeyPair();
        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();
    }

    /**
     * Encrypt data given the public key
     *
     * @param data
     * @param modulus
     * @param exponent
     * @return
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    public String encrypt(String data, BigInteger modulus, BigInteger exponent) throws BadPaddingException,
            IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKeyX509(modulus, exponent));
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
    }

    /**
     * Decrypt data given the private key
     *
     * @param data
     * @param modulus
     * @param exponent
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public String decrypt(String data, BigInteger modulus, BigInteger exponent) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, getPrivateKeyPKCS8(modulus, exponent));
        return new String(cipher.doFinal(Base64.getDecoder().decode(data.getBytes())));
    }

    /**
     * Returns the publickey object from the modulus and exponent
     *
     * @param modulus
     * @param exponent
     * @return
     */
    public PublicKey getPublicKeyX509(BigInteger modulus, BigInteger exponent) {
        PublicKey publicKey = null;
        try {
            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            publicKey = factory.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.debug("Exception happened: {}", e.toString());
        }
        return publicKey;
    }

    /**
     * Returns the privatekey object from the modulus and exponent
     * 
     * @param modulus
     * @param exponent
     * @return
     */
    public PrivateKey getPrivateKeyPKCS8(BigInteger modulus, BigInteger exponent) {
        PrivateKey privateKey = null;
        RSAPrivateKeySpec spec = new RSAPrivateKeySpec(modulus, exponent);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            privateKey = factory.generatePrivate(spec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            logger.debug("Exception happened: {}", e.toString());
        }
        return privateKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

}
