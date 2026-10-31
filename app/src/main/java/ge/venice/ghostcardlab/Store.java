package ge.venice.ghostcardlab;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
final class Store implements Protocol.Ledger {
 static final Object LOCK=new Object(); private final SharedPreferences p;
 Store(Context c){p=c.getSharedPreferences("lab",0);synchronized(LOCK){if(!p.contains("key"))p.edit().putString("key",Protocol.toHex(Protocol.random(32))).commit();}}
 byte[] key(){return Protocol.hex(p.getString("key",""));}
 boolean card(){return p.getBoolean("card",false);}
 void card(boolean v){p.edit().putBoolean("card",v).commit();}
 void key(String s){if(Protocol.hex(s).length!=32)throw new IllegalArgumentException();p.edit().putString("key",s).commit();}
 public int balance(){return p.getInt("balance",1000);}
 public int counter(){return p.getInt("counter",0);}
 public boolean seen(String id){return p.getStringSet("ids",new HashSet<>()).contains(id);}
 public boolean debit(String id,int amount){synchronized(LOCK){Set<String> ids=new HashSet<>(p.getStringSet("ids",new HashSet<>()));if(ids.contains(id)||balance()<amount||ids.size()>=10000)return false;ids.add(id);return p.edit().putInt("balance",balance()-amount).putInt("counter",counter()+1).putStringSet("ids",ids).commit();}}
 void reset(){synchronized(LOCK){p.edit().putInt("balance",1000).putInt("counter",0).remove("ids").commit();log("სასწავლო ბალანსი აღდგა: 10.00 ₾");}}
 void log(String s){synchronized(LOCK){String old=p.getString("log","");String line=new SimpleDateFormat("HH:mm:ss",Locale.US).format(new Date())+"  "+s+"\n";String next=line+old;if(next.length()>24000)next=next.substring(0,24000);p.edit().putString("log",next).commit();}}
 String logs(){return p.getString("log","");}
 void clear(){p.edit().remove("log").commit();}
}
