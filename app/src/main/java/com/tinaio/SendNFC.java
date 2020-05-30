package com.tinaio;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class SendNFC extends AppCompatActivity {
    TextView textView;
    NfcAdapter nfcAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_nfc);
        VerifyingViewItems();
        nfcAdapter=NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)){
            Tag tag=intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            NdefMessage ndefMessage = CreateNdefMessage("HELLO");
            WriteNDEFMessage(tag,ndefMessage);
            Toast.makeText(SendNFC.this,"DONE!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatchSystem();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatchSystem();
    }

    private void VerifyingViewItems() {
        textView = findViewById(R.id.message);
    }
    public void enableForegroundDispatchSystem(){
        Intent intent=new Intent(this,SendNFC.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent=PendingIntent.getActivity(this,0,intent,0);
        IntentFilter[] intentFilters=new IntentFilter[] {};
        nfcAdapter.enableForegroundDispatch(this,pendingIntent,intentFilters,null);

    }
    public void disableForegroundDispatchSystem(){
        nfcAdapter.disableForegroundDispatch(this);
    }
    private void FormatTag(Tag tag, NdefMessage ndefMessage){
        NdefFormatable formatable=NdefFormatable.get(tag);
        if (formatable == null){
            Toast.makeText(SendNFC.this,"Tag is not formatable!!",Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            formatable.connect();
            formatable.format(ndefMessage);
            formatable.close();
        } catch (Exception e) {
            Log.i("Formatable",e.toString());
        }
    }
    private void WriteNDEFMessage(Tag tag,NdefMessage ndefMessage){
        if (tag ==null){
            Toast.makeText(SendNFC.this,"this var cant be null!!",Toast.LENGTH_SHORT).show();
            return;
        }
        Ndef ndef =Ndef.get(tag);
        if (ndef ==null){
            FormatTag(tag,ndefMessage);
        }
        else {
            try {
                ndef.connect();
                if (!ndef.isWritable()){
                    Toast.makeText(SendNFC.this,"its not writable :(",Toast.LENGTH_SHORT).show();
                    ndef.close();
                    return;
                }
                try {
                    ndef.writeNdefMessage(ndefMessage);
                    Toast.makeText(SendNFC.this,"WRITTEN!!",Toast.LENGTH_SHORT).show();
                    ndef.close();
                } catch (FormatException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private NdefRecord CreateTextRecord(String content){
        try {
            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");
            final byte[] text = content.getBytes();
            final int languagesize = language.length;
            final int textsize = text.length;
            final ByteArrayOutputStream payload =new ByteArrayOutputStream(languagesize + textsize + 1);
            payload.write(languagesize & 0x1f);
            payload.write(language,0,languagesize);
            payload.write(text,0,textsize);
            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT,new byte[0],payload.toByteArray());



        }
        catch (UnsupportedEncodingException e){
            Log.i("createTextRecord",e.getMessage());
        }
        return null;
    }
    private NdefMessage CreateNdefMessage(String content){
        NdefRecord record = CreateTextRecord(content);
        NdefMessage ndefMessage=new NdefMessage(new NdefRecord[] {record});
        return ndefMessage;
    }
}
