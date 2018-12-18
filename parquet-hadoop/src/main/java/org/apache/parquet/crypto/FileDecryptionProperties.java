/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.parquet.crypto;

import java.util.Map;

import org.apache.parquet.hadoop.metadata.ColumnPath;

public class FileDecryptionProperties {

  private static final boolean CHECK_SIGNATURE = true;

  private final byte[] footerKey;
  private final DecryptionKeyRetriever keyRetriever;
  private final byte[] aadPrefix;
  private final Map<ColumnPath, ColumnDecryptionProperties> columnPropertyMap;
  private final boolean checkPlaintextFooterIntegrity;
  
  private FileDecryptionProperties(byte[] footerKey, DecryptionKeyRetriever keyRetriever,
      boolean checkPlaintextFooterIntegrity,  byte[] aadPrefix, 
      Map<ColumnPath, ColumnDecryptionProperties> columnPropertyMap) {
    
    if ((null == footerKey) && (null == keyRetriever) && (null == columnPropertyMap)) {
      throw new IllegalArgumentException("No decryption properties are specified");
    }
    if ((null != footerKey) && 
        !(footerKey.length == 16 || footerKey.length == 24 || footerKey.length == 32)) {
      throw new IllegalArgumentException("Wrong footer key length " + footerKey.length);
    }
    if ((null == footerKey) && checkPlaintextFooterIntegrity && (null == keyRetriever)) {
      throw new IllegalArgumentException("Can't check footer integrity with null footer key and null key retriever");
    }

    
    this.footerKey = footerKey;
    this.checkPlaintextFooterIntegrity = checkPlaintextFooterIntegrity;
    this.keyRetriever = keyRetriever;
    this.aadPrefix = aadPrefix;
    this.columnPropertyMap = columnPropertyMap;
  }

  public static Builder builder() {
    return new Builder();
  }
  
  public static class Builder {
    private byte[] footerKey;
    private DecryptionKeyRetriever keyRetriever;
    private byte[] aadPrefixBytes;
    private Map<ColumnPath, ColumnDecryptionProperties> columnPropertyMap;
    private boolean checkPlaintextFooterIntegrity;
    
    private Builder() {
      this.checkPlaintextFooterIntegrity = CHECK_SIGNATURE;
    }

    /**
     * Set an explicit footer decryption key. If applied on a file that contains footer 
     * encryption key metadata - 
     * the metadata will be ignored, the footer will be decrypted with this key.
     * If explicit key is not set, decryption key will be fetched from key retriever.
     * @param footerDecryptionKey Key length must be either 16, 24 or 32 bytes. 
     */
    public Builder withFooterKey(byte[] footerKey) {
      if (null == footerKey) {
        return this;
      }
      if (null != this.footerKey) {
        throw new IllegalArgumentException("Footer key already set");
      }
      this.footerKey = footerKey;
      return this;
    }

    /**
     * Set the column encryption properties.
     * @param columnPropertyMap
     * @return
     */
    public Builder withColumnProperties(Map<ColumnPath, ColumnDecryptionProperties> columnPropertyMap) {
      if (null == columnPropertyMap) {
        return this;
      }
      if (null != this.columnPropertyMap) {
        throw new IllegalArgumentException("Column properties already set");
      }
      this.columnPropertyMap = columnPropertyMap;
      return this;
    }
    
    /**
     * Set a key retriever callback. Its also possible to
     * set explicit footer or column keys on this property object. Upon file decryption, 
     * availability of explicit keys is checked before invocation of the retriever callback.
     * If an explicit key is available for a footer or a column, its key metadata will
     * be ignored. 
     * @param keyRetriever
     */
    public Builder withKeyRetriever(DecryptionKeyRetriever keyRetriever) {
      if (null == keyRetriever) {
        return this;
      }
      if (null != this.keyRetriever) {
        throw new IllegalArgumentException("Key retriever already set");
      }
      this.keyRetriever = keyRetriever;
      return this;
    }
    
    /**
     * Specify whether integrity of plaintext footer must be verified.
     * If yes (default), an exception will be thrown in the following runtime situations:
     * - footer signing key is not available (not passed, or not found in key retriever)
     * - footer content and signature don't match
     * @param checkFooterIntegrity
     * @return
     */
    public Builder withoutFooterSignatureVerification() {
      this.checkPlaintextFooterIntegrity = false;
      return this;
    }
    
    
    /**
     * Explicitly supply the AAD prefix.
     * A must when a prefix is used for file encryption, but not stored in file.
     * If AAD prefix is stored in file, the explicitly supplied value will be ignored.
     * @param aad
     */
    public Builder withAADPrefix(byte[] aadPrefixBytes) {
      if (null == aadPrefixBytes) {
        return this;
      }
      if (null != this.aadPrefixBytes) {
        throw new IllegalArgumentException("AAD Prefix already set");
      }
      this.aadPrefixBytes = aadPrefixBytes;
      return this;
    }
    
    public FileDecryptionProperties build() {
      return new FileDecryptionProperties(footerKey, keyRetriever, 
          checkPlaintextFooterIntegrity, aadPrefixBytes, columnPropertyMap);
    }
  }
  
  public byte[] getFooterKey() {
    return footerKey;
  }
  
  public byte[] getColumnKey(ColumnPath path) {
    if (null == columnPropertyMap) return null;
    ColumnDecryptionProperties columnDecryptionProperties = columnPropertyMap.get(path);
    if (null == columnDecryptionProperties) return null;
    return columnDecryptionProperties.getKeyBytes();
  }

  public DecryptionKeyRetriever getKeyRetriever() {
    return keyRetriever;
  }

  public byte[] getAADPrefix() {
    return aadPrefix;
  }
  
  public boolean checkFooterIntegrity() {
    return checkPlaintextFooterIntegrity;
  }
}
