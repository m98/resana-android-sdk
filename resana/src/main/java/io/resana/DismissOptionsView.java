package io.resana;

import android.app.Dialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

public class DismissOptionsView extends Dialog {

    private RelativeLayout container;
    private ListView listView;
    private List<DismissOption> dismissOptions;

    private Delegate delegate;

    interface Delegate {
        void itemSelected(String key, String reason);
    }


    public DismissOptionsView(Context context, List<DismissOption> dismissOptions, Delegate delegate) {
        super(context);
        this.dismissOptions = dismissOptions;
        this.delegate = delegate;
        init();
        setContentView(container, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setTitle("عدم نمایش تبلیغات");
    }

    private void init() {
//        if (dismissOptions == null)
//            return;
        dismissOptions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            dismissOptions.add(new DismissOption("1", "دلیل" + i));
        }
        container = new RelativeLayout(getContext());
        listView = new ListView(getContext());
        container.addView(listView);
        final List<String> dismissOptionsString = new ArrayList<String>();
        for (int i = 0; i < dismissOptions.size(); i++) {
            dismissOptionsString.add(dismissOptions.get(i).getReason());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, dismissOptionsString);
        listView.setAdapter(adapter);
    }

    public void setDismissOptions(List<DismissOption> dismissOptions) {
        this.dismissOptions = dismissOptions;
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }
}
