package ge.venice.ghostcardlab;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.cardemulation.CardEmulation;
import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;
import android.widget.*;
import android.view.WindowManager;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity implements NfcAdapter.ReaderCallback {
 private Store store;private NfcAdapter nfc;private boolean reader=false;private volatile boolean armed=false;
 private TextView status,log; private EditText key;private final AtomicBoolean busy=new AtomicBoolean();
 @Override public void onCreate(Bundle b){super.onCreate(b);store=new Store(this);nfc=NfcAdapter.getDefaultAdapter(this);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,40,24,32);scroll.addView(root);setContentView(scroll);
  TextView title=new TextView(this);title.setText("GhostCard Lab\nNFC სასწავლო ლაბორატორია");title.setTextSize(25);root.addView(title);
  TextView info=new TextView(this);info.setText("სატესტო ერთეულები • რეალური თანხა არ გადაირიცხება\nორივე ტელეფონზე შეიყვანე ერთი და იგივე 64-სიმბოლოიანი HEX გასაღები. გამოიყენე მხოლოდ ამ პროექტის წამკითხველთან.");root.addView(info);
  status=new TextView(this);status.setTextSize(18);root.addView(status);
  button(root,"ბარათის რეჟიმი",()->{armed=false;reader=false;store.card(true);configure();refresh();});
  button(root,"წამკითხველის რეჟიმი",()->{armed=false;reader=true;store.card(false);configure();refresh();});
  button(root,"მოვამზადო 1 ₾-ის სატესტო ოპერაცია",()->{if(!reader){toast("ჯერ აირჩიე წამკითხველის რეჟიმი");return;}armed=true;refresh();});
  key=new EditText(this);key.setSingleLine(true);key.setText(store.key().length==32?Protocol.toHex(store.key()):"");key.setHint("64 HEX სიმბოლო");root.addView(key);
  button(root,"გასაღების შენახვა",()->{try{if(busy.get()||store.card()){toast("გასაღების შესაცვლელად აირჩიე წამკითხველის რეჟიმი და დაასრულე მიმდინარე ოპერაცია");return;}store.key(key.getText().toString().trim());armed=false;toast("შენახულია");}catch(Exception e){toast("საჭიროა ზუსტად 64 HEX სიმბოლო");}});
  button(root,"NFC პარამეტრები",()->startActivity(new Intent(Settings.ACTION_NFC_SETTINGS)));
  button(root,"სატესტო ბალანსის აღდგენა",()->new AlertDialog.Builder(this).setMessage("აღდგეს სასწავლო ბალანსი 10 ₾-მდე? ჟურნალში ეს აღინიშნება.").setPositiveButton("აღდგენა",(d,w)->{if(busy.get()||store.card()){toast("ჯერ გადადი წამკითხველის რეჟიმში");return;}store.reset();refresh();}).setNegativeButton("გაუქმება",null).show());
  button(root,"ჟურნალის განახლება",this::refresh);
  button(root,"ჟურნალის ექსპორტი",()->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,"GhostCard Lab — SIMULATION ONLY\n"+store.logs());startActivity(Intent.createChooser(i,"ტესტის ანგარიში"));});
  button(root,"ჟურნალის გასუფთავება",()->{store.clear();refresh();});
  log=new TextView(this);log.setTextSize(12);log.setTextIsSelectable(true);root.addView(log);refresh();
 }
 private void button(LinearLayout r,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setOnClickListener(v->action.run());r.addView(b);}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 private void refresh(){if(status==null)return;status.setText("NFC: "+(nfc==null?"არ არის":nfc.isEnabled()?"ჩართულია":"გამორთულია")+"\nრეჟიმი: "+(reader?"წამკითხველი":store.card()?"ბარათი":"აირჩიე რეჟიმი")+"\nლოკალური სატესტო ბალანსი: "+String.format(Locale.US,"%.2f",store.balance()/100.0)+" ₾\n"+(armed?"მზადაა 1 ₾ — მიადე ბარათის ტელეფონი":"ოპერაცია მოუმზადებელია"));if(log!=null)log.setText(store.logs());}
 @Override protected void onResume(){super.onResume();configure();refresh();}
 @Override protected void onPause(){if(nfc!=null){nfc.disableReaderMode(this);CardEmulation.getInstance(nfc).unsetPreferredService(this);}armed=false;super.onPause();}
 private void configure(){if(nfc==null)return;nfc.disableReaderMode(this);CardEmulation ce=CardEmulation.getInstance(nfc);ce.unsetPreferredService(this);if(reader)nfc.enableReaderMode(this,this,NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,null);else if(store.card())ce.setPreferredService(this,new ComponentName(this,LabService.class));}
 @Override public void onTagDiscovered(Tag tag){
  if(!armed||!busy.compareAndSet(false,true))return;armed=false;
  boolean sent=false;
  try{IsoDep iso=IsoDep.get(tag);if(iso==null)throw new Exception("ISO-DEP არ არის");try{iso.connect();iso.setTimeout(5000);
   byte[] selected=exchange(iso,Protocol.SELECT);if(!Protocol.ok(selected))throw new Exception("სასწავლო AID უარყოფილია");
   byte[] challenge=exchange(iso,Protocol.CHALLENGE);if(challenge.length!=18||!Protocol.ok(challenge))throw new Exception("გამოწვევა უარყოფილია");
   byte[] k=store.key();byte[] request=Protocol.request(k,Arrays.copyOf(challenge,16),Protocol.random(16),100);sent=true;
   byte[] result=exchange(iso,request);
   if(!Protocol.ok(result)){store.log("უარყოფილია: "+Protocol.toHex(result));}
   else if(!Protocol.verify(k,result,request)){store.log("შედეგი დაუდასტურებელია: ხელმოწერის შეცდომა; გადაამოწმე ბარათის ჟურნალი");}
   else{int balance=ByteBuffer.wrap(result,36,4).getInt();store.log("წარმატება • 1.00 ₾ სიმულაცია • ბარათის ნაშთი "+String.format(Locale.US,"%.2f",balance/100.0));}
  }finally{iso.close();}}
  catch(Exception e){store.log((sent?"შედეგი უცნობია — შეამოწმე ბარათის ჟურნალი; ავტომატურად არ გაიმეორო. ":"ჩამოჭრა არ გაგზავნილა. ")+e.getClass().getSimpleName()+": "+e.getMessage());}
  finally{busy.set(false);runOnUiThread(this::refresh);}
 }
 private byte[] exchange(IsoDep iso,byte[] a)throws Exception{store.log("TX "+Protocol.toHex(a));byte[] r=iso.transceive(a);store.log("RX "+Protocol.toHex(r));return r;}
}
