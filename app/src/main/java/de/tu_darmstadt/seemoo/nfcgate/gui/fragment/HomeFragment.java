package de.tu_darmstadt.seemoo.nfcgate.gui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import de.tu_darmstadt.seemoo.nfcgate.R;
import de.tu_darmstadt.seemoo.nfcgate.gui.MainActivity;

/**
 * Landing screen: a grid of mode cards. Each card opens its mode fragment via
 * {@link MainActivity#openMode(int)}. Replaces the old navigation drawer.
 */
public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        bind(v, R.id.card_capture, R.drawable.ic_capture_black_24dp, R.string.menu_capture,
                "Log live NFC traffic", R.id.nav_capture);
        bind(v, R.id.card_relay, R.drawable.ic_relay_black_24dp, R.string.menu_relay,
                "Reader / tag over a server", R.id.nav_relay);
        bind(v, R.id.card_replay, R.drawable.ic_replay_black_24dp, R.string.menu_replay,
                "Re-send a capture", R.id.nav_replay);
        bind(v, R.id.card_clone, R.drawable.ic_clone_black_24dp, R.string.menu_clone,
                "Copy tag information", R.id.nav_clone);
        bind(v, R.id.card_status, R.drawable.ic_perm_device_information_black_24dp, R.string.menu_status,
                "Device & module status", R.id.nav_status);
        bind(v, R.id.card_about, R.drawable.ic_about_black_24dp, R.string.menu_about,
                "Licenses & info", R.id.nav_about);

        return v;
    }

    private void bind(View root, int cardId, @DrawableRes int icon, @StringRes int title,
                      String desc, int navId) {
        View card = root.findViewById(cardId);
        ((ImageView) card.findViewById(R.id.card_icon)).setImageResource(icon);
        ((TextView) card.findViewById(R.id.card_title)).setText(title);
        ((TextView) card.findViewById(R.id.card_desc)).setText(desc);
        card.setOnClickListener(x -> ((MainActivity) requireActivity()).openMode(navId));
    }
}
