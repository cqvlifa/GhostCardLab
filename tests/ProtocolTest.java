package ge.venice.ghostcardlab;
import java.util.*;
public class ProtocolTest {
 static class Memory implements Protocol.Ledger{int b=1000,c=0;Set<String>s=new HashSet<>();public int balance(){return b;}public int counter(){return c;}public boolean seen(String i){return s.contains(i);}public boolean debit(String i,int a){if(seen(i)||b<a)return false;s.add(i);b-=a;c++;return true;}}
 static int tests;static void check(boolean b,String n){if(!b)throw new AssertionError(n);tests++;System.out.println("PASS "+n);}
 static byte[] nonce(Protocol p){p.process(Protocol.SELECT);return Arrays.copyOf(p.process(Protocol.CHALLENGE),16);}
 static boolean sw(byte[] r,String s){return Protocol.toHex(r).equals(s);}
 public static void main(String[] args){byte[] key=Protocol.random(32);Memory m=new Memory();Protocol p=new Protocol(m,key);
 check(sw(p.process(new byte[0]),"6700"),"short APDU");check(sw(p.process(Protocol.CHALLENGE),"6985"),"selection required");
 byte[] n=nonce(p),id=Protocol.random(16),req=Protocol.request(key,n,id,100);byte[] result=p.process(req);
 check(Protocol.verify(key,result,req)&&m.b==900&&m.c==1,"signed debit and receipt");
 check(sw(p.process(req),"6985")&&m.b==900,"same session replay rejected");
 check(sw(p.process(Protocol.request(key,nonce(p),id,100)),"6984"),"transaction ID replay rejected");
 byte[] bad=Protocol.request(key,nonce(p),Protocol.random(16),100);bad[72]^=1;check(sw(p.process(bad),"6982")&&m.b==900,"tampered MAC");
 check(sw(p.process(Protocol.request(key,nonce(p),Protocol.random(16),500)),"6A80"),"one unit limit");
 check(sw(p.process(Protocol.request(key,nonce(p),Protocol.random(16),-100)),"6A80"),"negative amount");
 byte[] old=nonce(p);p.disconnect();p.process(Protocol.SELECT);check(sw(p.process(Protocol.request(key,old,Protocol.random(16),100)),"6985"),"disconnect invalidates challenge");
 nonce(p);check(sw(p.process(Protocol.hex("80FF000000")),"6D00"),"unknown instruction");
 check(sw(p.process(Protocol.hex("00A4040001FF00")),"6A82"),"foreign AID");
 byte[] req2=Protocol.request(key,nonce(p),Protocol.random(16),100);byte[] r2=p.process(req2);r2[0]^=1;check(!Protocol.verify(key,r2,req2),"tampered receipt");
 m.b=0;check(sw(p.process(Protocol.request(key,nonce(p),Protocol.random(16),100)),"6A84"),"insufficient funds");
 byte[] malformed=Protocol.request(key,nonce(p),Protocol.random(16),100);malformed[4]=1;check(sw(p.process(malformed),"6700"),"invalid length");
 System.out.println(tests+" protocol checks passed; NFC hardware NOT tested.");
 }
}
