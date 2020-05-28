package com.tinaio;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.tinaio.parser.NdefMessageParser;
import com.tinaio.record.ParsedNdefRecord;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        ViewVerifying();
        if (IsNFCAvailable()) {
            IsNFCEnable();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        ResolveIntent(intent);
    }

    private void ResolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
        ) {
            Parcelable[] RawMessage = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] ndefMessages;
            if (RawMessage != null) {
                ndefMessages = new NdefMessage[RawMessage.length];
                for (int i = 0; i < RawMessage.length; ++i) {
                    ndefMessages[i] = (NdefMessage) RawMessage[i];
                }
            } else {
                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                for (byte bt : id)
                    Log.i("TEST : ID", String.valueOf(bt));
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Log.i("TEST : TAG", String.valueOf(tag));
                byte[] payload = dumpTagData(tag).getBytes();
                for (byte bt2 : payload)
                    Log.i("TEST : PAYLOAD", String.valueOf(bt2));
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);
                NdefMessage message = new NdefMessage(record);
                Log.i("TEST : MESSAGE", String.valueOf(message));
                ndefMessages = new NdefMessage[]{message};
                DisplayMessage(ndefMessages);
            }
        }
    }

    private void DisplayMessage(NdefMessage[] ndefMessages) {
        if (ndefMessages == null || ndefMessages.length == 0) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        List<ParsedNdefRecord> records = NdefMessageParser.parse(ndefMessages[0]);
        for (int i = 0; i < records.size(); i++) {
            ParsedNdefRecord record = records.get(i);
            String str = record.str();
            stringBuilder.append(str).append("\n");
        }
        textView.setText(stringBuilder.toString());
    }

    private void ViewVerifying() {
        textView = findViewById(R.id.result);
    }

    private boolean IsNFCAvailable() {
        if (nfcAdapter == null) {
            textView.setText("NO NFC!");
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 4000);
            return false;
        } else return true;
    }

    private boolean IsNFCEnable() {
        if (!nfcAdapter.isEnabled()) {
            ShowWirelessSetting();
            return false;
        } else {

            return true;
        }
    }

    private void ShowWirelessSetting() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("NFC Permission")
                .setMessage("NFC is not enabled\nDo you want to go to setting menu?")
                .setPositiveButton("go", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "CANCELED", Toast.LENGTH_SHORT).show();
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                finish();
                            }
                        }, 2000);
                    }
                })
                .create()
                .show();

    }

    @Override
    protected void onResume() {
        IsNFCAvailable();
        if (IsNFCEnable()) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null)
            nfcAdapter.disableForegroundDispatch(this);
    }

    private String dumpTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        sb.append("ID (hex): ").append(toHex(id)).append('\n');
        sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n');
        sb.append("ID (dec): ").append(toDec(id)).append('\n');
        sb.append("ID (reversed dec): ").append(toReversedDec(id)).append('\n');

        String prefix = "android.nfc.tech.";
        sb.append("Technologies: ");
        for (String tech : tag.getTechList()) {
            sb.append(tech.substring(prefix.length()));
            sb.append(", ");
        }

        sb.delete(sb.length() - 2, sb.length());

        for (String tech : tag.getTechList()) {
            if (tech.equals(MifareClassic.class.getName())) {
                sb.append('\n');
                String type = "Unknown";

                try {
                    MifareClassic mifareTag = MifareClassic.get(tag);

                    switch (mifareTag.getType()) {
                        case MifareClassic.TYPE_CLASSIC:
                            type = "Classic";
                            break;
                        case MifareClassic.TYPE_PLUS:
                            type = "Plus";
                            break;
                        case MifareClassic.TYPE_PRO:
                            type = "Pro";
                            break;
                    }
                    sb.append("Mifare Classic type: ");
                    sb.append(type);
                    sb.append('\n');

                    sb.append("Mifare size: ");
                    sb.append(mifareTag.getSize() + " bytes");
                    sb.append('\n');

                    sb.append("Mifare sectors: ");
                    sb.append(mifareTag.getSectorCount());
                    sb.append('\n');

                    sb.append("Mifare blocks: ");
                    sb.append(mifareTag.getBlockCount());
                } catch (Exception e) {
                    sb.append("Mifare classic error: " + e.getMessage());
                }
            }

            if (tech.equals(MifareUltralight.class.getName())) {
                sb.append('\n');
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                String type = "Unknown";
                switch (mifareUlTag.getType()) {
                    case MifareUltralight.TYPE_ULTRALIGHT:
                        type = "Ultralight";
                        break;
                    case MifareUltralight.TYPE_ULTRALIGHT_C:
                        type = "Ultralight C";
                        break;
                }
                sb.append("Mifare Ultralight type: ");
                sb.append(type);
            }
        }

        return sb.toString();
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
            if (i > 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private String toReversedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0) {
                sb.append(" ");
            }
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }

//    private long toDec(byte[] bytes) {
//        long result = 0;
//        long factor = 1;
//        for (int i = 0; i < bytes.length; ++i) {
//            long value = bytes[i] & 0xffl;
//            result += value * factor;
//            factor *= 256l;
//        }
//        return result;
//    }

    private String toDec(byte[] bytes) {
        String result="" ;
        for (byte b:bytes) {
            result = result+(b & 0xff);
        }
        return result;
    }

    private long toReversedDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = bytes.length - 1; i >= 0; --i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }
}