package es.bsc.mobile.monitor;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

public class MonitorActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RelativeLayout ll = new RelativeLayout(this);
        LayoutParams lp;

        TextView title = new TextView(this);
        title.setId(001001001);
        title.setText("COMPSs Mobile Service Management");
        title.setTextSize(20);
        lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        ll.addView(title, lp);

        Button resources = new Button(this);
        resources.setId(001002001);
        resources.setText("Resource Management");
        resources.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Obrir menu de recursos");
            }
        });
        lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.BELOW, title.getId());
        ll.addView(resources, lp);

        Button profile = new Button(this);
        profile.setId(001002002);
        profile.setText("Profile Information");
        profile.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Obrir profile dels cores");
            }
        });
        lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.BELOW, resources.getId());
        ll.addView(profile, lp);

        Button progress = new Button(this);
        progress.setId(001002003);
        progress.setText("Execution Progress");
        progress.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("Obrir el progrés de la execució");
            }
        });
        lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.BELOW, profile.getId());
        ll.addView(progress, lp);

        lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        setContentView(ll, lp);
    }

}
