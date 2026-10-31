package ge.venice.ghostcardlab;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
public class LabService extends HostApduService {
 private Protocol protocol; private Store store;
 @Override public void onCreate(){super.onCreate();store=new Store(this);}
 @Override public byte[] processCommandApdu(byte[] apdu,Bundle extras){
  synchronized(Store.LOCK){
   if(!store.card())return Protocol.hex("6985");
   if(protocol==null)protocol=new Protocol(store,store.key());
   byte[] response=protocol.process(apdu);
   store.log("HCE RX "+Protocol.toHex(apdu)+" → "+Protocol.toHex(response));
   return response;
  }
 }
 @Override public void onDeactivated(int reason){if(protocol!=null)protocol.disconnect();protocol=null;}
}
