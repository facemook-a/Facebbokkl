/* B5 integration: Keystore-protected temporary OpenVPN credentials. GPL-2.0. */
package de.blinkt.openvpn.core;
import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
class ProfileEncryption {
 static synchronized javax.crypto.SecretKey key() throws GeneralSecurityException, IOException {
  KeyStore s=KeyStore.getInstance("AndroidKeyStore");s.load(null);
  javax.crypto.SecretKey k=(javax.crypto.SecretKey)s.getKey("favor.vpn.profile",null);
  if(k!=null)return k;
  KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");
  g.init(new KeyGenParameterSpec.Builder("favor.vpn.profile",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();
 }
 static void initMasterCryptAlias(Context c) { }
 static boolean encryptionEnabled(){return true;}
 static InputStream getEncryptedVpInput(Context c,File f) throws GeneralSecurityException,IOException {
  byte[] all;
  try(FileInputStream in=new FileInputStream(f);ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
   byte[] buffer=new byte[4096];int n;
   while((n=in.read(buffer))!=-1){if(bytes.size()+n>1048576)throw new IOException("Encrypted profile exceeds limit");bytes.write(buffer,0,n);}
   all=bytes.toByteArray();
  }
  if(all.length<28)throw new IOException("Invalid encrypted profile");
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
  cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,java.util.Arrays.copyOfRange(all,0,12)));
  // Authenticate the complete GCM payload BEFORE Java profile deserialization.
  return new ByteArrayInputStream(cipher.doFinal(java.util.Arrays.copyOfRange(all,12,all.length)));
 }
 static OutputStream getEncryptedVpOutput(Context c,File f) throws GeneralSecurityException,IOException {
  final Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
  if(cipher.getIV().length!=12)throw new GeneralSecurityException("Unexpected GCM nonce length");
  return new ByteArrayOutputStream(){
   private boolean closed=false;
   @Override public void close() throws IOException {
    if(closed)return;closed=true;
    try(FileOutputStream out=new FileOutputStream(f)) {
     if(size()>1048500)throw new IOException("Profile exceeds encryption limit");
     out.write(cipher.getIV());out.write(cipher.doFinal(toByteArray()));
    }catch(GeneralSecurityException e){throw new IOException("Profile encryption failed",e);}finally{reset();super.close();}
   }
  };
 }
}
