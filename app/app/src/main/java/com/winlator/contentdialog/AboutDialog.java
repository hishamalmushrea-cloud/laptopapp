package com.winlator.contentdialog;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.winlator.R;
import com.winlator.core.FileUtils;

/**
 * The app's About screen.
 *
 * <p>It is now the user-facing identity surface only - product name, version and icon.
 * The upstream links and third-party credits that previously filled it were removed on
 * the owner's request: they are presentation, not a legal duty.
 *
 * <p>What LGPL-2.1 actually requires is that the license text and copyright notices
 * accompany the distribution. That is satisfied by {@code assets/legal/LICENSE} and
 * {@code assets/legal/NOTICE}, which ship inside the APK and are shown, verbatim, from
 * the "Open-source Licenses" button. Attribution therefore stays with the software
 * without advertising it on the About screen.
 */
public class AboutDialog extends ContentDialog {
    public AboutDialog(Context context) {
        super(context, R.layout.about_dialog);

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            ((TextView)findViewById(R.id.TVAppVersion)).setText(context.getString(R.string.version)+" "+pInfo.versionName);
        }
        catch (PackageManager.NameNotFoundException e) {}

        Button licensesButton = findViewById(R.id.BTConfirm);
        licensesButton.setText(R.string.open_source_licenses);
        setOnConfirmCallback(() -> showLicenses(context));
    }

    /** Shows the bundled license and notice texts that accompany the distribution. */
    private void showLicenses(Context context) {
        String body;
        try {
            String license = FileUtils.readString(context, "legal/LICENSE");
            String notice = FileUtils.readString(context, "legal/NOTICE");
            body = (license != null ? license : "") + "\n\n" + (notice != null ? notice : "");
        }
        catch (Exception e) {
            body = "";
        }

        ContentDialog licenses = new ContentDialog(context);
        licenses.setTitle(R.string.open_source_licenses);

        TextView text = new TextView(context);
        text.setTextSize(11);
        text.setTextIsSelectable(true);
        text.setText(body);
        ScrollView scroll = new ScrollView(context);
        scroll.addView(text);

        FrameLayout frame = licenses.getContentView().findViewById(R.id.FrameLayout);
        frame.setVisibility(VISIBLE);
        frame.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        licenses.show();
    }
}
