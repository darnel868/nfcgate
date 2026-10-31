package de.tu_darmstadt.seemoo.nfcgate.gui;

import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.util.List;

import de.tu_darmstadt.seemoo.nfcgate.R;
import de.tu_darmstadt.seemoo.nfcgate.db.SessionLog;
import de.tu_darmstadt.seemoo.nfcgate.db.pcapng.ISO14443Stream;
import de.tu_darmstadt.seemoo.nfcgate.db.worker.LogInserter;
import de.tu_darmstadt.seemoo.nfcgate.gui.fragment.AboutFragment;
import de.tu_darmstadt.seemoo.nfcgate.gui.fragment.CaptureFragment;
import de.tu_darmstadt.seemoo.nfcgate.gui.fragment.CloneFragment;
import de.tu_darmstadt.seemoo.nfcgate.gui.fragment.HomeFragment;
import de.tu_darmstadt.seemoo.nfcgate.gui.fragment.StatusFragment;
import de.tu_darmstadt.seemoo.nfcgate.gui.log.LoggingFragment;
import de.tu_darmstadt.seemoo.nfcgate.gui.fragment.RelayFragment;
import de.tu_darmstadt.seemoo.nfcgate.gui.fragment.ReplayFragment;
import de.tu_darmstadt.seemoo.nfcgate.gui.fragment.SettingsFragment;
import de.tu_darmstadt.seemoo.nfcgate.network.UserTrustManager;
import de.tu_darmstadt.seemoo.nfcgate.nfc.NfcManager;
import de.tu_darmstadt.seemoo.nfcgate.util.NfcComm;
import de.tu_darmstadt.seemoo.nfcgate.xposed.InjectionBroadcastWrapper;

public class MainActivity extends AppCompatActivity {
    // UI
    Toolbar mToolbar;
    BottomNavigationView mBottomNav;

    // NFC
    NfcManager mNfc;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // toolbar setup
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // show an up-arrow whenever a mode is opened on top of a primary destination
        final FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(() -> {
            final ActionBar ab = getSupportActionBar();
            if (ab == null)
                return;
            boolean up = fm.getBackStackEntryCount() > 0;
            ab.setDisplayHomeAsUpEnabled(up);
            if (!up)
                ab.setSubtitle(null);
        });

        // bottom navigation: primary destinations
        mBottomNav = findViewById(R.id.bottom_nav);
        mBottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.bottom_home)
                showPrimary(new HomeFragment(), R.string.app_name);
            else if (id == R.id.bottom_captures)
                showPrimary(new LoggingFragment(), R.string.nav_logging);
            else if (id == R.id.bottom_settings)
                showPrimary(new SettingsFragment(), R.string.menu_settings);
            else
                return false;
            return true;
        });

        // start on the home screen
        if (savedInstanceState == null)
            mBottomNav.setSelectedItemId(R.id.bottom_home);

        // NFC setup
        mNfc = new NfcManager(this);
        if (!mNfc.hasNfc() || !mNfc.isNFCEnabled())
            showWarning(getString(R.string.error_NFCCAP));

        // TLS setup
        UserTrustManager.init(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // pass initial intent to current mode in case it carries a tag
        if (getIntent() != null)
            onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // tech discovered is triggered by XML, tag discovered by foreground dispatch
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()))
            mNfc.onTagDiscovered(intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        else if (Intent.ACTION_SEND.equals(intent.getAction()))
            importPcap(intent.getParcelableExtra(Intent.EXTRA_STREAM));
        else if (Intent.ACTION_VIEW.equals(intent.getAction()))
            importPcap(intent.getData());
        else if (InjectionBroadcastWrapper.NFCGATE_BROADCAST.equals(intent.getAction()))
            mNfc.getDaemon().onResponse(intent);
        else
            super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfc.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfc.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Returns a Fragment for every mode action (ids from menu/navbar_items)
     */
    private Fragment getFragmentByAction(int id) {
        if (R.id.nav_clone == id) {
            return new CloneFragment();
        } else if (R.id.nav_relay == id) {
            return new RelayFragment();
        } else if (R.id.nav_replay == id) {
            return new ReplayFragment();
        } else if (R.id.nav_capture == id) {
            return new CaptureFragment();
        } else if (R.id.nav_settings == id) {
            return new SettingsFragment();
        } else if (R.id.nav_status == id) {
            return new StatusFragment();
        } else if (R.id.nav_about == id) {
            return new AboutFragment();
        } else if (R.id.nav_logging == id) {
            return new LoggingFragment();
        }

        throw new IllegalArgumentException("Position out of range");
    }

    @StringRes
    private int titleForAction(int id) {
        if (R.id.nav_clone == id) return R.string.menu_clone;
        if (R.id.nav_relay == id) return R.string.menu_relay;
        if (R.id.nav_replay == id) return R.string.menu_replay;
        if (R.id.nav_capture == id) return R.string.menu_capture;
        if (R.id.nav_settings == id) return R.string.menu_settings;
        if (R.id.nav_status == id) return R.string.menu_status;
        if (R.id.nav_about == id) return R.string.menu_about;
        if (R.id.nav_logging == id) return R.string.nav_logging;
        return R.string.app_name;
    }

    /**
     * Opens a mode on top of the current primary destination (with a back arrow).
     * Called by the home screen cards.
     */
    public void openMode(int navId) {
        Fragment fragment = getFragmentByAction(navId);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_content, fragment)
                .addToBackStack(null)
                .commit();
        applyChrome(titleForAction(navId));
    }

    /**
     * Shows a primary destination, clearing any mode opened on top of it.
     */
    private void showPrimary(Fragment fragment, @StringRes int title) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction()
                .replace(R.id.main_content, fragment)
                .commit();
        applyChrome(title);
    }

    private void applyChrome(@StringRes int title) {
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(title);
            ab.setSubtitle(null);
        }
        // hide status banner by default; fragments show it when relevant
        findViewById(R.id.banner).setVisibility(View.GONE);
        // avoid carrying over actions from a previous fragment
        supportInvalidateOptionsMenu();
    }

    private void importPcap(Uri uri) {
        try {
            LogInserter inserter = new LogInserter(this, SessionLog.SessionType.RELAY, null);

            for (NfcComm e : new ISO14443Stream().readAll(getContentResolver().openInputStream(uri)))
                inserter.log(e);
            Toast.makeText(this, getString(R.string.pcap_success), Toast.LENGTH_SHORT).show();
        }
        catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.pcap_error), Toast.LENGTH_SHORT).show();
        }
    }

    public void importCapture(List<byte[]> capture) {
        LogInserter inserter = new LogInserter(this, SessionLog.SessionType.CAPTURE, null);

        for (byte[] b : capture)
            inserter.log(new NfcComm(b));

        Toast.makeText(this, getString(R.string.pcap_log, capture.size()), Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a warning dialog with the specified message
     */
    public void showWarning(String warning) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.status_warning))
                .setMessage(warning)
                .setNegativeButton(R.string.button_ok, null)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .show();
    }
    /**
     * Display an informational Toast with the specified message
     */
    public void showInfo(String info) {
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
    }

    public NfcManager getNfc() {
        return mNfc;
    }
}
