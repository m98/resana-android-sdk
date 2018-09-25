package io.resana.player;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;


import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.resana.Resana;
import io.resana.player.FileManager.Dir;
import io.resana.player.FileManager.FileManagerHelper;
import io.resana.player.FileManager.Media;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    @Bind(R.id.expandableListView)
    ExpandableListView list;
    @Bind(R.id.no_permission)
    View noPermission;

    public static final String VIDEO_PATH_KEY = "VIDEO_PATH_KEY";

    AdapterExpandbleList adapterList;

    Dir currentDir;

    public static final int ACCESS_STORAGE_PERMISSION_REQUEST_CODE = 0;
    private Resana resana;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        resana = Resana.create(this, null, Resana.LOG_LEVEL_VERBOSE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        adapterList = new AdapterExpandbleList(this);
        list.setAdapter(adapterList);
        list.setDivider(null);
        list.setGroupIndicator(null);

        list.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Log.d(TAG, "onChildClick: ");
                if (adapterList.getGroup(groupPosition) == AdapterExpandbleList.Group.Folders)
                    changeDirectory(((Dir) adapterList.getChild(groupPosition, childPosition)));
                else
                    playVideo(((Media) adapterList.getChild(groupPosition, childPosition)));
                return true;
            }
        });

        if (isAccessingSrorageGranted()) {
            noPermission.setVisibility(View.GONE);
            new LoadFiles().execute();
        } else {
            noPermission.setVisibility(View.VISIBLE);
            requestStrorageAccessPermission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resana.release();
    }

    private void changeDirectory(Dir directory) {
        updateList(directory);
    }

    private void playVideo(Media media) {
//        Intent intent = new Intent(this, ActivityVideoPlayer.class);
//        intent.putExtra(VIDEO_PATH_KEY, media.getPath());
//        startActivity(intent);
        Intent intent = new Intent(this, SimpleVideoPlayer.class);
        intent.putExtra(VIDEO_PATH_KEY, media.getPath());
        startActivity(intent);
    }

    private boolean isAccessingSrorageGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onBackPressed() {
        if (!currentDir.isRoot()) {
            updateList(currentDir.parent);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ACCESS_STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    noPermission.setVisibility(View.GONE);
                    new LoadFiles().execute();
                } else {
                    noPermission.setVisibility(View.VISIBLE);
                }
                return;
        }
    }

    public void expandAllGroups(ExpandableListView list) {
        for (int i = 0; i < list.getExpandableListAdapter().getGroupCount(); i++) {
            list.expandGroup(i);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_view) {
            showChooseViewDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showChooseViewDialog() {
        FileManagerHelper.viewType = (FileManagerHelper.viewType + 1) % 3;
        if (FileManagerHelper.viewType == 0) {
            FileManagerHelper.onlyMediaContainingSubDirs = true;
            FileManagerHelper.collapseUnnecessaryIntermediateSubDirs = true;
            new LoadFiles().execute();
        } else if (FileManagerHelper.viewType == 1) {
            FileManagerHelper.onlyMediaContainingSubDirs = false;
            FileManagerHelper.collapseUnnecessaryIntermediateSubDirs = false;
            new LoadFiles().execute();
        } else if (FileManagerHelper.viewType == 2) {
            while (!currentDir.isRoot())
                currentDir = currentDir.parent;
            currentDir.medias = currentDir.getAllMediaRecursively();
            currentDir.subDirs = new ArrayList<>();
            updateList(currentDir);
        }
    }

    public void onViewClick(View v) {
        switch (v.getId()) {
            case R.id.grant_permissions:
                requestStrorageAccessPermission();
                break;
        }
    }

    private void requestStrorageAccessPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                ACCESS_STORAGE_PERMISSION_REQUEST_CODE);
    }

    private void updateList(Dir directory) {
        currentDir = directory;
        setTitle(directory.isRoot() ? "Videos" : directory.name);
        adapterList.setData(directory);
        adapterList.notifyDataSetChanged();
        expandAllGroups(list);
    }

    private class LoadFiles extends AsyncTask<String, Void, Dir> {
        private ProgressDialog loading;

        @Override
        protected void onPreExecute() {
            loading = ProgressDialog.show(MainActivity.this, "Loading Files Data", "It wont take so long ...");
        }

        @Override
        protected Dir doInBackground(String... params) {
            Dir root = FileManagerHelper.getRawFileSystem(MainActivity.this);
            if (root == null) {
                root = new Dir(null, Environment.getExternalStorageDirectory().getPath());
                Log.d(TAG, root.getPath());
                root.print("");
                FileManagerHelper.saveFileSystem(MainActivity.this, root);
            }
            root.onlyMediaContainingSubDirs = FileManagerHelper.onlyMediaContainingSubDirs;
            root.collapseUnnecessaryIntermediateSubDirs = FileManagerHelper.collapseUnnecessaryIntermediateSubDirs;
            root.applyConfiguration();
            return root;
        }

        @Override
        protected void onPostExecute(Dir dir) {
            updateList(dir);
            loading.dismiss();
        }
    }
}
