package ge.venice.ghostcardlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.cardemulation.CardEmulation;
import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Redesigned educational UI. Private teaching AID only; all amounts simulated. */
public class MainActivity extends Activity implements NfcAdapter.ReaderCallback {

    static final int BG      = Color.parseColor("#0E1116");
    static final int SURFACE = Color.parseColor("#171C24");
    static final int SURFACE2= Color.parseColor("#1F2630");
    static final int STROKE  = Color.parseColor("#2A313D");
    static final int TXT     = Color.parseColor("#F3F6FA");
    static final int TXT2    = Color.parseColor("#95A1B2");
    static final int MINT    = Color.parseColor("#2BD9A8");
    static final int GREEN   = Color.parseColor("#34D399");
    static final int BLUE    = Color.parseColor("#48A7FF");
    static final int CORAL   = Color.parseColor("#FF6B5E");

    private Store store; private NfcAdapter nfc;
    private boolean reader=false; private volatile boolean armed=false;
    private final AtomicBoolean busy=new AtomicBoolean();

    private TextView nfcVal, modeVal, balVal, logView, primaryLbl, primaryHint;
    private ImageView nfcIco, modeIco, primaryIco;
    private LinearLayout cardTog, readerTog, primaryBtn;
    private ImageView cardIco, readerIco; private TextView cardLbl, readerLbl;
    private EditText key;

    private float d;
    private int dp(float v){ return Math.round(v*d); }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        d = getResources().getDisplayMetrics().density;
        store = new Store(this);
        nfc = NfcAdapter.getDefaultAdapter(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(28),dp(20),dp(32));
        scroll.addView(root);
        setContentView(scroll);

        // ---- Header ----
        LinearLayout header = row(); header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout logoChip = new LinearLayout(this);
        logoChip.setBackground(round(SURFACE2,16,STROKE,1));
        logoChip.setPadding(dp(11),dp(11),dp(11),dp(11));
        logoChip.addView(icon("ic_nfc",MINT,28));
        header.addView(logoChip);
        LinearLayout htxt = new LinearLayout(this); htxt.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams hl=new LinearLayout.LayoutParams(0,-2,1); hl.leftMargin=dp(14); htxt.setLayoutParams(hl);
        htxt.addView(text("GhostCard Lab",23,TXT,true));
        htxt.addView(text("NFC სასწავლო ლაბორატორია",13,TXT2,false));
        header.addView(htxt);
        root.addView(header);

        // ---- Simulation chip ----
        TextView chip = text("●  სიმულაცია · რეალური თანხა არ გადაირიცხება",11.5f,GREEN,false);
        chip.setBackground(round(Color.parseColor("#12241D"),20,Color.parseColor("#1E3A2E"),1));
        chip.setPadding(dp(13),dp(7),dp(13),dp(7));
        gap(chip,16); root.addView(chip);

        // ---- Status / balance card ----
        LinearLayout status = card(); gap(status,16);
        LinearLayout balCap = row(); balCap.setGravity(Gravity.CENTER_VERTICAL);
        balCap.addView(icon("ic_wallet",TXT2,15));
        TextView bc = text("ლოკალური სატესტო ბალანსი",12,TXT2,false);
        ((LinearLayout.LayoutParams) mlp(bc)).leftMargin=dp(7); balCap.addView(bc);
        status.addView(balCap);
        balVal = text("10.00 ₾",34,TXT,true); gap(balVal,2); status.addView(balVal);
        status.addView(divider());
        LinearLayout grid = row();
        nfcIco=icon("ic_nfc",BLUE,18); nfcVal=text("—",14.5f,TXT,true);
        grid.addView(infoCell("NFC",nfcIco,nfcVal));
        modeIco=icon("ic_card",TXT2,18); modeVal=text("—",14.5f,TXT,true);
        grid.addView(infoCell("რეჟიმი",modeIco,modeVal));
        gap(grid,14); status.addView(grid);
        root.addView(status);

        // ---- Mode toggles ----
        root.addView(sectionLabel("რეჟიმის არჩევა"));
        LinearLayout modes = row(); gap(modes,10);
        cardTog = buildToggle(0,"ბარათი","ოპერაციას იღებს","ic_card",GREEN);
        readerTog = buildToggle(1,"წამკითხველი","ოპერაციას აგზავნის","ic_wifi",BLUE);
        modes.addView(cardTog); modes.addView(readerTog);
        root.addView(modes);

        // ---- Primary action ----
        primaryBtn = new LinearLayout(this); primaryBtn.setOrientation(LinearLayout.HORIZONTAL);
        primaryBtn.setGravity(Gravity.CENTER); primaryBtn.setPadding(dp(16),dp(16),dp(16),dp(16));
        primaryBtn.setElevation(dp(5));
        primaryIco=icon("ic_bolt",BG,20);
        primaryLbl=text("1 ₾ ოპერაციის მომზადება",16,BG,true);
        ((LinearLayout.LayoutParams) mlp(primaryLbl)).leftMargin=dp(9);
        primaryBtn.addView(primaryIco); primaryBtn.addView(primaryLbl);
        LinearLayout.LayoutParams pbl=new LinearLayout.LayoutParams(-1,-2); pbl.topMargin=dp(16); primaryBtn.setLayoutParams(pbl);
        primaryBtn.setOnClickListener(v->{
            if(!reader){ toast("ჯერ აირჩიე წამკითხველის რეჟიმი"); return; }
            armed=true; refresh();
        });
        root.addView(primaryBtn);
        primaryHint = text("",12.5f,TXT2,false); primaryHint.setGravity(Gravity.CENTER); gap(primaryHint,8);
        root.addView(primaryHint);

        // ---- Shared key ----
        root.addView(sectionLabel("საერთო გასაღები"));
        LinearLayout keyCard = card(); gap(keyCard,10);
        keyCard.addView(text("64-სიმბოლოიანი HEX — ორივე ტელეფონზე ერთი და იგივე",12,TXT2,false));
        key = new EditText(this);
        key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        key.setTextColor(TXT); key.setHintTextColor(TXT2);
        key.setTextSize(13); key.setTypeface(Typeface.MONOSPACE);
        key.setText(store.key().length==32?Protocol.toHex(store.key()):"");
        key.setHint("64 HEX სიმბოლო");
        key.setBackground(round(Color.parseColor("#121720"),12,STROKE,1));
        key.setPadding(dp(12),dp(12),dp(12),dp(12));
        LinearLayout.LayoutParams kl=new LinearLayout.LayoutParams(-1,-2); kl.topMargin=dp(10); key.setLayoutParams(kl);
        keyCard.addView(key);
        LinearLayout saveBtn = pill("ic_key","გასაღების შენახვა",MINT);
        gap(saveBtn,10);
        saveBtn.setOnClickListener(v->{
            try{
                if(busy.get()||store.card()){ toast("გასაღების შესაცვლელად აირჩიე წამკითხველის რეჟიმი"); return; }
                store.key(key.getText().toString().trim()); armed=false; toast("შენახულია");
            }catch(Exception e){ toast("საჭიროა ზუსტად 64 HEX სიმბოლო"); }
        });
        keyCard.addView(saveBtn);
        root.addView(keyCard);

        // ---- Actions ----
        root.addView(sectionLabel("მოქმედებები"));
        LinearLayout actions = card(); gap(actions,10); actions.setPadding(dp(6),dp(6),dp(6),dp(6));
        actions.addView(actionRow("ic_settings","NFC პარამეტრები",BLUE,
                ()->startActivity(new Intent(Settings.ACTION_NFC_SETTINGS))));
        actions.addView(actionRow("ic_restore","სატესტო ბალანსის აღდგენა",GREEN,()->
                new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert)
                    .setMessage("აღდგეს სასწავლო ბალანსი 10 ₾-მდე?")
                    .setPositiveButton("აღდგენა",(di,w)->{ if(busy.get()||store.card()){toast("ჯერ გადადი წამკითხველის რეჟიმში");return;} store.reset(); refresh(); })
                    .setNegativeButton("გაუქმება",null).show()));
        actions.addView(actionRow("ic_refresh","ჟურნალის განახლება",TXT2,this::refresh));
        actions.addView(actionRow("ic_share","ჟურნალის ექსპორტი",TXT2,()->{
            Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT,"GhostCard Lab — SIMULATION ONLY\n"+store.logs());
            startActivity(Intent.createChooser(i,"ტესტის ანგარიში"));
        }));
        actions.addView(actionRow("ic_delete","ჟურნალის გასუფთავება",CORAL,()->{ store.clear(); refresh(); }));
        root.addView(actions);

        // ---- Log ----
        root.addView(sectionLabel("ჟურნალი"));
        LinearLayout logCard = card(); gap(logCard,10);
        logView = new TextView(this);
        logView.setTextColor(TXT2); logView.setTextSize(12); logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextIsSelectable(true); logView.setMinHeight(dp(120));
        logCard.addView(logView);
        root.addView(logCard);

        refresh();
    }

    // ---------- UI builders ----------
    private LinearLayout buildToggle(int idx,String titleS,String subS,String ic,int accent){
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL); col.setPadding(dp(14),dp(18),dp(14),dp(16));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); if(idx==1) lp.leftMargin=dp(12); col.setLayoutParams(lp);
        ImageView iv=icon(ic,TXT2,26);
        TextView tl=text(titleS,16,TXT,true); gap(tl,8); tl.setGravity(Gravity.CENTER);
        TextView sl=text(subS,11.5f,TXT2,false); gap(sl,1); sl.setGravity(Gravity.CENTER);
        col.addView(iv); col.addView(tl); col.addView(sl);
        if(idx==0){ cardIco=iv; cardLbl=tl; } else { readerIco=iv; readerLbl=tl; }
        col.setOnClickListener(v->{
            armed=false;
            if(idx==0){ reader=false; store.card(true); } else { reader=true; store.card(false); }
            configure(); refresh();
        });
        return col;
    }

    private LinearLayout infoCell(String cap,ImageView iv,TextView val){
        LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); col.setLayoutParams(lp);
        col.addView(text(cap,11.5f,TXT2,false));
        LinearLayout r=row(); r.setGravity(Gravity.CENTER_VERTICAL); gap(r,4);
        r.addView(iv); ((LinearLayout.LayoutParams) mlp(val)).leftMargin=dp(7); r.addView(val);
        col.addView(r); return col;
    }

    private LinearLayout actionRow(String ic,String label,int accent,Runnable act){
        LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(12),dp(13),dp(12),dp(13));
        r.setBackground(round(0x00000000,12,0x00000000,0));
        LinearLayout chip=new LinearLayout(this); chip.setBackground(round(withAlpha(accent,0x22),10,0x00000000,0));
        chip.setPadding(dp(8),dp(8),dp(8),dp(8)); chip.addView(icon(ic,accent,18));
        r.addView(chip);
        TextView t=text(label,15,TXT,false);
        LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(0,-2,1); tl.leftMargin=dp(13); t.setLayoutParams(tl);
        r.addView(t);
        r.setOnClickListener(v->act.run());
        return r;
    }

    private LinearLayout pill(String ic,String label,int accent){
        LinearLayout p=new LinearLayout(this); p.setOrientation(LinearLayout.HORIZONTAL);
        p.setGravity(Gravity.CENTER); p.setPadding(dp(14),dp(13),dp(14),dp(13));
        p.setBackground(round(withAlpha(accent,0x22),12,withAlpha(accent,0x66),1));
        p.addView(icon(ic,accent,18));
        TextView t=text(label,14.5f,accent,true); ((LinearLayout.LayoutParams) mlp(t)).leftMargin=dp(8); p.addView(t);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); p.setLayoutParams(lp);
        return p;
    }

    private LinearLayout card(){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(round(SURFACE,18,STROKE,1)); c.setPadding(dp(18),dp(18),dp(18),dp(18));
        c.setElevation(dp(3));
        c.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));
        return c;
    }
    private LinearLayout row(){ LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutParams(new LinearLayout.LayoutParams(-1,-2)); return r; }
    private View divider(){ View v=new View(this); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));
        lp.topMargin=dp(14); lp.bottomMargin=dp(2); v.setLayoutParams(lp); v.setBackgroundColor(STROKE); return v; }
    private TextView sectionLabel(String s){ TextView t=text(s.toUpperCase(Locale.ROOT),12,TXT2,true);
        t.setLetterSpacing(0.08f); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2); lp.topMargin=dp(22); lp.bottomMargin=dp(2); lp.leftMargin=dp(4); t.setLayoutParams(lp); return t; }

    private TextView text(String s,float sp,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if(bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLayoutParams(new LinearLayout.LayoutParams(-2,-2)); return t;
    }
    private ImageView icon(String name,int color,int sizeDp){
        ImageView iv=new ImageView(this);
        iv.setImageResource(getResources().getIdentifier(name,"drawable",getPackageName()));
        iv.setColorFilter(color);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp),dp(sizeDp))); return iv;
    }
    private GradientDrawable round(int fill,float rDp,int stroke,int strokeDp){
        GradientDrawable g=new GradientDrawable(); g.setColor(fill); g.setCornerRadius(dp(rDp));
        if(strokeDp>0) g.setStroke(dp(strokeDp),stroke); return g;
    }
    private ViewGroup.LayoutParams mlp(View v){ if(v.getLayoutParams()==null) v.setLayoutParams(new LinearLayout.LayoutParams(-2,-2)); return v.getLayoutParams(); }
    private void gap(View v,int topDp){ ViewGroup.LayoutParams p=mlp(v); if(p instanceof LinearLayout.LayoutParams) ((LinearLayout.LayoutParams)p).topMargin=dp(topDp); }
    private int withAlpha(int color,int a){ return (color&0x00FFFFFF)|(a<<24); }
    private void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    // ---------- State ----------
    private void refresh(){
        if(balVal==null) return;
        boolean on = nfc!=null && nfc.isEnabled();
        nfcVal.setText(nfc==null?"არ არის":on?"ჩართული":"გამორთული");
        int nc = nfc==null?CORAL:on?GREEN:CORAL; nfcVal.setTextColor(nc); nfcIco.setColorFilter(nc);
        String mode = reader?"წამკითხველი":store.card()?"ბარათი":"აირჩიე";
        modeVal.setText(mode);
        modeIco.setImageResource(getResources().getIdentifier(reader?"ic_wifi":"ic_card","drawable",getPackageName()));
        int mc = reader?BLUE:store.card()?GREEN:TXT2; modeVal.setTextColor(mc); modeIco.setColorFilter(mc);
        balVal.setText(String.format(Locale.US,"%.2f ₾",store.balance()/100.0));

        styleToggle(cardTog,cardIco,cardLbl,GREEN,!reader&&store.card());
        styleToggle(readerTog,readerIco,readerLbl,BLUE,reader);

        if(!reader){
            primaryBtn.setBackground(round(SURFACE2,14,STROKE,1)); primaryBtn.setElevation(0);
            primaryLbl.setText("ჯერ აირჩიე წამკითხველი"); primaryLbl.setTextColor(TXT2); primaryIco.setColorFilter(TXT2);
            primaryHint.setText("");
        } else if(armed){
            primaryBtn.setBackground(round(CORAL,14,0x00000000,0)); primaryBtn.setElevation(dp(6));
            primaryLbl.setText("მზადაა — მიადე ბარათის ტელეფონი"); primaryLbl.setTextColor(Color.WHITE); primaryIco.setColorFilter(Color.WHITE);
            primaryHint.setText("ანტენები ერთმანეთს მიადე და გააჩერე");
        } else {
            primaryBtn.setBackground(round(MINT,14,0x00000000,0)); primaryBtn.setElevation(dp(5));
            primaryLbl.setText("1 ₾ ოპერაციის მომზადება"); primaryLbl.setTextColor(BG); primaryIco.setColorFilter(BG);
            primaryHint.setText("");
        }
        logView.setText(store.logs());
    }

    private void styleToggle(LinearLayout col,ImageView iv,TextView lbl,int accent,boolean sel){
        if(sel){
            col.setBackground(round(withAlpha(accent,0x22),16,accent,2)); col.setElevation(dp(2));
            iv.setColorFilter(accent); lbl.setTextColor(TXT);
        } else {
            col.setBackground(round(SURFACE2,16,STROKE,1)); col.setElevation(0);
            iv.setColorFilter(TXT2); lbl.setTextColor(TXT2);
        }
    }

    // ---------- NFC (unchanged logic) ----------
    @Override protected void onResume(){ super.onResume(); configure(); refresh(); }
    @Override protected void onPause(){ if(nfc!=null){ nfc.disableReaderMode(this); CardEmulation.getInstance(nfc).unsetPreferredService(this);} armed=false; super.onPause(); }
    private void configure(){
        if(nfc==null) return;
        nfc.disableReaderMode(this);
        CardEmulation ce=CardEmulation.getInstance(nfc); ce.unsetPreferredService(this);
        if(reader) nfc.enableReaderMode(this,this,NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,null);
        else if(store.card()) ce.setPreferredService(this,new ComponentName(this,LabService.class));
    }

    @Override public void onTagDiscovered(Tag tag){
        if(!armed||!busy.compareAndSet(false,true)) return; armed=false;
        boolean sent=false;
        try{
            IsoDep iso=IsoDep.get(tag); if(iso==null) throw new Exception("ISO-DEP არ არის");
            try{
                iso.connect(); iso.setTimeout(5000);
                byte[] selected=exchange(iso,Protocol.SELECT); if(!Protocol.ok(selected)) throw new Exception("სასწავლო AID უარყოფილია");
                byte[] challenge=exchange(iso,Protocol.CHALLENGE); if(challenge.length!=18||!Protocol.ok(challenge)) throw new Exception("გამოწვევა უარყოფილია");
                byte[] k=store.key(); byte[] request=Protocol.request(k,Arrays.copyOf(challenge,16),Protocol.random(16),100); sent=true;
                byte[] result=exchange(iso,request);
                if(!Protocol.ok(result)){ store.log("უარყოფილია: "+Protocol.toHex(result)); }
                else if(!Protocol.verify(k,result,request)){ store.log("შედეგი დაუდასტურებელია: ხელმოწერის შეცდომა"); }
                else{ int balance=ByteBuffer.wrap(result,36,4).getInt(); store.log("წარმატება • 1.00 ₾ სიმულაცია • ბარათის ნაშთი "+String.format(Locale.US,"%.2f",balance/100.0)); }
            } finally { iso.close(); }
        }
        catch(Exception e){ store.log((sent?"შედეგი უცნობია — შეამოწმე ბარათის ჟურნალი. ":"ჩამოჭრა არ გაგზავნილა. ")+e.getClass().getSimpleName()+": "+e.getMessage()); }
        finally{ busy.set(false); runOnUiThread(this::refresh); }
    }
    private byte[] exchange(IsoDep iso,byte[] a)throws Exception{ store.log("TX "+Protocol.toHex(a)); byte[] r=iso.transceive(a); store.log("RX "+Protocol.toHex(r)); return r; }
}
