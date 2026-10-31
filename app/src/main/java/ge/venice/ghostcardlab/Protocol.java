package ge.venice.ghostcardlab;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Private teaching protocol. Not EMV; units are simulated tetri. */
public final class Protocol {
 public static final byte[] AID=hex("F047484F535401");
 public static final byte[] SELECT=concat(hex("00A4040007"),AID,hex("00"));
 public static final byte[] CHALLENGE=hex("8010000000");
 public interface Ledger { int balance(); int counter(); boolean seen(String id); boolean debit(String id,int amount); }
 private final Ledger ledger; private final byte[] key; private byte[] challenge; private boolean selected;
 public Protocol(Ledger ledger,byte[] key){ if(key.length!=32)throw new IllegalArgumentException("32 byte key required"); this.ledger=ledger;this.key=key.clone(); }
 public void disconnect(){selected=false;challenge=null;}
 public synchronized byte[] process(byte[] a){
  if(a==null||a.length<4)return hex("6700");
  if(Arrays.equals(a,SELECT)){selected=true;challenge=null;return concat(new byte[]{1},hex("9000"));}
  if((a[1]&255)==0xA4){disconnect();return hex("6A82");}
  if(!selected)return hex("6985");
  if(Arrays.equals(a,CHALLENGE)){challenge=random(16);return concat(challenge,hex("9000"));}
  if((a[0]&255)!=0x80)return hex("6E00");
  if((a[1]&255)!=0x20)return hex("6D00");
  if(a.length!=73||(a[4]&255)!=68)return hex("6700");
  if(a[2]!=0||a[3]!=0)return hex("6A86");
  byte[] body=Arrays.copyOfRange(a,5,41), sig=Arrays.copyOfRange(a,41,73);
  if(!MessageDigest.isEqual(mac(key,body),sig))return hex("6982");
  if(challenge==null||!MessageDigest.isEqual(challenge,Arrays.copyOfRange(body,0,16)))return hex("6985");
  challenge=null; // A valid signed request consumes the nonce even when declined.
  String id=toHex(Arrays.copyOfRange(body,16,32)); int amount=ByteBuffer.wrap(body,32,4).getInt();
  if(amount!=100)return hex("6A80");
  if(ledger.seen(id))return hex("6984");
  if(ledger.balance()<amount)return hex("6A84");
  if(!ledger.debit(id,amount))return hex("6581");
  byte[] receipt=concat(body,ByteBuffer.allocate(8).putInt(ledger.balance()).putInt(ledger.counter()).array());
  return concat(receipt,mac(key,receipt),hex("9000"));
 }
 public static byte[] request(byte[] key,byte[] nonce,byte[] id,int amount){
  if(nonce.length!=16||id.length!=16)throw new IllegalArgumentException();
  byte[] body=concat(nonce,id,ByteBuffer.allocate(4).putInt(amount).array());
  return concat(hex("8020000044"),body,mac(key,body));
 }
 public static boolean verify(byte[] key,byte[] response,byte[] request){
  if(response.length!=78||!ok(response))return false;
  byte[] receipt=Arrays.copyOfRange(response,0,44);
  return MessageDigest.isEqual(Arrays.copyOfRange(request,5,41),Arrays.copyOfRange(receipt,0,36))
   && MessageDigest.isEqual(mac(key,receipt),Arrays.copyOfRange(response,44,76));
 }
 public static boolean ok(byte[] a){return a!=null&&a.length>=2&&a[a.length-2]==(byte)0x90&&a[a.length-1]==0;}
 public static byte[] random(int n){byte[] a=new byte[n];new SecureRandom().nextBytes(a);return a;}
 public static byte[] mac(byte[] key,byte[] body){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(key,"HmacSHA256"));return m.doFinal(body);}catch(Exception e){throw new IllegalStateException(e);}}
 public static byte[] concat(byte[]... arrays){int n=0;for(byte[] a:arrays)n+=a.length;byte[] out=new byte[n];int p=0;for(byte[] a:arrays){System.arraycopy(a,0,out,p,a.length);p+=a.length;}return out;}
 public static byte[] hex(String s){if(s.length()%2!=0||!s.matches("[0-9a-fA-F]*"))throw new IllegalArgumentException("HEX");byte[] a=new byte[s.length()/2];for(int i=0;i<a.length;i++)a[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return a;}
 public static String toHex(byte[] a){StringBuilder s=new StringBuilder();for(byte b:a)s.append(String.format("%02X",b&255));return s.toString();}
}
